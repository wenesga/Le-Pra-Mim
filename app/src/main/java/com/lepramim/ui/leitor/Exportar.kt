package com.lepramim.ui.leitor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.lepramim.data.Preferencias
import com.lepramim.leitura.LeituraServico
import java.util.Locale

/** Janela de confirmação do MP3: mostra duração, caracteres e quanto resta da cota do mês. */
@Composable
fun DialogoExportar(
    texto: String,
    cps: Float,
    prefs: Preferencias,
    aoConfirmar: () -> Unit,
    aoCancelar: () -> Unit,
) {
    val gasto = LeituraServico.semLinks(texto, prefs).trim().length.toLong()
    val restam = (Preferencias.COTA_AZURE - prefs.usadosNoMes).coerceAtLeast(0)
    val minutos = Math.round(gasto / cps / 60f).coerceAtLeast(1)
    val cabe = gasto <= restam
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Exportar MP3") },
        text = {
            Text(
                "Cerca de $minutos min de áudio, %,d caracteres.\nRestam %,d este mês.".format(Locale("pt", "BR"), gasto, restam) +
                    if (cabe) "" else "\n\nNão cabe na cota do mês.",
            )
        },
        confirmButton = { TextButton(enabled = cabe && gasto > 0, onClick = aoConfirmar) { Text("Exportar") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}

fun nomeDoMp3(): String =
    "LePraMim_" + java.text.SimpleDateFormat("dd-MM-yy_HH-mm", Locale.ROOT).format(java.util.Date()) + ".mp3"
