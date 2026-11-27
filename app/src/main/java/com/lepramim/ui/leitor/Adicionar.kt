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

/** Aba Recursos: de onde vem o texto. */
@Composable
fun PaginaRecursos(vm: LeitorViewModel, avisos: SnackbarHostState, aoVoltar: () -> Unit) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    val abrirDocumento = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { vm.importar(uri); aoVoltar() }
    }
    // Scanner de documento do Google: câmera com os quatro cantos e correção de perspectiva, ou imagem da galeria.
    val escanear = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val resultado = GmsDocumentScanningResult.fromActivityResultIntent(r.data)
        val paginas = resultado?.pages?.map { it.imageUri }.orEmpty()
        if (paginas.isNotEmpty()) { vm.lerImagens(paginas); aoVoltar() }
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
                Cartao("Escrever texto", listOf("Digitar", "Colar"), { vm.pedirEdicao(); aoVoltar() }) { IconeGrande(Icons.Outlined.Edit) }
                Spacer(Modifier.weight(1f))
            }
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
