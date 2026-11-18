package com.lepramim.ui.leitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.lepramim.ui.ajustes.formatarVelocidade
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
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
    velocidade: Float,
    aoMudarVelocidade: (Float) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
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
            Text(
                Leitura.tempoTexto(arrastando?.let { (it * total).toInt() } ?: posicao, total, cps),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = aoVoltar, enabled = situacao != Situacao.PARADO, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Voltar", modifier = Modifier.size(36.dp), tint = if (situacao != Situacao.PARADO) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.38f))
                }
                FilledIconButton(onClick = aoAlternar, enabled = !vazio, modifier = Modifier.size(64.dp)) {
                    if (situacao == Situacao.TOCANDO) Icon(Icons.Outlined.Pause, contentDescription = "Pausar", modifier = Modifier.size(32.dp))
                    else Icon(Icons.Outlined.PlayArrow, contentDescription = "Ler", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = aoAvancar, enabled = situacao != Situacao.PARADO, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Avançar", modifier = Modifier.size(36.dp), tint = if (situacao != Situacao.PARADO) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.38f))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { expandido = !expandido }) {
                        Icon(
                            if (expandido) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                            contentDescription = if (expandido) "Recolher" else "Expandir",
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = expandido,
                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200)),
            ) { Column {
                var valor by remember(velocidade) { mutableStateOf(velocidade) }
                Text(
                    "Velocidade: " + formatarVelocidade(valor),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = Espaco.m),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { aoMudarVelocidade((velocidade - 0.1f).coerceAtLeast(0.5f)) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mais devagar")
                    }
                    Slider(
                        value = valor, onValueChange = { valor = it },
                        onValueChangeFinished = { aoMudarVelocidade(valor) },
                        valueRange = 0.5f..2.5f, steps = 19, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { aoMudarVelocidade((velocidade + 0.1f).coerceAtMost(2.5f)) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Mais rápido")
                    }
                }
            } }
        }
    }
}
