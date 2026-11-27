package com.lepramim.leitura

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.lepramim.MainActivity
import com.lepramim.R
import com.lepramim.data.Preferencias
import com.lepramim.data.Repositorio
import android.media.MediaPlayer
import android.widget.Toast
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lê em segundo plano, com notificação, como um player de música. */
class LeituraServico : Service() {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var motorAtual: String? = null
    private var pronto = false
    private var frases: List<IntRange> = emptyList()
    private var atual = 0
    private var prefs = Preferencias()
    private var foco: AudioFocusRequest? = null
    private var inicioFrase = 0L
    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private val atributos = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        Leitura.persistir(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CANAL, "Leitura", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Leitura.ACAO_LER -> {
                val texto = intent.getStringExtra(Leitura.EXTRA_TEXTO).orEmpty()
                Leitura._texto.value = texto
                Leitura._trecho.value = null
                frases = dividir(texto)
                val inicio = intent.getIntExtra(Leitura.EXTRA_POSICAO, 0)
                atual = frases.indexOfLast { it.first <= inicio }.coerceAtLeast(0)
                retomar = -1
                Leitura._situacao.value = Situacao.TOCANDO
                mostrar()
                escopo.launch {
                    prefs = Repositorio(this@LeituraServico).preferencias.first()
                    Leitura._cps.value = 14f * prefs.velocidade
                    prepararMotor { falar() }
                }
            }
            Leitura.ACAO_PAUSAR -> {
                // Guarda o ponto exato: ao continuar, a voz segue dali.
                if (Leitura._situacao.value == Situacao.TOCANDO) retomar = posicaoAtual()
                tts?.stop()
                pararAzure()
                Leitura._situacao.value = Situacao.PAUSADO
                soltarFoco()
                mostrar()
            }
            Leitura.ACAO_CONTINUAR -> {
                Leitura._situacao.value = Situacao.TOCANDO
                mostrar()
                escopo.launch {
                    prefs = Repositorio(this@LeituraServico).preferencias.first()
                    prepararMotor { falar() }
                }
            }
            Leitura.ACAO_IR -> {
                val pos = intent.getIntExtra(Leitura.EXTRA_POSICAO, 0)
                retomar = -1
                if (frases.isNotEmpty()) {
                    atual = frases.indexOfLast { it.first <= pos }.coerceAtLeast(0)
                    Leitura._trecho.value = frases[atual]
                    if (Leitura._situacao.value == Situacao.TOCANDO) falar() else mostrar()
                }
            }
            Leitura.ACAO_RECARREGAR -> if (Leitura._situacao.value == Situacao.TOCANDO) escopo.launch {
                // Voz ou velocidade mudaram: continua do ponto exato em que a voz parou, não do começo da frase.
                retomar = posicaoAtual()
                tts?.stop()
                pararAzure()
                prefs = Repositorio(this@LeituraServico).preferencias.first()
                prepararMotor { falar() }
            }
            Leitura.ACAO_VOLTAR -> pular(-1)
            Leitura.ACAO_AVANCAR -> pular(1)
            Leitura.ACAO_PARAR -> encerrar()
            else -> if (Leitura._situacao.value == Situacao.PARADO) encerrar()
        }
        return START_NOT_STICKY
    }

    /** Onde retomar na frase atual (posição no texto) depois de trocar a voz; -1 = do começo da frase. Usado uma vez. */
    private var retomar = -1
    private var retomadoIdx = -1
    private var retomadoEm = 0
    /** Início da palavra que o motor do aparelho está falando (posição no texto). */
    @Volatile private var palavra = -1

    private fun inicioDe(i: Int) = if (i == retomadoIdx) retomadoEm else frases[i].first

    /** Posição no texto em que a fala está agora, recuada até o começo da palavra. */
    private fun posicaoAtual(): Int {
        val fr = frases.getOrNull(atual) ?: return -1
        val texto = Leitura._texto.value
        val mp = player
        val bruto = if (prefs.azure && mp != null) {
            val f = runCatching { mp.currentPosition.toFloat() / mp.duration.coerceAtLeast(1) }.getOrDefault(0f)
            inicioDe(atual) + (f * (fr.last + 1 - inicioDe(atual))).toInt()
        } else palavra.takeIf { it >= inicioDe(atual) } ?: return -1
        var p = bruto.coerceIn(fr.first, fr.last)
        while (p > fr.first && !texto[p - 1].isWhitespace()) p--
        return p
    }

    private fun pular(passo: Int) {
        retomar = -1
        if (frases.isEmpty()) return
        atual = (atual + passo).coerceIn(0, frases.lastIndex)
        Leitura._trecho.value = frases[atual]
        if (Leitura._situacao.value == Situacao.TOCANDO) falar() else mostrar()
    }

    /** Cria o motor de voz escolhido (ou reaproveita o atual) e depois fala. */
    private fun prepararMotor(depois: () -> Unit) {
        if (prefs.azure) {
            if (prefs.chave.isNotBlank()) { depois(); return }
            aviso("Sem chave. Voz do aparelho.")
            prefs = prefs.copy(motor = "")
        }
        val motor = prefs.motor
        if (tts != null && pronto && motor == motorAtual) {
            aplicarVoz()
            depois()
            return
        }
        tts?.shutdown()
        pronto = false
        motorAtual = motor
        val ouvinte = TextToSpeech.OnInitListener { s ->
            escopo.launch {
                if (s == TextToSpeech.SUCCESS) {
                    pronto = true
                    tts?.setAudioAttributes(atributos)
                    tts?.setOnUtteranceProgressListener(Progresso())
                    aplicarVoz()
                    depois()
                } else {
                    encerrar()
                }
            }
        }
        tts = if (motor.isEmpty()) TextToSpeech(this, ouvinte) else TextToSpeech(this, ouvinte, motor)
    }

    private fun aplicarVoz() {
        val t = tts ?: return
        if (prefs.voz.isNotEmpty()) runCatching { t.voices?.find { it.name == prefs.voz }?.let { t.voice = it } }
        t.setSpeechRate(prefs.velocidade)
    }

    /** Fala da frase atual até o fim, em fila. */
    private fun falar() {
        if (prefs.azure) { tts?.stop(); falarAzure(); return }
        pararAzure()
        val t = tts ?: return
        if (!pronto || frases.isEmpty()) return
        pedirFoco()
        val texto = Leitura._texto.value
        definirRetomada()
        for (i in atual..frases.lastIndex) {
            val r = frases[i]
            val modo = if (i == atual) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            t.speak(semLinks(texto.substring(inicioDe(i), r.last + 1), prefs).ifBlank { "." }, modo, Bundle(), "f$i")
        }
    }

    /** Consome o ponto de retomada: vale só para a frase atual, uma vez. */
    private fun definirRetomada() {
        val r = retomar
        retomar = -1
        val fr = frases.getOrNull(atual)
        if (r >= 0 && fr != null && r in fr) { retomadoIdx = atual; retomadoEm = r } else retomadoIdx = -1
        palavra = -1
    }

    private var jobAzure: Job? = null
    private var proxAzure: Deferred<ByteArray>? = null
    private var player: MediaPlayer? = null

    private fun aviso(msg: String) {
        if (!(Leitura.appVisivel && Leitura.avisos.tryEmit(msg))) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun pararAzure() {
        Leitura._carregando.value = false
        jobAzure?.cancel(); jobAzure = null
        proxAzure?.cancel(); proxAzure = null
        player?.let { runCatching { it.release() } }
        player = null
    }

    /** Voz da Azure: busca o MP3 de cada frase e já pede o da próxima enquanto a atual toca. */
    private fun falarAzure() {
        if (frases.isEmpty()) return
        pararAzure()
        pedirFoco()
        val texto = Leitura._texto.value
        val p = prefs
        val repo = Repositorio(this)
        definirRetomada()
        fun buscar(i: Int) = escopo.async(Dispatchers.IO) {
            val r = frases[i]
            val t = semLinks(texto.substring(inicioDe(i), r.last + 1), p).trim()
            if (t.isEmpty()) return@async ByteArray(0)
            Azure.sintetizar(p.regiao, p.chave, p.vozAzure, p.velocidade, t).also { repo.somarCaracteres(t.length) }
        }
        jobAzure = escopo.launch {
            try {
                var i = atual
                proxAzure = buscar(i)
                while (i <= frases.lastIndex) {
                    val pedido = proxAzure!!
                    val lento = if (pedido.isCompleted) null else launch {
                        Leitura._carregando.value = true
                        delay(5000)
                        aviso("Conexão lenta. Aguarde.")
                    }
                    val mp3 = pedido.await()
                    lento?.cancel()
                    Leitura._carregando.value = false
                    proxAzure = if (i < frases.lastIndex) buscar(i + 1) else null
                    atual = i
                    Leitura._trecho.value = inicioDe(i)..frases[i].last
                    if (Leitura._situacao.value == Situacao.TOCANDO) mostrar()
                    if (mp3.isNotEmpty()) tocar(mp3, inicioDe(i)..frases[i].last)
                    i++
                }
                encerrar()
            } catch (e: ErroAzure) {
                Leitura._carregando.value = false
                aviso(Azure.mensagemCurta(e.codigo) + " Voz do aparelho.")
                prefs = prefs.copy(motor = "")
                prepararMotor { falar() }
            }
        }
    }

    /** Toca o áudio da frase `faixa`; com o destaque ligado, marca a palavra estimada pelo ponto do áudio. */
    private suspend fun tocar(mp3: ByteArray, faixa: IntRange) = suspendCancellableCoroutine<Unit> { k ->
        val tamanho = faixa.last - faixa.first + 1
        val arq = File.createTempFile("fala", ".mp3", cacheDir).apply { writeBytes(mp3) }
        val mp = MediaPlayer().apply { setAudioAttributes(atributos); setDataSource(arq.path); prepare() }
        player = mp
        val ms = mp.duration
        if (ms > 400 && tamanho > 8) {
            Leitura._cps.value = (Leitura._cps.value * 0.5f + tamanho * 1000f / ms * 0.5f).coerceIn(5f, 60f)
        }
        // A Azure por REST não informa o tempo de cada palavra: estima pelo tamanho das palavras dentro da frase.
        val palavras = Regex("\\S+").findAll(Leitura._texto.value.substring(faixa.first, faixa.last + 1))
            .map { (faixa.first + it.range.first)..(faixa.first + it.range.last) }.toList()
        val acumulado = palavras.runningFold(0) { soma, w -> soma + (w.last - w.first + 2) }
        val marcador = if (prefs.destacar && palavras.size > 1) escopo.launch {
            var ultima = -1
            while (isActive) {
                val f = runCatching { mp.currentPosition.toFloat() / mp.duration.coerceAtLeast(1) }.getOrDefault(0f)
                val alvo = f * acumulado.last()
                val n = (acumulado.indexOfLast { it <= alvo }).coerceIn(0, palavras.lastIndex)
                if (n != ultima) { ultima = n; Leitura._palavra.value = palavras[n] }
                delay(40)
            }
        } else null
        fun fim() {
            marcador?.cancel()
            Leitura._palavra.value = null
            runCatching { mp.release() }
            if (player === mp) player = null
            arq.delete()
            if (k.isActive) k.resume(Unit)
        }
        mp.setOnCompletionListener { fim() }
        mp.setOnErrorListener { _, _, _ -> fim(); true }
        k.invokeOnCancellation { marcador?.cancel(); runCatching { mp.release() }; arq.delete() }
        mp.start()
    }

    private inner class Progresso : UtteranceProgressListener() {
        override fun onStart(id: String) {
            val i = id.removePrefix("f").toIntOrNull() ?: return
            inicioFrase = SystemClock.elapsedRealtime()
            palavra = frases.getOrNull(i)?.let { inicioDe(i) } ?: -1
            escopo.launch {
                atual = i
                Leitura._trecho.value = frases.getOrNull(i)?.let { inicioDe(i)..it.last }
                if (Leitura._situacao.value == Situacao.TOCANDO) mostrar()
            }
        }

        override fun onRangeStart(id: String, inicio: Int, fim: Int, frame: Int) {
            val i = id.removePrefix("f").toIntOrNull() ?: return
            if (i !in frases.indices) return
            val base = inicioDe(i)
            palavra = base + inicio
            if (!prefs.destacar) return
            // A frase continua marcada (suave); a palavra ganha o destaque forte.
            escopo.launch { Leitura._palavra.value = (base + inicio) until (base + fim) }
        }

        override fun onDone(id: String) {
            val i = id.removePrefix("f").toIntOrNull() ?: return
            val ms = SystemClock.elapsedRealtime() - inicioFrase
            val tamanho = frases.getOrNull(i)?.let { it.last - inicioDe(i) + 1 } ?: 0
            if (i == frases.lastIndex) escopo.launch { encerrar() }
            else if (ms > 400 && tamanho > 8) escopo.launch {
                // Ajusta a estimativa com o ritmo real da voz.
                Leitura._cps.value = (Leitura._cps.value * 0.7f + tamanho * 1000f / ms * 0.3f).coerceIn(5f, 60f)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(id: String) {
            escopo.launch { encerrar() }
        }
    }

    private fun pedirFoco() {
        if (foco != null) return
        foco = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(atributos)
            .build()
            .also { audio.requestAudioFocus(it) }
    }

    private fun soltarFoco() {
        foco?.let { audio.abandonAudioFocusRequest(it) }
        foco = null
    }

    private fun encerrar() {
        tts?.stop()
        pararAzure()
        soltarFoco()
        Leitura._situacao.value = Situacao.PARADO
        Leitura._trecho.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun intencao(acao: String) = PendingIntent.getService(
        this, acao.hashCode(), Intent(this, LeituraServico::class.java).setAction(acao), PendingIntent.FLAG_IMMUTABLE,
    )

    /** Só os botões, com o nome do app no topo (o sistema desenha). Play grande na cor do app. */
    private fun controles(tocando: Boolean) = RemoteViews(packageName, R.layout.notificacao).apply {
        setOnClickPendingIntent(R.id.voltar, intencao(Leitura.ACAO_VOLTAR))
        setOnClickPendingIntent(R.id.play, intencao(if (tocando) Leitura.ACAO_PAUSAR else Leitura.ACAO_CONTINUAR))
        setOnClickPendingIntent(R.id.avancar, intencao(Leitura.ACAO_AVANCAR))
        setImageViewResource(R.id.play, if (tocando) R.drawable.ic_pausa_escuro else R.drawable.ic_play_escuro)
        setContentDescription(R.id.play, if (tocando) "Pausar" else "Continuar")
        setTextViewText(R.id.tempo, Leitura.tempoTexto())
    }

    private var sessao: MediaSession? = null
    private val semCapa = android.os.Build.MANUFACTURER.equals("nubia", ignoreCase = true)

    /**
     * Capa do player: alto-falante menta sobre grafite. No Red Magic o player estica a capa no cartão todo,
     * então lá ela é só um fundo verde liso.
     */
    private var capaCache: Pair<Int, Bitmap>? = null

    /** Cor forte do destaque escolhido (a Automática vem do papel de parede). */
    private fun corCapa(): Int {
        val c = prefs.cor
        if (c == com.lepramim.data.CorDestaque.AUTOMATICA && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            return getColor(android.R.color.system_accent1_600)
        return c.claro.botao.toInt()
    }

    private fun capa(): Bitmap {
        val cor = if (semCapa) 0 else corCapa()
        capaCache?.let { if (it.first == cor) return it.second }
        val b = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        if (semCapa) {
            // Degradê verde-acinzentado, como o cartão do Poco: mais claro no centro, mais escuro nas bordas.
            val pincel = android.graphics.Paint().apply {
                shader = android.graphics.RadialGradient(200f, 210f, 420f, intArrayOf(0xFF3E726D.toInt(), 0xFF33504E.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
            }
            c.drawRect(0f, 0f, 512f, 512f, pincel)
        } else {
            // A arte é branca + verde; cada pixel vira branco + a cor de destaque (a dobra sai mais escura).
            val orig = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.capa_logo)
            if (orig != null) {
                val px = IntArray(512 * 512)
                Bitmap.createScaledBitmap(orig, 512, 512, true).getPixels(px, 0, 512, 0, 0, 512, 512)
                val ar = android.graphics.Color.red(cor); val ag = android.graphics.Color.green(cor); val ab = android.graphics.Color.blue(cor)
                for (i in px.indices) {
                    val t = ((255 - android.graphics.Color.red(px[i])) / 204f).coerceIn(0f, 1.2f)
                    fun m(x: Int) = (255 + t * (x - 255)).toInt().coerceIn(0, 255)
                    px[i] = android.graphics.Color.rgb(m(ar), m(ag), m(ab))
                }
                b.setPixels(px, 0, 512, 0, 0, 512, 512)
            }
        }
        capaCache = cor to b
        return b
    }

    /** Notificação de mídia do sistema, com sessão: o Android (e a Xiaomi) desenham o player. */
    private fun mostrarMidia() {
        val tocando = Leitura._situacao.value == Situacao.TOCANDO
        val s = sessao ?: MediaSession(this, "LePraMim").also { nova ->
            nova.setCallback(object : MediaSession.Callback() {
                override fun onPlay() = Leitura.comando(this@LeituraServico, Leitura.ACAO_CONTINUAR)
                override fun onPause() = Leitura.comando(this@LeituraServico, Leitura.ACAO_PAUSAR)
                override fun onSkipToNext() = Leitura.comando(this@LeituraServico, Leitura.ACAO_AVANCAR)
                override fun onSkipToPrevious() = Leitura.comando(this@LeituraServico, Leitura.ACAO_VOLTAR)
                override fun onStop() = Leitura.comando(this@LeituraServico, Leitura.ACAO_PARAR)
                override fun onSeekTo(pos: Long) {
                    val total = Leitura._texto.value.length
                    Leitura.ir(this@LeituraServico, (pos / 1000f * Leitura._cps.value).toInt().coerceIn(0, total))
                }
            })
            nova.isActive = true
            sessao = nova
        }
        val cps = Leitura._cps.value
        val total = (Leitura._texto.value.length / cps * 1000).toLong()
        val posicao = ((Leitura._trecho.value?.first ?: 0) / cps * 1000).toLong()
        val meta = MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Lê Pra Mim").putLong(MediaMetadata.METADATA_KEY_DURATION, total)

        meta.putBitmap(MediaMetadata.METADATA_KEY_ART, capa()).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, capa())
        s.setMetadata(meta.build())
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP or PlaybackState.ACTION_SEEK_TO)
                .setState(if (tocando) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, posicao, if (tocando) 1f else 0f)
                .build(),
        )
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_ler)
            .setContentTitle("Lê Pra Mim")
            .setLargeIcon(capa())
            .setContentIntent(abrir)
            .setDeleteIntent(intencao(Leitura.ACAO_PARAR))
            .setOngoing(tocando)
            .setSilent(true)
            .setColor(0xFF5EE0A8.toInt())
            .addAction(R.drawable.ic_voltar, "Voltar", intencao(Leitura.ACAO_VOLTAR))
            .addAction(
                if (tocando) R.drawable.ic_pausa else R.drawable.ic_play, if (tocando) "Pausar" else "Continuar",
                intencao(if (tocando) Leitura.ACAO_PAUSAR else Leitura.ACAO_CONTINUAR),
            )
            .addAction(R.drawable.ic_avancar, "Avançar", intencao(Leitura.ACAO_AVANCAR))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(android.support.v4.media.session.MediaSessionCompat.Token.fromToken(s.sessionToken))
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun mostrar() {
        if (MIDIA) { mostrarMidia(); return }
        val tocando = Leitura._situacao.value == Situacao.TOCANDO
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_ler)
            .setContentIntent(abrir)
            .setDeleteIntent(intencao(Leitura.ACAO_PARAR))
            .setOngoing(tocando)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF5EE0A8.toInt())
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(controles(tocando))
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    override fun onDestroy() {
        pararAzure()
        sessao?.release()
        sessao = null
        tts?.shutdown()
        tts = null
        escopo.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "leitura"
        /** true: notificação de mídia do sistema; false: a versão própria com play grande. */
        private const val MIDIA = true
        private const val LIMITE = 3500
        /** Regra de links do Balabolka: o que ela pega não é lido. */
        private val LINK = Regex("""((http|ftp|https)://)?(www\.)?([-a-zA-Z0-9@:%._\+~#\=]{2,256}\.[a-z]{2,6}\b)([-a-zA-Z0-9@:%_\+.~#?&//\=]*)""")

        /** Do link, lê só o nome do site (sem http, www nem caminho). Completa com espaços para o destaque não se deslocar. */
        fun semLinks(s: String, p: Preferencias): String {
            var r = LINK.replace(s) { it.groupValues[4].padEnd(it.value.length) }
            if (p.markdown) r = semMarcas(r)
            // Filtros do usuário: o que a expressão encontrar vira espaço e não é lido.
            for (re in p.regexes) r = re.replace(r) { " ".repeat(it.value.length) }
            return r
        }

        /** Marcas de Markdown que não devem ser lidas: asteriscos, crases e, no começo da linha, #, travessão de lista e >. */
        private val MARCAS = Regex("""\*+|`+|^[ \t]*(?:#{1,6}|[-•>]+)[ \t]+""", RegexOption.MULTILINE)

        /** Troca as marcas por espaços, para o destaque não se deslocar. */
        fun semMarcas(s: String) = MARCAS.replace(s) { " ".repeat(it.value.length) }
        /** Fim de frase: pontuação seguida de espaço ou do fim do texto, ou quebra de linha. Assim "Academia.edu" e "1.500" ficam inteiros. */
        private val FIM = Regex("[.!?…]+(?=\\s|$)|\\n")

        /** Divide em frases, com a posição de cada uma, para voltar, avançar e destacar. */
        fun dividir(texto: String): List<IntRange> {
            val saida = mutableListOf<IntRange>()
            fun guardar(inicio: Int, fim: Int) {
                var ini = inicio
                while (ini < fim && texto[ini].isWhitespace()) ini++
                var ult = fim - 1
                while (ult >= ini && texto[ult].isWhitespace()) ult--
                if (ult < ini) return
                while (ult - ini + 1 > LIMITE) {
                    val corte = texto.lastIndexOf(' ', ini + LIMITE).takeIf { it > ini } ?: (ini + LIMITE)
                    saida += ini until corte
                    ini = corte
                }
                saida += ini..ult
            }
            var inicio = 0
            FIM.findAll(texto).forEach { m ->
                guardar(inicio, m.range.last + 1)
                inicio = m.range.last + 1
            }
            guardar(inicio, texto.length)
            return saida
        }
    }
}
