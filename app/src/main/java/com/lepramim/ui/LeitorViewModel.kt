package com.lepramim.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.lepramim.data.CorDestaque
import com.lepramim.data.FundoEscuro
import com.lepramim.data.Preferencias
import com.lepramim.data.TemaApp
import com.lepramim.leitura.LeituraServico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lepramim.leitura.Azure
import com.lepramim.leitura.Arquivos
import com.lepramim.leitura.Leitura
import com.lepramim.leitura.ErroAzure
import com.lepramim.data.Repositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class Voz(val id: String, val nome: String)

class LeitorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repositorio(app)
    private val bib = com.lepramim.data.Biblioteca(app)

    private val _biblioteca = MutableStateFlow<List<com.lepramim.data.ItemBiblioteca>>(emptyList())
    val biblioteca: StateFlow<List<com.lepramim.data.ItemBiblioteca>> = _biblioteca
    private fun recarregarBiblioteca() {
        viewModelScope.launch { _biblioteca.value = withContext(Dispatchers.IO) { bib.listar() } }
    }
    init { recarregarBiblioteca() }

    /** Título quando a pessoa não dá um: as primeiras palavras do texto. */
    private fun tituloDe(texto: String) =
        texto.trim().lineSequence().firstOrNull().orEmpty().trim().take(40).ifEmpty { "Sem título" }

    /** Nome do arquivo escolhido, sem a extensão. */
    private fun nomeDoArquivo(origem: Uri): String? = runCatching {
        getApplication<Application>().contentResolver.query(origem, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?.substringBeforeLast('.')?.trim()?.ifEmpty { null }
    }.getOrNull()

    /** Guarda o texto na biblioteca e devolve a lista atualizada. */
    private suspend fun guardar(titulo: String, texto: String) {
        withContext(Dispatchers.IO) { bib.salvar(titulo.trim().ifEmpty { tituloDe(texto) }, texto) }
        _biblioteca.value = withContext(Dispatchers.IO) { bib.listar() }
        backupAutomatico()
    }

    /**
     * Backup automático: grava a cópia na pasta escolhida quando já passou o período (diário ou semanal).
     * Sem pasta ou desligado, não faz nada. `forcar` grava agora, sem esperar o período.
     */
    fun backupAutomatico(forcar: Boolean = false, pasta: String = prefs.value.pastaBackup) {
        val p = prefs.value
        if (pasta.isEmpty() || (p.frequenciaBackup == com.lepramim.data.FrequenciaBackup.DESLIGADO && !forcar)) return
        val agora = System.currentTimeMillis()
        if (!forcar && agora - p.ultimoBackup < p.frequenciaBackup.periodoMs) return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = com.lepramim.data.Backup.gravarNaPasta(getApplication(), Uri.parse(pasta), bib.exportar())
            if (ok) repo.definirUltimoBackup(agora)
            else _aviso.value = "Não consegui gravar o backup. Escolha o local de novo em Configurações → Backup."
        }
    }

    fun definirFrequenciaBackup(f: com.lepramim.data.FrequenciaBackup) {
        viewModelScope.launch { repo.definirFrequenciaBackup(f) }
    }

    /** Backup manual: grava toda a biblioteca no arquivo que a pessoa escolheu. */
    fun fazerBackup(destino: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(destino, "wt")!!.use { it.write(bib.exportar().toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            _aviso.value = if (ok) "Backup salvo" else "Não foi possível salvar o backup"
        }
    }

    /** Restaura um arquivo de backup, sem duplicar o que já existe. */
    fun restaurarBackup(origem: Uri) {
        viewModelScope.launch {
            val novos = withContext(Dispatchers.IO) {
                runCatching {
                    val json = getApplication<Application>().contentResolver.openInputStream(origem)!!.use { String(it.readBytes(), Charsets.UTF_8) }
                    bib.restaurar(json)
                }.getOrNull()
            }
            recarregarBiblioteca()
            _aviso.value = when (novos) {
                null -> "Esse arquivo não é um backup do Lê Pra Mim"
                0 -> "Nada novo para restaurar: todos os textos já estão aqui"
                1 -> "1 texto restaurado"
                else -> "$novos textos restaurados"
            }
            if (novos != null && novos > 0) backupAutomatico()
        }
    }

    /** Escolhe o local do backup automático e já grava a primeira cópia. Sem frequência escolhida, fica diário. */
    fun escolherPastaBackup(pasta: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                pasta, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        viewModelScope.launch {
            repo.definirPastaBackup(pasta.toString())
            if (prefs.value.frequenciaBackup == com.lepramim.data.FrequenciaBackup.DESLIGADO) repo.definirFrequenciaBackup(com.lepramim.data.FrequenciaBackup.DIARIO)
            backupAutomatico(forcar = true, pasta = pasta.toString())
        }
    }

    fun removerPastaBackup() {
        val atual = prefs.value.pastaBackup
        viewModelScope.launch {
            repo.definirPastaBackup("")
            repo.definirFrequenciaBackup(com.lepramim.data.FrequenciaBackup.DESLIGADO)
            if (atual.isNotEmpty()) runCatching {
                getApplication<Application>().contentResolver.releasePersistableUriPermission(
                    Uri.parse(atual), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
    }

    /** Escrever texto: guarda na biblioteca e deixa o texto pronto na Leitura, sem tocar sozinho. */
    fun guardarEAbrir(titulo: String, texto: String) {
        viewModelScope.launch {
            guardar(titulo, texto)
            Leitura.editar(getApplication(), texto)
        }
    }

    /** Link da web: baixa a página, tira o texto e guarda na biblioteca. */
    fun guardarLink(entrada: String) {
        _aviso.value = "Lendo a página…"
        viewModelScope.launch {
            val pagina = withContext(Dispatchers.IO) { com.lepramim.leitura.Web.ler(entrada) }
            _aviso.value = when {
                pagina == null -> "Não foi possível abrir o link"
                pagina.second.isBlank() -> "Não encontrei texto na página"
                else -> { guardar(pagina.first, pagina.second); "Guardado na biblioteca" }
            }
        }
    }

    /** Guarda na biblioteca o texto que está na Leitura (sem repetir um que já esteja lá). */
    fun guardarAtual() {
        val texto = Leitura.texto.value
        if (texto.isBlank()) return
        viewModelScope.launch {
            if (_biblioteca.value.any { it.texto == texto }) {
                _aviso.value = "Já está na biblioteca"
            } else {
                guardar("", texto)
                _aviso.value = "Guardado na biblioteca"
            }
        }
    }

    /** Abre um item da biblioteca na Leitura. */
    fun abrirDaBiblioteca(item: com.lepramim.data.ItemBiblioteca) = Leitura.editar(getApplication(), item.texto)

    fun renomearItem(id: String, titulo: String) {
        val novo = titulo.trim()
        if (novo.isEmpty()) return
        viewModelScope.launch { withContext(Dispatchers.IO) { bib.renomear(id, novo) }; recarregarBiblioteca(); backupAutomatico() }
    }

    fun excluirItem(id: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { bib.excluir(id) }; recarregarBiblioteca(); backupAutomatico() }
    }
    val prefs: StateFlow<Preferencias> = repo.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, Preferencias())

    private val _vozes = MutableStateFlow<List<Voz>>(emptyList())
    val vozes: StateFlow<List<Voz>> = _vozes

    /** Motor só para listar as vozes; a leitura usa o do serviço. */
    private var consulta: TextToSpeech? = null

    init {
        Leitura.persistir(app)
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
        // Ao abrir o app, atualiza sozinho o consumo do portal (sem mostrar erro).
        viewModelScope.launch {
            val p = repo.preferencias.first()
            if (p.azure && p.monitorPronto) atualizarPortal(mostrarErro = false)
        }
    }

    private fun listar(motor: String) {
        consulta?.shutdown()
        var t: TextToSpeech? = null
        val ouvinte = TextToSpeech.OnInitListener { s ->
            val tts = t ?: return@OnInitListener
            if (s != TextToSpeech.SUCCESS) return@OnInitListener
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
    fun alternarFavorita(id: String) = viewModelScope.launch {
        val f = prefs.value.favoritas
        repo.definirFavoritas(if (id in f) f - id else f + id)
    }
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

    private val _mensagemMonitor = MutableStateFlow("")
    val mensagemMonitor: StateFlow<String> = _mensagemMonitor

    fun definirMonApp(v: String) = viewModelScope.launch { repo.definirMonApp(v) }
    fun definirMonTenant(v: String) = viewModelScope.launch { repo.definirMonTenant(v) }
    fun definirMonAssinatura(v: String) = viewModelScope.launch { repo.definirMonAssinatura(v) }
    fun definirMonRecurso(v: String) = viewModelScope.launch { repo.definirMonRecurso(v) }
    fun colarSegredo(v: String) = viewModelScope.launch { repo.definirMonSegredo(v); _mensagemMonitor.value = "" }
    fun removerMonitor() = viewModelScope.launch { repo.limparMonitor(); _mensagemMonitor.value = "" }

    /** Lê no Azure Monitor o total de caracteres do mês (o número do portal). */
    fun atualizarPortal(mostrarErro: Boolean = true) = viewModelScope.launch {
        val p = prefs.value
        if (!p.monitorPronto) return@launch
        if (mostrarErro) _mensagemMonitor.value = "Buscando..."
        try {
            val (total, sub) = withContext(Dispatchers.IO) {
                Azure.consumoDoMes(p.monTenant, p.monApp, p.monSegredo, p.monAssinatura, p.monRecurso)
            }
            repo.guardarPortal(total, sub)
            _mensagemMonitor.value = if (mostrarErro) "Atualizado." else ""
        } catch (e: ErroAzure) {
            if (mostrarErro) _mensagemMonitor.value = Azure.mensagemMonitor(e.codigo)
        }
    }

    fun definirFundo(f: FundoEscuro) = viewModelScope.launch { repo.definirFundo(f) }
    fun definirCor(c: CorDestaque) = viewModelScope.launch { repo.definirCor(c) }
    fun definirTema(t: TemaApp) = viewModelScope.launch { repo.definirTema(t) }
    fun definirTamanho(t: com.lepramim.data.TamanhoTexto) = viewModelScope.launch { repo.definirTamanho(t) }
    fun definirFonte(f: com.lepramim.data.FonteTexto) = viewModelScope.launch { repo.definirFonte(f) }
    fun definirMarkdown(m: Boolean) = viewModelScope.launch { repo.definirMarkdown(m); aplicar() }

    /** Guarda um filtro novo. Devolve a mensagem de erro se a expressão for inválida ou repetida; null se deu certo. */
    fun adicionarFiltro(padrao: String): String? {
        val p = padrao.trim()
        if (p.isEmpty()) return "Digite a expressão."
        if (p.length > 2 && p.startsWith('/') && (p.endsWith('/') || p.matches(Regex("/.*/[a-z]*")))) {
            return "Digite sem as barras do começo e do fim."
        }
        runCatching { Regex(p) }.onFailure { return "Expressão inválida. Confira os parênteses, colchetes e barras." }
        if (p in prefs.value.filtros) return "Esse filtro já existe."
        viewModelScope.launch { repo.definirFiltros(prefs.value.filtros + p); aplicar() }
        return null
    }

    fun removerFiltro(padrao: String) = viewModelScope.launch { repo.definirFiltros(prefs.value.filtros - padrao); aplicar() }

    /** Andamento da exportação ("3 de 20"); vazio quando não há exportação em curso. */
    private val _exportando = MutableStateFlow("")
    val exportando: StateFlow<String> = _exportando
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso
    fun avisoLido() { _aviso.value = null }

    /** A página de entradas pede para a tela principal abrir a edição (Escrever texto). */
    private val _editarPedido = MutableStateFlow(false)
    val editarPedido: StateFlow<Boolean> = _editarPedido
    fun pedirEdicao() { _editarPedido.value = true }
    fun editarAtendido() { _editarPedido.value = false }

    /** PDF: cada página vira imagem e passa pelo reconhecimento de texto; pode demorar. */
    fun lerPdf(origem: Uri) {
        _aviso.value = "Lendo o PDF… pode demorar"
        viewModelScope.launch {
            val texto = withContext(Dispatchers.IO) { com.lepramim.leitura.Imagens.pdf(getApplication(), origem) }
            _aviso.value = if (texto == null) "Não foi possível ler o PDF" else { guardar(nomeDoArquivo(origem) ?: "", texto); "Guardado na biblioteca" }
        }
    }

    /** Reconhece o texto das imagens escaneadas ou escolhidas e põe o resultado no editor. */
    fun lerImagens(imagens: List<Uri>) {
        _aviso.value = "Reconhecendo o texto…"
        com.lepramim.leitura.Imagens.texto(getApplication(), imagens) { texto ->
            if (texto == null) { _aviso.value = "Não encontrei texto na imagem"; return@texto }
            viewModelScope.launch {
                val hora = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date())
                guardar("Digitalização $hora", texto)
                _aviso.value = "Guardado na biblioteca"
            }
        }
    }

    /** Lê um arquivo TXT e põe o conteúdo no editor. Aceita UTF-8 e, se não for, Windows-1252 (TXT antigo). */
    fun importar(origem: Uri) {
        if (getApplication<Application>().contentResolver.getType(origem) == "application/pdf") { lerPdf(origem); return }
        viewModelScope.launch {
            val texto = withContext(Dispatchers.IO) {
                Arquivos.texto(getApplication(), origem)
            }
            _aviso.value = when {
                texto == null -> "Não foi possível abrir o arquivo"
                texto.isEmpty() -> "O arquivo está vazio"
                else -> { guardar(nomeDoArquivo(origem) ?: "", texto); "Guardado na biblioteca" }
            }
        }
    }

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
                            val t = LeituraServico.semLinks(texto.substring(r.first, r.last + 1), p).trim()
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
