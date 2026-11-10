package com.lepramim.atalhos

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.lepramim.leitura.Leitura
import com.lepramim.leitura.Situacao

/**
 * Tela invisível que recebe o texto e passa para o serviço de leitura.
 * Do botão rápido, lê o que foi copiado: o Android só libera a área de
 * transferência para o app que está na frente, por isso ela abre por um instante.
 */
class LerActivity : Activity() {
    private var esperandoFoco = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> concluir(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString())
            Intent.ACTION_SEND -> concluir(intent.getStringExtra(Intent.EXTRA_TEXT))
            ACAO_COPIADO -> esperandoFoco = true
            else -> finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !esperandoFoco) return
        esperandoFoco = false
        val texto = Leitura.copiado(this)
        // Lendo texto que não veio da área de transferência (marcar na tela): o toque também para.
        val lendo = Leitura.situacao.value != Situacao.PARADO
        val copiadoDepois = Leitura.horaDoCopiado(this) > Leitura.inicio
        if (lendo && (texto.isNullOrEmpty() || texto == Leitura.texto.value || !copiadoDepois)) {
            Leitura.comando(this, Leitura.ACAO_PARAR)
            finish()
            return
        }
        concluir(texto)
    }

    private fun concluir(texto: String?) {
        if (texto.isNullOrBlank()) Toast.makeText(this, "Nenhum texto copiado", Toast.LENGTH_SHORT).show()
        else Leitura.ler(this, texto)
        finish()
    }

    companion object {
        const val ACAO_COPIADO = "com.lepramim.LER_COPIADO"
    }
}
