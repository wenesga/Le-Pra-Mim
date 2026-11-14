package com.lepramim.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lepramim.data.Preferencias
import com.lepramim.leitura.Azure
import com.lepramim.leitura.Leitura
import com.lepramim.leitura.ErroAzure
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
        // Carrega sozinho as vozes pt-BR da Azure quando há chave.
        viewModelScope.launch {
            repo.preferencias.map { Triple(it.azure, it.regiao, it.chave) }.distinctUntilChanged().collect { (az, reg, ch) ->
                if (!az || ch.isEmpty()) return@collect
                runCatching {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Azure.vozes(reg, ch) }
                }.onSuccess { l -> _vozesAzure.value = l.map { Voz(it.first, it.second) } }
                    .onFailure { e -> _mensagem.value = "Vozes não carregaram: " + if (e is ErroAzure) Azure.mensagem(e.codigo) else (e.message ?: "erro") }
            }
        }
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

    private val _vozesAzure = MutableStateFlow<List<Voz>>(emptyList())
    val vozesAzure: StateFlow<List<Voz>> = _vozesAzure
    private val _mensagem = MutableStateFlow("")
    val mensagem: StateFlow<String> = _mensagem

    fun definirRegiao(r: String) = viewModelScope.launch { repo.definirRegiao(r) }
    fun definirVozAzure(v: String) = viewModelScope.launch { repo.definirVozAzure(v); aplicar() }
    fun colarChave(c: String) = viewModelScope.launch { repo.definirChave(c); _mensagem.value = "" }
    fun removerChave() = viewModelScope.launch { repo.definirChave(""); _vozesAzure.value = emptyList(); _mensagem.value = "" }

    /** Manda uma frase curta e busca as vozes pt-BR; mostra o resultado em palavras. */
    fun testar() = viewModelScope.launch {
        val p = prefs.value
        _mensagem.value = "Testando..."
        _mensagem.value = try {
            val lista = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                Azure.sintetizar(p.regiao, p.chave, p.vozAzure, 1f, "Teste.")
                Azure.vozes(p.regiao, p.chave)
            }
            _vozesAzure.value = lista.map { Voz(it.first, it.second) }
            "Funcionando."
        } catch (e: ErroAzure) {
            Azure.mensagem(e.codigo)
        }
    }

    private fun aplicar() = Leitura.comando(getApplication(), Leitura.ACAO_RECARREGAR)

    fun definirMotor(m: String) = viewModelScope.launch { repo.definirMotor(m); aplicar() }
    fun definirVoz(v: String) = viewModelScope.launch { repo.definirVoz(v); aplicar() }
    fun definirVelocidade(v: Float) = viewModelScope.launch { repo.definirVelocidade(v); aplicar() }
    fun definirDestacar(d: Boolean) = viewModelScope.launch { repo.definirDestacar(d) }

    override fun onCleared() {
        consulta?.shutdown()
        super.onCleared()
    }
}
