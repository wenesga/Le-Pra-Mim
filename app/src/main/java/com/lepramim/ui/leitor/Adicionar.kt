package com.lepramim.ui.leitor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.lepramim.ui.componentes.Secao
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.componentes.HostAvisos
import com.lepramim.ui.theme.Espaco
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.data.ItemBiblioteca
import com.lepramim.ui.Rota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Aba Recursos: de onde vem o texto. */
@Composable
fun PaginaRecursos(vm: LeitorViewModel, avisos: SnackbarHostState, navegar: (Rota) -> Unit, aoAbrir: () -> Unit) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var linkAberto by rememberSaveable { mutableStateOf(false) }

    val abrirDocumento = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importar(uri)
    }
    // Scanner de documento do Google: câmera com os quatro cantos e correção de perspectiva, ou imagem da galeria.
    val escanear = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val resultado = GmsDocumentScanningResult.fromActivityResultIntent(r.data)
        val paginas = resultado?.pages?.map { it.imageUri }.orEmpty()
        if (paginas.isNotEmpty()) vm.lerImagens(paginas)
    }
    val digitalizar: () -> Unit = {
        val opcoes = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .build()
        val atividade = contexto as? android.app.Activity
        if (atividade != null) {
            GmsDocumentScanning.getClient(opcoes).getStartScanIntent(atividade)
                .addOnSuccessListener { escanear.launch(IntentSenderRequest.Builder(it).build()) }
                .addOnFailureListener { escopo.launch { avisos.showSnackbar("O scanner não está disponível neste aparelho") } }
        }
    }

    Scaffold(snackbarHost = { HostAvisos(avisos) }, contentWindowInsets = WindowInsets.navigationBars) { margens ->
    Column(
        Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(Espaco.gg),
        verticalArrangement = Arrangement.spacedBy(Espaco.xg),
    ) {
        Secao("Recursos") {
            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
                Cartao(
                    "Carregar", listOf("TXT", "Word", "PDF"),
                    { abrirDocumento.launch(arrayOf("text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf")) },
                ) { IconeGrande(Icons.Outlined.FileOpen) }
                Cartao("Digitalizar", listOf("Câmera", "Imagem"), digitalizar) { IconeGrande(Icons.Outlined.DocumentScanner) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
                Cartao("Link da web", listOf("Site", "Artigo"), { linkAberto = true }) { IconeGrande(Icons.Outlined.Link) }
                Cartao("Escrever texto", listOf("Digitar", "Colar"), { navegar(Rota.ESCREVER) }) { IconeGrande(Icons.Outlined.Edit) }
            }
        }
        Secao("Biblioteca") { ListaBiblioteca(vm, aoAbrir) }
        if (linkAberto) DialogoLink(aoConfirmar = { vm.guardarLink(it); linkAberto = false }, aoCancelar = { linkAberto = false })
    }
    }
}

/** Pede o endereço da página para guardar o texto dela na biblioteca. */
@Composable
private fun DialogoLink(aoConfirmar: (String) -> Unit, aoCancelar: () -> Unit) {
    val contexto = LocalContext.current
    // Se o que foi copiado é um link, já vem no campo.
    var endereco by rememberSaveable {
        mutableStateOf(com.lepramim.leitura.Leitura.copiado(contexto)?.trim()?.takeIf { it.startsWith("http", ignoreCase = true) && !it.contains(' ') }.orEmpty())
    }
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Link da web") },
        text = {
            OutlinedTextField(
                value = endereco, onValueChange = { endereco = it },
                placeholder = { Text("https://exemplo.com/…") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
            )
        },
        confirmButton = { TextButton(onClick = { aoConfirmar(endereco) }, enabled = endereco.isNotBlank()) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } },
    )
}

