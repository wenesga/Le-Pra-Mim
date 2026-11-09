package com.leitorvoz.atalhos

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.leitorvoz.leitura.Leitura

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
        val clip = getSystemService(ClipboardManager::class.java).primaryClip
        concluir(clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString())
    }

    private fun concluir(texto: String?) {
        if (texto.isNullOrBlank()) Toast.makeText(this, "Nenhum texto copiado", Toast.LENGTH_SHORT).show()
        else Leitura.ler(this, texto)
        finish()
    }

    companion object {
        const val ACAO_COPIADO = "com.leitorvoz.LER_COPIADO"
    }
}
