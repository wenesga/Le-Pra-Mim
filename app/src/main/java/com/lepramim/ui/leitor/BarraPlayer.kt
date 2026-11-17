package com.lepramim.ui.leitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lepramim.leitura.Leitura
import com.lepramim.leitura.Situacao
import com.lepramim.ui.theme.Espaco
import java.util.Locale

/** Barra de baixo: andamento, tempo, contador de caracteres e os controles da leitura. */
@Composable
fun BarraPlayer(
    total: Int,
    trecho: IntRange?,
    situacao: Situacao,
    cps: Float,
    exportando: String,
    uso: Pair<Long, Long>?,
    aoAlternar: () -> Unit,
    aoVoltar: () -> Unit,
    aoAvancar: () -> Unit,
    aoIr: (Int) -> Unit,
) {
    val vazio = total == 0
    var arrastando by remember { mutableStateOf<Float?>(null) }
    val posicao = trecho?.first ?: 0
    val fracao = if (total == 0) 0f else (posicao.toFloat() / total).coerceIn(0f, 1f)

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            // Com o teclado aberto a barra de navegação já está coberta; somar de novo deixava um vão.
            Modifier.windowInsetsPadding(WindowInsets.navigationBars.exclude(WindowInsets.ime)).padding(horizontal = Espaco.gg).padding(top = Espaco.m, bottom = Espaco.m),
        ) {
            Slider(
                value = arrastando ?: fracao,
                onValueChange = { arrastando = it },
                onValueChangeFinished = {
                    arrastando?.let { aoIr((it * total).toInt()) }
                    arrastando = null
                },
                enabled = situacao != Situacao.PARADO,
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = Espaco.m)) {
                val rotulo = MaterialTheme.typography.labelMedium
                val cor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(Leitura.tempoTexto(arrastando?.let { (it * total).toInt() } ?: posicao, total, cps), style = rotulo, color = cor, modifier = Modifier.weight(1f))
                Text(
                    when {
                        exportando.isNotEmpty() -> "Exportando $exportando"
                        else -> "%,d car.".format(Locale("pt", "BR"), total)
                    },
                    style = rotulo, color = cor,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = aoVoltar, enabled = situacao != Situacao.PARADO) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Voltar")
                }
                FilledIconButton(onClick = aoAlternar, enabled = !vazio, modifier = Modifier.size(64.dp)) {
                    if (situacao == Situacao.TOCANDO) Icon(Icons.Outlined.Pause, contentDescription = "Pausar", modifier = Modifier.size(32.dp))
                    else Icon(Icons.Outlined.PlayArrow, contentDescription = "Ler", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = aoAvancar, enabled = situacao != Situacao.PARADO) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Avançar")
                }
            }
        }
    }
}
