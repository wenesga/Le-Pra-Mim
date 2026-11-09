package com.leitorvoz.leitura

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Situacao { PARADO, TOCANDO, PAUSADO }

/** Estado da leitura, compartilhado entre o serviço e a tela. */
object Leitura {
    internal val _texto = MutableStateFlow("")
    internal val _trecho = MutableStateFlow<IntRange?>(null)
    internal val _situacao = MutableStateFlow(Situacao.PARADO)
    val texto: StateFlow<String> = _texto
    val trecho: StateFlow<IntRange?> = _trecho
    val situacao: StateFlow<Situacao> = _situacao

    const val ACAO_LER = "ler"
    const val ACAO_PAUSAR = "pausar"
    const val ACAO_CONTINUAR = "continuar"
    const val ACAO_VOLTAR = "voltar"
    const val ACAO_AVANCAR = "avancar"
    const val ACAO_PARAR = "parar"
    const val EXTRA_TEXTO = "texto"

    fun ler(c: Context, texto: String) {
        val limpo = texto.trim()
        if (limpo.isEmpty()) return
        c.startForegroundService(Intent(c, LeituraServico::class.java).setAction(ACAO_LER).putExtra(EXTRA_TEXTO, limpo))
    }

    fun comando(c: Context, acao: String) {
        if (acao == ACAO_CONTINUAR && _situacao.value == Situacao.PARADO) { ler(c, _texto.value); return }
        if (_situacao.value == Situacao.PARADO) return
        c.startService(Intent(c, LeituraServico::class.java).setAction(acao))
    }
}
