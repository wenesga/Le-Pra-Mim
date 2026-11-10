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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
                atual = 0
                Leitura._situacao.value = Situacao.TOCANDO
                mostrar()
                escopo.launch {
                    prefs = Repositorio(this@LeituraServico).preferencias.first()
                    Leitura._cps.value = 14f * prefs.velocidade
                    prepararMotor { falar() }
                }
            }
            Leitura.ACAO_PAUSAR -> {
                tts?.stop()
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
                if (frases.isNotEmpty()) {
                    atual = frases.indexOfLast { it.first <= pos }.coerceAtLeast(0)
                    Leitura._trecho.value = frases[atual]
                    if (Leitura._situacao.value == Situacao.TOCANDO) falar() else mostrar()
                }
            }
            Leitura.ACAO_VOLTAR -> pular(-1)
            Leitura.ACAO_AVANCAR -> pular(1)
            Leitura.ACAO_PARAR -> encerrar()
            else -> if (Leitura._situacao.value == Situacao.PARADO) encerrar()
        }
        return START_NOT_STICKY
    }

    private fun pular(passo: Int) {
        if (frases.isEmpty()) return
        atual = (atual + passo).coerceIn(0, frases.lastIndex)
        Leitura._trecho.value = frases[atual]
        if (Leitura._situacao.value == Situacao.TOCANDO) falar() else mostrar()
    }

    /** Cria o motor de voz escolhido (ou reaproveita o atual) e depois fala. */
    private fun prepararMotor(depois: () -> Unit) {
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
        val t = tts ?: return
        if (!pronto || frases.isEmpty()) return
        pedirFoco()
        val texto = Leitura._texto.value
        for (i in atual..frases.lastIndex) {
            val r = frases[i]
            val modo = if (i == atual) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            t.speak(texto.substring(r.first, r.last + 1), modo, Bundle(), "f$i")
        }
    }

    private inner class Progresso : UtteranceProgressListener() {
        override fun onStart(id: String) {
            val i = id.removePrefix("f").toIntOrNull() ?: return
            inicioFrase = SystemClock.elapsedRealtime()
            escopo.launch {
                atual = i
                Leitura._trecho.value = frases.getOrNull(i)
                if (Leitura._situacao.value == Situacao.TOCANDO) mostrar()
            }
        }

        override fun onRangeStart(id: String, inicio: Int, fim: Int, frame: Int) {
            if (!prefs.destacar) return
            val i = id.removePrefix("f").toIntOrNull() ?: return
            val base = frases.getOrNull(i)?.first ?: return
            escopo.launch { Leitura._trecho.value = (base + inicio) until (base + fim) }
        }

        override fun onDone(id: String) {
            val i = id.removePrefix("f").toIntOrNull() ?: return
            val ms = SystemClock.elapsedRealtime() - inicioFrase
            val tamanho = frases.getOrNull(i)?.let { it.last - it.first + 1 } ?: 0
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

    /** Capa do player: alto-falante menta sobre grafite, as cores do app. */
    private val capa by lazy {
        val b = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(0xFF44484F.toInt())
        getDrawable(R.drawable.ic_ler)!!.mutate().apply { setTint(0xFF5EE0A8.toInt()); setBounds(112, 112, 400, 400); draw(c) }
        b
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
        s.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Lê Pra Mim").putLong(MediaMetadata.METADATA_KEY_DURATION, total)
            .putBitmap(MediaMetadata.METADATA_KEY_ART, capa).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, capa).build())
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
            .setLargeIcon(capa)
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
