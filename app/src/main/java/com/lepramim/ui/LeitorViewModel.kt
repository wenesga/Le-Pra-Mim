package com.lepramim.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.lepramim.data.CorDestaque
import com.lepramim.data.Preferencias
import com.lepramim.leitura.LeituraServico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    fun definirCor(c: CorDestaque) = viewModelScope.launch { repo.definirCor(c) }

    /** Andamento da exportação ("3 de 20"); vazio quando não há exportação em curso. */
    private val _exportando = MutableStateFlow("")
    val exportando: StateFlow<String> = _exportando
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso
    fun avisoLido() { _aviso.value = null }

    /** Gera o MP3 do texto, frase a frase, com a voz da Azure escolhida, e grava no arquivo escolhido. */
    fun exportar(destino: Uri) {
        if (_exportando.value.isNotEmpty()) return
        val p = prefs.value
        val texto = Leitura.texto.value
        val frases = LeituraServico.dividir(texto)
        viewModelScope.launch {
            _exportando.value = "0 de ${frases.size}"
            _aviso.value = try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(destino, "wt")!!.use { saida ->
                        frases.forEachIndexed { i, r ->
                            val t = LeituraServico.semLinks(texto.substring(r.first, r.last + 1)).trim()
                            if (t.isNotEmpty()) {
                                saida.write(Azure.sintetizar(p.regiao, p.chave, p.vozAzure, p.velocidade, t))
                                repo.somarCaracteres(t.length)
                            }
                            _exportando.value = "${i + 1} de ${frases.size}"
                        }
                    }
                }
                "MP3 salvo"
            } catch (e: ErroAzure) {
                Azure.mensagem(e.codigo)
            } catch (e: Exception) {
                "Não foi possível salvar o MP3"
            }
            _exportando.value = ""
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
