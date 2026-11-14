package com.lepramim

import android.Manifest
import android.content.ClipboardManager
import com.lepramim.leitura.Leitura
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.TelaLeitor
import com.lepramim.ui.theme.TemaLeitor

class MainActivity : ComponentActivity() {
    private val vm: LeitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A notificação de leitura é o controle fora do app.
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            TemaLeitor(prefs.cor) { TelaLeitor(vm) }
        }
    }

    /** Ao abrir, lê sozinho o que foi copiado, se for um texto novo. Só com foco o Android libera a área de transferência. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        val texto = Leitura.copiado(this)
        if (texto.isNullOrEmpty() || texto == Leitura.texto.value) return
        // Guarda só uma marca do texto, para não ler de novo o mesmo copiado.
        val marca = texto.hashCode()
        val loja = getSharedPreferences("copiado", MODE_PRIVATE)
        if (loja.getInt("ultimo", 0) == marca) return
        loja.edit().putInt("ultimo", marca).apply()
        Leitura.ler(this, texto)
    }
}
