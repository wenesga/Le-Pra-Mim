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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
    carregando: Boolean,
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
    // Recolhido: só o play pequeno e a barra de andamento.
    var recolhido by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val vazio = total == 0
    var arrastando by remember { mutableStateOf<Float?>(null) }
    val posicao = trecho?.first ?: 0
    val fracao = if (total == 0) 0f else (posicao.toFloat() / total).coerceIn(0f, 1f)

    val barra: @Composable (Modifier) -> Unit = { m ->
        // A barra fica mais baixa que os 48 dp padrão do Material, para o player ocupar menos da tela.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified,
        ) {
            Slider(
                value = arrastando ?: fracao,
                onValueChange = { arrastando = it },
                onValueChangeFinished = {
                    arrastando?.let { aoIr((it * total).toInt()) }
                    arrastando = null
                },
                enabled = situacao != Situacao.PARADO,
                colors = com.lepramim.ui.theme.corSlider(),
                modifier = m.height(28.dp),
            )
        }
    }
    val conteudoPlay: @Composable (Int, Int) -> Unit = { icone, indicador ->
        if (situacao == Situacao.TOCANDO && carregando) androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(indicador.dp), strokeWidth = 3.dp, color = androidx.compose.material3.LocalContentColor.current,
        )
        else if (situacao == Situacao.TOCANDO) Icon(Icons.Outlined.Pause, contentDescription = "Pausar", modifier = Modifier.size(icone.dp))
        else Icon(Icons.Outlined.PlayArrow, contentDescription = "Ler", modifier = Modifier.size(icone.dp))
    }
    val corPlay = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    Surface(
        // Mesma cor do fundo da aba selecionada: o tom do tema a 20% sobre a cor das barras.
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f).compositeOver(MaterialTheme.colorScheme.surfaceContainer),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 6.dp,
    ) {
        Column(
            // Com o teclado aberto a barra de navegação já está coberta; somar de novo deixava um vão.
            Modifier.windowInsetsPadding(WindowInsets.navigationBars.exclude(WindowInsets.ime)).padding(horizontal = Espaco.gg).padding(top = 10.dp, bottom = 4.dp),
        ) {
            AnimatedVisibility(visible = recolhido) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(onClick = aoAlternar, enabled = !vazio, modifier = Modifier.size(36.dp), colors = corPlay) { conteudoPlay(20, 18) }
                    barra(Modifier.weight(1f).padding(horizontal = Espaco.m))
                    IconButton(onClick = { recolhido = false }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Expandir") }
                }
            }
            AnimatedVisibility(visible = !recolhido) { Column {
            // Tempo decorrido e total nas pontas da barra (uma linha só).
            val tempos = Leitura.tempoTexto(arrastando?.let { (it * total).toInt() } ?: posicao, total, cps).split(" / ")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tempos.getOrElse(0) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
                barra(Modifier.weight(1f).padding(horizontal = Espaco.m))
                Text(tempos.getOrElse(1) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    TextButton(onClick = { expandido = !expandido }) { Text(formatarVelocidade(velocidade)) }
                }
                IconButton(onClick = aoVoltar, enabled = situacao != Situacao.PARADO, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Voltar", modifier = Modifier.size(28.dp), tint = if (situacao != Situacao.PARADO) MaterialTheme.colorScheme.onSurface else androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.38f))
                }
                FilledIconButton(onClick = aoAlternar, enabled = !vazio, modifier = Modifier.size(52.dp), colors = corPlay) { conteudoPlay(26, 24) }
                IconButton(onClick = aoAvancar, enabled = situacao != Situacao.PARADO, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Avançar", modifier = Modifier.size(28.dp), tint = if (situacao != Situacao.PARADO) MaterialTheme.colorScheme.onSurface else androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.38f))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { recolhido = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Recolher")
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
                        valueRange = 0.5f..2.5f, steps = 19, modifier = Modifier.weight(1f), colors = com.lepramim.ui.theme.corSlider(),
                    )
                    IconButton(onClick = { aoMudarVelocidade((velocidade + 0.1f).coerceAtMost(2.5f)) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Mais rápido")
                    }
                }
            } }
            } }
        }
    }
}
