package com.lepramim.ui


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Download
import com.lepramim.data.CorDestaque
import com.lepramim.ui.theme.amostraDe
import com.lepramim.ui.theme.nomeDaCor
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.atalhos.MarcarTela
import com.lepramim.leitura.Leitura
import com.lepramim.leitura.LeituraServico
import com.lepramim.leitura.Situacao
import com.lepramim.ui.theme.Espaco
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaLeitor(vm: LeitorViewModel) {
    val contexto = LocalContext.current
    val avisos = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val colar: () -> Unit = {
        val t = Leitura.copiado(contexto)
        if (t.isNullOrBlank()) escopo.launch { avisos.showSnackbar("Nenhum texto copiado") } else Leitura.ler(contexto, t)
    }

    var ajustes by rememberSaveable { mutableStateOf(false) }

    var confirmar by rememberSaveable { mutableStateOf(false) }
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val exportando by vm.exportando.collectAsStateWithLifecycle()
    val aviso by vm.aviso.collectAsStateWithLifecycle()
    LaunchedEffect(aviso) { aviso?.let { avisos.showSnackbar(it); vm.avisoLido() } }
    val criarMp3 = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("audio/mpeg"),
    ) { uri -> if (uri != null) vm.exportar(uri) }
    val exportar: () -> Unit = {
        if (prefs.chave.isBlank()) escopo.launch { avisos.showSnackbar("O MP3 usa a voz da Azure. Configure a chave em Ajustes.") }
        else confirmar = true
    }
    if (confirmar) {
        val texto = Leitura.texto.value
        val cps by Leitura.cps.collectAsStateWithLifecycle()
        val gasto = LeituraServico.semLinks(texto).trim().length.toLong()
        val restam = (COTA_AZURE - prefs.caracteres).coerceAtLeast(0)
        val minutos = Math.round(gasto / cps / 60f).coerceAtLeast(1)
        val cabe = gasto <= restam
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Exportar MP3") },
            text = {
                Text(
                    "Cerca de $minutos min de áudio, %,d caracteres.\nRestam %,d este mês.".format(Locale("pt", "BR"), gasto, restam) +
                        if (cabe) "" else "\n\nNão cabe na cota do mês.",
                )
            },
            confirmButton = {
                TextButton(enabled = cabe && gasto > 0, onClick = { confirmar = false; criarMp3.launch("LePraMim_" + java.text.SimpleDateFormat("dd-MM-yy_HH-mm", Locale.ROOT).format(java.util.Date()) + ".mp3") }) { Text("Exportar") }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(avisos) }) { margens ->
        Column(
            Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(Espaco.gg),
            verticalArrangement = Arrangement.spacedBy(Espaco.gg),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = Espaco.m, start = Espaco.p), verticalAlignment = Alignment.CenterVertically) {
                Text("Lê Pra Mim", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { ajustes = true }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = MaterialTheme.colorScheme.primary)
                }
            }
            CartaoLeitura(colar, exportar, exportando)
        }
    }

    if (ajustes) FolhaAjustes(vm) { ajustes = false }
}

