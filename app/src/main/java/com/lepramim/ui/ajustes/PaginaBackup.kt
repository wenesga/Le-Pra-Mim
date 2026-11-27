package com.lepramim.ui.ajustes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.data.FrequenciaBackup
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.componentes.Pagina
import com.lepramim.ui.theme.Espaco
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backup da biblioteca: arquivo manual e cópia automática (desligada, diária ou semanal) numa pasta, que pode ser do Google Drive. */
@Composable
fun PaginaBackup(vm: LeitorViewModel, avisos: SnackbarHostState, voltar: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val itens by vm.biblioteca.collectAsStateWithLifecycle()

    val criar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.fazerBackup(uri)
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.restaurarBackup(uri)
    }
    val escolherPasta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.escolherPastaBackup(uri)
    }

    val pasta = prefs.pastaBackup
    Pagina("Backup", voltar, avisos) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaco.m)) {
            LinhaBackup(Icons.Outlined.CloudUpload, "Fazer backup", "Salva a biblioteca num arquivo", ativo = itens.isNotEmpty()) {
                criar.launch("LePraMim_biblioteca_" + SimpleDateFormat("dd-MM-yy_HH-mm", Locale.ROOT).format(Date()) + ".json")
            }
            LinhaBackup(Icons.Outlined.History, "Restaurar backup", "Restaura a biblioteca") { abrir.launch(arrayOf("*/*")) }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(Espaco.gg),
                verticalArrangement = Arrangement.spacedBy(Espaco.g),
            ) {
                Text("Backup automático", style = MaterialTheme.typography.bodyLarge)
                val opcoes = FrequenciaBackup.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opcoes.forEachIndexed { i, f ->
                        SegmentedButton(
                            selected = prefs.frequenciaBackup == f,
                            onClick = {
                                vm.definirFrequenciaBackup(f)
                                // Ligar sem ter escolhido o local: pergunta onde guardar.
                                if (f != FrequenciaBackup.DESLIGADO && pasta.isEmpty()) escolherPasta.launch(null)
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, opcoes.size),
                            icon = {},
                        ) { Text(f.nome, maxLines = 1) }
                    }
                }
            }

            LinhaBackup(Icons.Outlined.Schedule, "Local do backup automático", resumoDoLocal(pasta, prefs.ultimoBackup)) { escolherPasta.launch(null) }
        }
    }
}

/** Cartão da lista: ícone cinza, título e uma linha curta. */
@Composable
private fun LinhaBackup(icone: ImageVector, titulo: String, resumo: String? = null, ativo: Boolean = true, aoClicar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().alpha(if (ativo) 1f else 0.5f).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = ativo, onClick = aoClicar).padding(Espaco.gg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Espaco.gg))
        Column {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (resumo != null) Text(resumo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Onde o backup está, sem mostrar o caminho, e quando foi o último. */
private fun resumoDoLocal(pasta: String, ultimo: Long): String {
    if (pasta.isEmpty()) return "Ainda não escolhido · toque para escolher"
    val provedor = Uri.parse(pasta).authority.orEmpty()
    val onde = when {
        provedor.contains("google", ignoreCase = true) || provedor.contains("docs", ignoreCase = true) -> "Google Drive"
        provedor.contains("externalstorage", ignoreCase = true) || provedor.contains("downloads", ignoreCase = true) -> "Dispositivo"
        else -> "Local escolhido"
    }
    return if (ultimo > 0) onde + " · " + SimpleDateFormat("dd/MM HH:mm", Locale.ROOT).format(Date(ultimo)) else "$onde · ainda não foi feito"
}
