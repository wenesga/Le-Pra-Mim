package com.leitorvoz.ui

import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leitorvoz.atalhos.MarcarTela
import com.leitorvoz.leitura.Leitura
import com.leitorvoz.leitura.Situacao
import com.leitorvoz.ui.theme.Espaco
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TelaLeitor(vm: LeitorViewModel) {
    val contexto = LocalContext.current
    val avisos = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val colar: () -> Unit = {
        val clip = contexto.getSystemService(ClipboardManager::class.java).primaryClip
        val t = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(contexto)?.toString()
        if (t.isNullOrBlank()) escopo.launch { avisos.showSnackbar("Nenhum texto copiado") } else Leitura.ler(contexto, t)
    }

    Scaffold(snackbarHost = { SnackbarHost(avisos) }) { margens ->
        Column(
            Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(Espaco.gg),
            verticalArrangement = Arrangement.spacedBy(Espaco.gg),
        ) {
            Text("Leitor Voz", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = Espaco.m, start = Espaco.p))
            CartaoLeitura(colar)
            CartaoVoz(vm)
            CartaoAtalhos()
        }
    }
}

@Composable
private fun CartaoLeitura(colar: () -> Unit) {
    val contexto = LocalContext.current
    val texto by Leitura.texto.collectAsStateWithLifecycle()
    val trecho by Leitura.trecho.collectAsStateWithLifecycle()
    val situacao by Leitura.situacao.collectAsStateWithLifecycle()
    val destaque = MaterialTheme.colorScheme.primaryContainer
    val noDestaque = MaterialTheme.colorScheme.onPrimaryContainer

    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.animateContentSize()) {
        if (texto.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(Espaco.xg), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Espaco.m))
                Text("Nenhum texto", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Espaco.gg))
                OutlinedButton(onClick = colar) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Espaco.m))
                    Text("Colar")
                }
            }
            return@Card
        }
        Column(Modifier.fillMaxWidth().padding(Espaco.gg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (situacao) { Situacao.TOCANDO -> "Lendo agora"; Situacao.PAUSADO -> "Pausado"; Situacao.PARADO -> "Última leitura" },
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = colar) { Icon(Icons.Outlined.ContentPaste, contentDescription = "Colar") }
            }
            val r = trecho
            val anotado = buildAnnotatedString {
                if (r == null || r.first >= texto.length) append(texto) else {
                    val fim = (r.last + 1).coerceAtMost(texto.length)
                    append(texto.substring(0, r.first))
                    withStyle(SpanStyle(background = destaque, color = noDestaque)) { append(texto.substring(r.first, fim)) }
                    append(texto.substring(fim))
                }
            }
            val rolagem = rememberScrollState()
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            // Mantém a linha lida visível, um pouco abaixo do topo do cartão.
            LaunchedEffect(r?.first, layout) {
                val l = layout ?: return@LaunchedEffect
                val inicio = r?.first ?: return@LaunchedEffect
                if (inicio >= l.layoutInput.text.length) return@LaunchedEffect
                val linha = l.getLineForOffset(inicio)
                val topo = l.getLineTop(linha).toInt()
                val base = l.getLineBottom(linha).toInt()
                val altura = rolagem.viewportSize
                if (topo < rolagem.value || base > rolagem.value + altura) {
                    rolagem.animateScrollTo((topo - altura / 4).coerceAtLeast(0))
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rolagem)) {
                Text(
                    anotado,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f),
                    onTextLayout = { layout = it },
                )
            }
            Spacer(Modifier.height(Espaco.g))
            LinearProgressIndicator(
                progress = { if (r == null || texto.isEmpty()) 0f else (r.first.toFloat() / texto.length).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Espaco.g))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Leitura.comando(contexto, Leitura.ACAO_VOLTAR) }) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Voltar")
                }
                FilledIconButton(
                    onClick = {
                        Leitura.comando(contexto, if (situacao == Situacao.TOCANDO) Leitura.ACAO_PAUSAR else Leitura.ACAO_CONTINUAR)
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    if (situacao == Situacao.TOCANDO) Icon(Icons.Outlined.Pause, contentDescription = "Pausar", modifier = Modifier.size(32.dp))
                    else Icon(Icons.Outlined.PlayArrow, contentDescription = "Ler", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { Leitura.comando(contexto, Leitura.ACAO_AVANCAR) }) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Avançar")
                }
                IconButton(onClick = { Leitura.comando(contexto, Leitura.ACAO_PARAR) }, enabled = situacao != Situacao.PARADO) {
                    Icon(Icons.Outlined.Stop, contentDescription = "Parar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartaoVoz(vm: LeitorViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val motores by vm.motores.collectAsStateWithLifecycle()
    val vozes by vm.vozes.collectAsStateWithLifecycle()

    Card(shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(Espaco.gg), verticalArrangement = Arrangement.spacedBy(Espaco.g)) {
            Text("Voz", style = MaterialTheme.typography.titleMedium)
            Seletor(
                rotulo = "Motor",
                valor = motores.find { it.pacote == prefs.motor }?.nome ?: "Padrão do sistema",
                opcoes = listOf("" to "Padrão do sistema") + motores.map { it.pacote to it.nome },
                escolher = vm::definirMotor,
            )
            if (vozes.isNotEmpty()) Seletor(
                rotulo = "Voz",
                valor = vozes.find { it.id == prefs.voz }?.nome ?: "Padrão",
                opcoes = listOf("" to "Padrão") + vozes.map { it.id to it.nome },
                escolher = vm::definirVoz,
            )
            var velocidade by remember(prefs.velocidade) { mutableFloatStateOf(prefs.velocidade) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Velocidade", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(String.format(Locale("pt", "BR"), "%.1fx", velocidade), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = velocidade, onValueChange = { velocidade = it },
                onValueChangeFinished = { vm.definirVelocidade(velocidade) },
                valueRange = 0.5f..2.5f, steps = 19,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Destacar palavra", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = prefs.destacar, onCheckedChange = vm::definirDestacar)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Seletor(rotulo: String, valor: String, opcoes: List<Pair<String, String>>, escolher: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
        OutlinedTextField(
            value = valor, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (id, nome) ->
                DropdownMenuItem(text = { Text(nome) }, onClick = { escolher(id); aberto = false })
            }
        }
    }
}

private fun acessibilidadeLigada(c: Context): Boolean {
    val lista = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    return lista.split(':').any { ComponentName.unflattenFromString(it) == ComponentName(c, MarcarTela::class.java) }
}

@Composable
private fun CartaoAtalhos() {
    val contexto = LocalContext.current
    var ligada by remember { mutableStateOf(acessibilidadeLigada(contexto)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ligada = acessibilidadeLigada(contexto) }
    val cores = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f))

    Card(shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(vertical = Espaco.m)) {
            Text("Atalhos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Espaco.gg, vertical = Espaco.p))
            ListItem(
                colors = cores,
                leadingContent = { Icon(Icons.Outlined.AccessibilityNew, contentDescription = null) },
                headlineContent = { Text("Marcar na tela") },
                supportingContent = { Text(if (ligada) "Ligado" else "Desligado") },
                trailingContent = {
                    TextButton(onClick = {
                        contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(if (ligada) "Alterar" else "Ativar") }
                },
            )
        }
    }
}
