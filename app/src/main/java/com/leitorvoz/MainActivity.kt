package com.leitorvoz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.leitorvoz.ui.LeitorViewModel
import com.leitorvoz.ui.TelaLeitor
import com.leitorvoz.ui.theme.TemaLeitor

class MainActivity : ComponentActivity() {
    private val vm: LeitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A notificação de leitura é o controle fora do app.
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent { TemaLeitor { TelaLeitor(vm) } }
    }
}
