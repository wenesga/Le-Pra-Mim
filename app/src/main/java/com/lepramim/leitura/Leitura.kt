package com.lepramim.leitura

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Situacao { PARADO, TOCANDO, PAUSADO }

/** Estado da leitura, compartilhado entre o serviço e a tela. */
object Leitura {
    /** Avisos do serviço para a tela (aparecem na cor do tema). Sem tela na frente, o serviço usa o Toast do sistema. */
    val avisos = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    @Volatile var appVisivel = false
    internal val _texto = MutableStateFlow("")
    internal val _trecho = MutableStateFlow<IntRange?>(null)
    internal val _situacao = MutableStateFlow(Situacao.PARADO)
    val texto: StateFlow<String> = _texto
    val trecho: StateFlow<IntRange?> = _trecho
    /** Palavra que está sendo lida dentro da frase (destaque mais forte); na Azure é estimada. */
    internal val _palavra = MutableStateFlow<IntRange?>(null)
    val palavra: StateFlow<IntRange?> = _palavra
    val situacao: StateFlow<Situacao> = _situacao

    /** Esperando a voz da Azure mandar o áudio da frase (o botão de play mostra um círculo girando). */
    internal val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    /** Quando a leitura atual começou; serve para saber se o copiado é mais novo. */
    @Volatile var inicio = 0L

    /** Caracteres lidos por segundo, estimado pela velocidade e corrigido pelo tempo real das frases. */
    internal val _cps = MutableStateFlow(14f)
    val cps: StateFlow<Float> = _cps

    fun tempoTexto(posicao: Int, total: Int, cps: Float): String {
        if (total <= 0 || cps <= 0f) return ""
        fun fmt(s: Float) = "%d:%02d".format(s.toInt() / 60, s.toInt() % 60)
        return fmt(posicao / cps) + " / " + fmt(total / cps)
    }

    fun tempoTexto() = tempoTexto(_trecho.value?.first ?: 0, _texto.value.length, _cps.value)

    const val ACAO_LER = "ler"
    const val ACAO_PAUSAR = "pausar"
    const val ACAO_CONTINUAR = "continuar"
    const val ACAO_VOLTAR = "voltar"
    const val ACAO_AVANCAR = "avancar"
    const val ACAO_PARAR = "parar"
    const val ACAO_IR = "ir"
    const val ACAO_RECARREGAR = "recarregar"
    const val EXTRA_POSICAO = "posicao"
    const val EXTRA_TEXTO = "texto"

    /** Chamada para o link no fim do texto, como o "Veja mais em" do copia e cola dos sites. */
    private val chamadaFinal = Regex(
        // A: depois de traço, reticências ou quebra de linha, uma frase curta e o link. B: frase que começa com palavra de chamada.
        "\\s*(?:(?:[-–—…\\n]|\\.{3})\\s*(?:[^\\s.:/]+\\s+){0,5}[^\\s.:/]+\\s*:?" +
            "|[-–—]?\\s*(?:veja|leia|saiba|acesse|confira|continue|clique|baixe|assista|acompanhe|siga|compartilhe|fonte|link|" +
            "texto completo|matéria completa|reportagem completa|mais informações|originalmente)(?:\\s+[^\\s.:/]+){0,4}\\s*:?)" +
            "\\s*https?://\\S+\\s*$",
        RegexOption.IGNORE_CASE,
    )

    /** Tira só o link do fim com chamada; links no meio do texto ficam. */
    fun semChamadaFinal(texto: String) = texto.replace(chamadaFinal, "")

    fun ler(c: Context, texto: String, posicao: Int = 0) {
        val limpo = semChamadaFinal(texto).trim()
        if (limpo.isEmpty()) return
        inicio = System.currentTimeMillis()
        c.startForegroundService(
            Intent(c, LeituraServico::class.java).setAction(ACAO_LER)
                .putExtra(EXTRA_TEXTO, limpo).putExtra(EXTRA_POSICAO, posicao),
        )
    }

    /** Toque no texto: lê dali na hora, esteja parado, pausado ou tocando. */
    fun lerDe(c: Context, posicao: Int) {
        val texto = _texto.value
        if (texto.isBlank()) return
        if (_situacao.value == Situacao.PARADO) {
            // ler() tira os espaços do começo; a posição acompanha.
            ler(c, texto, (posicao - (texto.length - texto.trimStart().length)).coerceAtLeast(0))
            return
        }
        ir(c, posicao)
        if (_situacao.value == Situacao.PAUSADO) comando(c, ACAO_CONTINUAR)
    }

    /** Texto copiado, ou null se vazio ou marcado como sensível (senha de gerenciador de senhas). */
    fun copiado(c: Context): String? {
        val clip = c.getSystemService(ClipboardManager::class.java).primaryClip ?: return null
        if (clip.itemCount == 0) return null
        if (clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) return null
        return clip.getItemAt(0).coerceToText(c)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Hora em que o copiado foi feito, ou 0. */
    fun horaDoCopiado(c: Context): Long =
        c.getSystemService(ClipboardManager::class.java).primaryClip?.description?.timestamp ?: 0L

    fun copiarTexto(c: Context) {
        if (_texto.value.isEmpty()) return
        c.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Lê Pra Mim", _texto.value))
    }

    private var persistindo = false

    /** Guarda o texto num arquivo e o restaura ao abrir; o texto só some com "Limpar". */
    @Synchronized
    @OptIn(DelicateCoroutinesApi::class)
    fun persistir(c: Context) {
        if (persistindo) return
        persistindo = true
        val arquivo = java.io.File(c.applicationContext.filesDir, "texto.txt")
        if (_texto.value.isEmpty()) runCatching { if (arquivo.exists()) _texto.value = arquivo.readText() }
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _texto.debounce(500).collect { t ->
                runCatching { if (t.isEmpty()) arquivo.delete() else arquivo.writeText(t) }
            }
        }
    }

    fun limpar(c: Context) {
        if (_situacao.value != Situacao.PARADO) comando(c, ACAO_PARAR)
        _texto.value = ""
        _trecho.value = null
    }

    /** Texto digitado no editor. Editar para a leitura, porque as frases em andamento ficam velhas. */
    fun editar(c: Context, novo: String) {
        if (novo == _texto.value) return
        if (_situacao.value != Situacao.PARADO) comando(c, ACAO_PARAR)
        _texto.value = novo
        _trecho.value = null
    }

    /** Pula para a frase que contém a posição (em caracteres) do texto. */
    fun ir(c: Context, posicao: Int) {
        if (_situacao.value == Situacao.PARADO) return
        c.startService(Intent(c, LeituraServico::class.java).setAction(ACAO_IR).putExtra(EXTRA_POSICAO, posicao))
    }

    fun comando(c: Context, acao: String) {
        if (acao == ACAO_CONTINUAR && _situacao.value == Situacao.PARADO) { ler(c, _texto.value); return }
        if (_situacao.value == Situacao.PARADO) return
        c.startService(Intent(c, LeituraServico::class.java).setAction(acao))
    }
}
