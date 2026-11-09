package com.lepramim.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lepramim.data.Preferencias
import com.lepramim.data.Repositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class Motor(val pacote: String, val nome: String)
data class Voz(val id: String, val nome: String)

class LeitorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repositorio(app)
    val prefs: StateFlow<Preferencias> = repo.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, Preferencias())

    private val _motores = MutableStateFlow<List<Motor>>(emptyList())
    private val _vozes = MutableStateFlow<List<Voz>>(emptyList())
    val motores: StateFlow<List<Motor>> = _motores
    val vozes: StateFlow<List<Voz>> = _vozes

    /** Motor só para listar as vozes; a leitura usa o do serviço. */
    private var consulta: TextToSpeech? = null

    init {
        viewModelScope.launch {
            repo.preferencias.map { it.motor }.distinctUntilChanged().collect { listar(it) }
        }
    }

    private fun listar(motor: String) {
        consulta?.shutdown()
        var t: TextToSpeech? = null
        val ouvinte = TextToSpeech.OnInitListener { s ->
            val tts = t ?: return@OnInitListener
            if (s != TextToSpeech.SUCCESS) return@OnInitListener
            _motores.value = tts.engines.map { Motor(it.name, it.label) }
            val todas = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
                .filter { !it.isNetworkConnectionRequired }
            val pt = todas.filter { it.locale.language == "pt" }
            _vozes.value = (pt.ifEmpty { todas }).sortedBy { it.name }.map { Voz(it.name, nomeVoz(it.name, it.locale)) }
        }
        t = if (motor.isEmpty()) TextToSpeech(getApplication(), ouvinte) else TextToSpeech(getApplication(), ouvinte, motor)
        consulta = t
    }

    /** Deixa o nome técnico da voz mais legível. */
    private fun nomeVoz(id: String, local: Locale): String {
        val limpo = id.substringAfterLast('#').substringAfterLast('.')
            .replace(Regex("(?i)${local.toLanguageTag()}|-x-|-local|-network|_"), " ")
            .replace('-', ' ').trim()
        val base = limpo.ifEmpty { id }
        return base.replaceFirstChar { it.titlecase() } + " (" + local.getDisplayCountry(Locale("pt", "BR")).ifEmpty { local.displayName } + ")"
    }

    fun definirMotor(m: String) = viewModelScope.launch { repo.definirMotor(m) }
    fun definirVoz(v: String) = viewModelScope.launch { repo.definirVoz(v) }
    fun definirVelocidade(v: Float) = viewModelScope.launch { repo.definirVelocidade(v) }
    fun definirDestacar(d: Boolean) = viewModelScope.launch { repo.definirDestacar(d) }

    override fun onCleared() {
        consulta?.shutdown()
        super.onCleared()
    }
}