/** Páginas de Ajustes, como no Escala de Trabalho: uma lista de categorias, cada uma abre a sua página. */
private enum class Pagina(val titulo: String, val icone: androidx.compose.ui.graphics.vector.ImageVector) {
    VOZ("Voz", Icons.AutoMirrored.Outlined.VolumeUp),
    ATALHOS("Atalhos", Icons.Outlined.TouchApp),
    APARENCIA("Aparência", Icons.Outlined.Palette),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolhaAjustes(vm: LeitorViewModel, aoFechar: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val contexto = LocalContext.current
        val prefs by vm.prefs.collectAsStateWithLifecycle()
        var nome by rememberSaveable { mutableStateOf<String?>(null) }
        val pagina = nome?.let { Pagina.valueOf(it) }
        // Dentro de uma página, voltar leva à lista; na lista, fecha a folha.
        androidx.activity.compose.BackHandler(enabled = pagina != null) { nome = null }
        var ligada by remember { mutableStateOf(acessibilidadeLigada(contexto)) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ligada = acessibilidadeLigada(contexto) }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Espaco.gg).padding(bottom = Espaco.xg),
            verticalArrangement = Arrangement.spacedBy(Espaco.g),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pagina != null) IconButton(onClick = { nome = null }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                }
                Text(pagina?.titulo ?: "Ajustes", style = MaterialTheme.typography.headlineSmall)
            }
            when (pagina) {
                null -> {
                    val escuro = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val corCartao = if (escuro) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pagina.entries.forEach { p ->
                            Card(
                                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { nome = p.name },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = corCartao),
                            ) {
                                ListItem(
                                    headlineContent = { Text(p.titulo) },
                                    supportingContent = {
                                        Text(when (p) {
                                            Pagina.VOZ -> (if (prefs.azure) "Azure" else "Aparelho") + " · " + String.format(Locale("pt", "BR"), "%.1fx", prefs.velocidade)
                                            Pagina.ATALHOS -> "Marcar na tela: " + if (ligada) "ligado" else "desligado"
                                            Pagina.APARENCIA -> nomeDaCor(prefs.cor)
                                        })
                                    },
                                    leadingContent = { Icon(p.icone, contentDescription = null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            }
                        }
                    }
                }
                Pagina.VOZ -> CartaoVoz(vm)
                Pagina.ATALHOS -> CartaoAtalhos()
                Pagina.APARENCIA -> {
                    Row(Modifier.fillMaxWidth().padding(vertical = Espaco.gg), horizontalArrangement = Arrangement.SpaceBetween) {
                        CorDestaque.entries.forEach { opcao ->
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(amostraDe(opcao))
                                    .then(if (opcao == prefs.cor) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { vm.definirCor(opcao) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartaoLeitura(colar: () -> Unit, exportar: () -> Unit, exportando: String) {
    val contexto = LocalContext.current
    val texto by Leitura.texto.collectAsStateWithLifecycle()
    val trecho by Leitura.trecho.collectAsStateWithLifecycle()
    val situacao by Leitura.situacao.collectAsStateWithLifecycle()
    val cps by Leitura.cps.collectAsStateWithLifecycle()
    val destaque = MaterialTheme.colorScheme.primaryContainer
    val noDestaque = MaterialTheme.colorScheme.onPrimaryContainer
    var editando by rememberSaveable { mutableStateOf(false) }
    var rascunho by rememberSaveable { mutableStateOf("") }
    val foco = remember { FocusRequester() }
    LaunchedEffect(editando) { if (editando) foco.requestFocus() }
    val editar = {
        // A leitura para enquanto o texto é editado.
        if (situacao == Situacao.TOCANDO) Leitura.comando(contexto, Leitura.ACAO_PAUSAR)
        rascunho = texto
        editando = true
    }

    Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)) {
        Box(Modifier.animateContentSize()) {
        if (editando) {
            Column(Modifier.fillMaxWidth().padding(Espaco.gg), verticalArrangement = Arrangement.spacedBy(Espaco.g)) {
                Text("Editar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = rascunho, onValueChange = { rascunho = it },
                    placeholder = { Text("Digite ou cole o texto") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 360.dp).focusRequester(foco),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { editando = false }) { Text("Cancelar") }
                    Spacer(Modifier.size(Espaco.m))
                    Button(onClick = { editando = false; Leitura.definir(contexto, rascunho) }, enabled = rascunho.isNotBlank()) { Text("Salvar") }
                }
            }
            return@Card
        }
        if (texto.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(Espaco.xg), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.lepramim.R.drawable.banner), contentDescription = null,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).alpha(0.15f),
                )
                Spacer(Modifier.height(Espaco.m))
                Text("Nenhum texto", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Espaco.gg))
                Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
                    OutlinedButton(onClick = colar) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(Espaco.m))
                        Text("Colar")
                    }
                    OutlinedButton(onClick = editar) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(Espaco.m))
                        Text("Editar")
                    }
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
                IconButton(onClick = editar) { Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { Leitura.copiarTexto(contexto) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = colar) { Icon(Icons.Outlined.ContentPaste, contentDescription = "Colar", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { Leitura.limpar(contexto) }) { Icon(Icons.Outlined.Delete, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.primary) }
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
                SelectionContainer {
                Text(
                    anotado,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f),
                    onTextLayout = { layout = it },
                )
                }
            }
            Spacer(Modifier.height(Espaco.g))
            var arrastando by remember { mutableStateOf<Float?>(null) }
            val fracao = if (r == null || texto.isEmpty()) 0f else (r.first.toFloat() / texto.length).coerceIn(0f, 1f)
            Slider(
                value = arrastando ?: fracao,
                onValueChange = { arrastando = it },
                onValueChangeFinished = {
                    arrastando?.let { Leitura.ir(contexto, (it * texto.length).toInt()) }
                    arrastando = null
                },
                enabled = situacao != Situacao.PARADO,
                thumb = { Box(Modifier.size(20.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) },
                track = { estado ->
                    SliderDefaults.Track(
                        sliderState = estado, thumbTrackGapSize = 0.dp, trackInsideCornerSize = 0.dp,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                            disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        ),
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledThumbColor = MaterialTheme.colorScheme.primary,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                ),
            )
            Text(
                Leitura.tempoTexto(arrastando?.let { (it * texto.length).toInt() } ?: (r?.first ?: 0), texto.length, cps),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Espaco.g))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Leitura.comando(contexto, Leitura.ACAO_VOLTAR) }) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.primary)
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
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Avançar", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(Espaco.m))
            TextButton(onClick = exportar, enabled = exportando.isEmpty(), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Espaco.m))
                Text(if (exportando.isEmpty()) "Exportar" else "Exportando $exportando")
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

    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Espaco.g)) {
            Seletor(
                rotulo = "Motor",
                valor = if (prefs.azure) "Azure" else "Aparelho",
                opcoes = listOf("" to "Aparelho", "azure" to "Azure"),
                escolher = vm::definirMotor,
            )
            if (prefs.azure) BlocoAzure(vm)
            else if (vozes.isNotEmpty()) Seletor(
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

private const val COTA_AZURE = 500_000L
private const val DEZ_MESES = 300L * 24 * 3600 * 1000

@Composable
private fun BlocoAzure(vm: LeitorViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val vozes by vm.vozesAzure.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var regiao by remember(prefs.regiao) { mutableStateOf(prefs.regiao) }

    OutlinedTextField(
        value = regiao, onValueChange = { regiao = it; vm.definirRegiao(it) },
        singleLine = true, label = { Text("Região") }, modifier = Modifier.fillMaxWidth(),
    )
    val velha = prefs.chave.isNotEmpty() && prefs.chaveEm > 0 && System.currentTimeMillis() - prefs.chaveEm > DEZ_MESES
    OutlinedTextField(
        value = if (prefs.chave.isEmpty()) "" else "••••••••••••",
        onValueChange = {}, readOnly = true, singleLine = true,
        label = { Text("Chave") },
        placeholder = { Text("Cole a chave") },
        isError = velha,
        supportingText = when {
            velha -> ({ Text("Mais de 10 meses: troque no portal.") })
            mensagem.isNotEmpty() -> ({ Text(mensagem) })
            else -> null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (prefs.chave.isEmpty()) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            val cm = contexto.getSystemService(android.content.ClipboardManager::class.java)
            val t = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
            if (t.isNotEmpty()) { vm.colarChave(t); cm.clearPrimaryClip() }
        }) { Text("Colar") }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
            Button(modifier = Modifier.weight(1f), onClick = vm::testar) { Text("Testar") }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = vm::removerChave) { Text("Remover") }
        }
    }
    Seletor(
        rotulo = "Voz",
        valor = vozes.find { it.id == prefs.vozAzure }?.nome ?: prefs.vozAzure.removePrefix("pt-BR-").removeSuffix("Neural"),
        opcoes = vozes.map { it.id to it.nome }.ifEmpty { listOf("pt-BR-FranciscaNeural" to "Francisca", "pt-BR-AntonioNeural" to "Antonio", "pt-BR-ThalitaNeural" to "Thalita") },
        escolher = vm::definirVozAzure,
    )
    Text("Mês: %,d de %,d caracteres".format(Locale("pt", "BR"), prefs.caracteres, COTA_AZURE), style = MaterialTheme.typography.bodySmall,
        color = if (prefs.caracteres > COTA_AZURE * 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Ordem da lista mantida; a escolhida fica em destaque.
            opcoes.forEach { (id, nome) ->
                val atual = nome == valor
                DropdownMenuItem(
                    text = { Text(nome, color = if (atual) MaterialTheme.colorScheme.primary else Color.Unspecified, fontWeight = if (atual) FontWeight.Bold else null) },
                    onClick = { escolher(id); aberto = false },
                )
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

    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.fillMaxWidth().padding(vertical = Espaco.m)) {
            ListItem(
                colors = cores,
                leadingContent = { Icon(Icons.Outlined.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text("Marcar na tela") },
                supportingContent = { Text(if (ligada) "Ligado" else "Desligado") },
                trailingContent = {
                    TextButton(onClick = {
                        contexto.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(if (ligada) "Alterar" else "Ativar") }
                },
            )
            ListItem(
                colors = cores,
                leadingContent = { Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text("Botão Ler") },
                supportingContent = { Text("Para ler o texto copiado: puxe a Central de Controle, toque em Editar e arraste o botão Ler.") },
            )
        }
    }
}