/** Textos guardados: toque abre na Leitura; o menu renomeia ou exclui. */
@Composable
private fun ListaBiblioteca(vm: LeitorViewModel, aoAbrir: () -> Unit) {
    val itens by vm.biblioteca.collectAsStateWithLifecycle()
    var busca by rememberSaveable { mutableStateOf("") }
    var renomear by remember { mutableStateOf<ItemBiblioteca?>(null) }
    var excluir by remember { mutableStateOf<ItemBiblioteca?>(null) }

    if (itens.isEmpty()) {
        Text("Nada guardado ainda. O que você escrever, carregar ou digitalizar fica aqui.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    OutlinedTextField(
        value = busca, onValueChange = { busca = it },
        placeholder = { Text("Buscar") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    val visiveis = itens.filter { busca.isBlank() || it.titulo.contains(busca, ignoreCase = true) || it.texto.contains(busca, ignoreCase = true) }
    if (visiveis.isEmpty()) Text("Nada encontrado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    visiveis.forEach { item ->
        LinhaBiblioteca(item, aoAbrir = { vm.abrirDaBiblioteca(item); aoAbrir() }, aoRenomear = { renomear = item }, aoExcluir = { excluir = item })
    }

    renomear?.let { item ->
        var novo by remember(item.id) { mutableStateOf(item.titulo) }
        AlertDialog(
            onDismissRequest = { renomear = null },
            title = { Text("Renomear") },
            text = { OutlinedTextField(value = novo, onValueChange = { novo = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.renomearItem(item.id, novo); renomear = null }, enabled = novo.isNotBlank()) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { renomear = null }) { Text("Cancelar") } },
        )
    }
    excluir?.let { item ->
        AlertDialog(
            onDismissRequest = { excluir = null },
            title = { Text("Excluir") },
            text = { Text("Excluir \"${item.titulo}\" da biblioteca? Não dá para desfazer.") },
            confirmButton = { TextButton(onClick = { vm.excluirItem(item.id); excluir = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { excluir = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LinhaBiblioteca(item: ItemBiblioteca, aoAbrir: () -> Unit, aoRenomear: () -> Unit, aoExcluir: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val data = remember(item.criado) { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(item.criado)) }
    val tamanho = remember(item.texto) { String.format(Locale("pt", "BR"), "%,d", item.texto.length) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = aoAbrir).padding(start = Espaco.g, top = Espaco.m, bottom = Espaco.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Column(Modifier.weight(1f).padding(horizontal = Espaco.g)) {
            Text(item.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$data · $tamanho car.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Mais") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Renomear") }, onClick = { menu = false; aoRenomear() })
                DropdownMenuItem(text = { Text("Excluir") }, onClick = { menu = false; aoExcluir() })
            }
        }
    }
}

/** Cartão de entrada: fundo em degradê na cor do tema, brilho atrás do ícone e só o nome embaixo. */
@Composable
private fun RowScope.Cartao(titulo: String, etiquetas: List<String>, aoClicar: () -> Unit, figura: @Composable () -> Unit) {
    val forte = MaterialTheme.colorScheme.primaryContainer
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val forma = RoundedCornerShape(20.dp)
    Box(
        Modifier.weight(1f).clip(forma)
            .background(Brush.linearGradient(listOf(forte.copy(alpha = 0.25f), base, base)))
            .border(1.dp, forte.copy(alpha = 0.25f), forma)
            .clickable(onClick = aoClicar),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = Espaco.m, end = Espaco.m, top = 0.dp, bottom = Espaco.g), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) { figura() }
            Spacer(Modifier.height(Espaco.m))
            Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { etiquetas.forEach { Etiqueta(it) } }
        }
    }
}

/** Etiqueta pequena: fundo suave na cor do tema e texto na cor de destaque. */
@Composable
private fun Etiqueta(texto: String) {
    Text(
        texto, maxLines = 1, softWrap = false,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun IconeGrande(icone: ImageVector) {
    // O ícone esmaece de cima para baixo, como a figura dos cartões do outro app.
    Icon(
        icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(60.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, 0.45f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            },
    )
}
