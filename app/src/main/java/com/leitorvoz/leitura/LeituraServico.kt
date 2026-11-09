package com.leitorvoz.leitura

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.leitorvoz.MainActivity
import com.leitorvoz.R
import com.leitorvoz.data.Preferencias
import com.leitorvoz.data.Repositorio
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
            escopo.launch {
                atual = i
                Leitura._trecho.value = frases.getOrNull(i)
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
            if (i == frases.lastIndex) escopo.launch { encerrar() }
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

    private fun mostrar() {
        val tocando = Leitura._situacao.value == Situacao.TOCANDO
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_ler)
            .setContentTitle(if (tocando) "Lendo" else "Pausado")
            .setContentText(Leitura._texto.value.take(120))
            .setContentIntent(abrir)
            .setDeleteIntent(intencao(Leitura.ACAO_PARAR))
            .setOngoing(tocando)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Voltar", intencao(Leitura.ACAO_VOLTAR))
            .addAction(
                if (tocando) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (tocando) "Pausar" else "Continuar",
                intencao(if (tocando) Leitura.ACAO_PAUSAR else Leitura.ACAO_CONTINUAR),
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar", intencao(Leitura.ACAO_PARAR))
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        escopo.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "leitura"
        private const val LIMITE = 3500
        private val FRASE = Regex("[^.!?\\n]+[.!?]*")

        /** Divide em frases, com a posição de cada uma, para voltar, avançar e destacar. */
        fun dividir(texto: String): List<IntRange> {
            val saida = mutableListOf<IntRange>()
            FRASE.findAll(texto).forEach { m ->
                if (m.value.isBlank()) return@forEach
                var ini = m.range.first
                while (m.range.last - ini + 1 > LIMITE) {
                    val corte = texto.lastIndexOf(' ', ini + LIMITE).takeIf { it > ini } ?: (ini + LIMITE)
                    saida += ini until corte
                    ini = corte
                }
                saida += ini..m.range.last
            }
            return saida
        }
    }
}
