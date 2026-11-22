package com.lepramim.ui.leitor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.leitura.Leitura
import com.lepramim.leitura.Situacao
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.Rota
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.lepramim.ui.ajustes.VOZES_AZURE_PADRAO
import com.lepramim.ui.ajustes.nomeDaVozAzure
import com.lepramim.ui.theme.Espaco
import com.lepramim.ui.theme.familia
import kotlinx.coroutines.launch

/** Tela principal: o texto ocupa tudo; em cima ficam as ações e embaixo o player. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaLeitor(vm: LeitorViewModel, avisos: SnackbarHostState, navegar: (Rota) -> Unit) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val foco: FocusManager = LocalFocusManager.current

    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val exportando by vm.exportando.collectAsStateWithLifecycle()
    val texto by Leitura.texto.collectAsStateWithLifecycle()
    val trecho by Leitura.trecho.collectAsStateWithLifecycle()
    val situacao by Leitura.situacao.collectAsStateWithLifecycle()
    val carregando by Leitura.carregando.collectAsStateWithLifecycle()
    val cps by Leitura.cps.collectAsStateWithLifecycle()

    var vozAberta by rememberSaveable { mutableStateOf(false) }
    var confirmar by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    // O teclado só abre com a edição ligada; por padrão o texto é só para leitura.
    var editando by rememberSaveable { mutableStateOf(false) }

    val colar: () -> Unit = {
        val t = Leitura.copiado(contexto)
        if (t.isNullOrBlank()) { escopo.launch { avisos.showSnackbar("Nenhum texto copiado") } } else Leitura.ler(contexto, t)
    }
    val criarMp3 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri ->
        if (uri != null) vm.exportar(uri)
    }
    val abrirTxt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importar(uri)
    }
    val exportar: () -> Unit = {
        if (prefs.chave.isBlank()) {
            escopo.launch {
                val r = avisos.showSnackbar("O MP3 usa a voz da Azure.", actionLabel = "Ajustes")
                if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) navegar(Rota.AZURE)
            }
        } else {
            confirmar = true
        }
    }

    if (confirmar) DialogoExportar(
        texto = texto, cps = cps, prefs = prefs,
        aoConfirmar = { confirmar = false; criarMp3.launch(nomeDoMp3()) },
        aoCancelar = { confirmar = false },
    )

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent,
                ),
                title = {
                    // O seletor só existe com a Azure; a voz do aparelho se escolhe em Ajustes.
                    if (prefs.azure) {
                        val vozesAzure by vm.vozesAzure.collectAsStateWithLifecycle()
                        val opcoes = vozesAzure.map { it.id to it.nome }.ifEmpty { VOZES_AZURE_PADRAO }
                        Box {
                            AssistChip(
                                onClick = { vozAberta = true },
                                label = { Text(nomeDaVozAzure(prefs.vozAzure, opcoes)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                            // Um toque na voz já troca, e a leitura continua do ponto em que estava.
                            DropdownMenu(expanded = vozAberta, onDismissRequest = { vozAberta = false }) {
                                // Só as favoritas (definidas em Ajustes → Voz); sem favoritas, todas.
                                val lista = opcoes.filter { it.first in prefs.favoritas || it.first == prefs.vozAzure }
                                    .takeIf { prefs.favoritas.isNotEmpty() } ?: opcoes
                                lista.forEach { (id, nome) ->
                                    val atual = id == prefs.vozAzure
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                nome,
                                                color = if (atual) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                                fontWeight = if (atual) FontWeight.Bold else null,
                                            )
                                        },
                                        onClick = { vozAberta = false; vm.definirVozAzure(id) },
                                    )
                                }
                            }
                        }
                    } else Text(Rota.LEITOR.titulo)
                },
                actions = {
                    IconButton(onClick = { editando = !editando }) {
                        Icon(
                            Icons.Outlined.Edit, contentDescription = "Editar",
                            tint = if (editando) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = colar) { Icon(Icons.Outlined.ContentPaste, contentDescription = "Colar") }
                    IconButton(onClick = { navegar(Rota.AJUSTES) }) { Icon(Icons.Outlined.Settings, contentDescription = "Ajustes") }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Mais") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        val temTexto = texto.isNotEmpty()
                        DropdownMenuItem(
                            text = { Text("Importar") },
                            leadingIcon = { Icon(Icons.Outlined.FileOpen, contentDescription = null) },
                            onClick = { menu = false; abrirTxt.launch(arrayOf("text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        )
                        DropdownMenuItem(
                            text = { Text("Copiar") }, enabled = temTexto,
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = { menu = false; Leitura.copiarTexto(contexto) },
                        )
                        DropdownMenuItem(
                            text = { Text("Limpar") }, enabled = temTexto,
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { menu = false; Leitura.limpar(contexto) },
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar") }, enabled = temTexto && exportando.isEmpty(),
                            leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                            onClick = { menu = false; exportar() },
                        )
                    }
                },
            )
            val usados = prefs.portalNoMes ?: prefs.usadosNoMes
            val cota = com.lepramim.data.Preferencias.COTA_AZURE
            val perto = prefs.azure && usados > cota * 0.9
            val corTexto = if (perto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        if (prefs.azure) "Mês %,d / %d mil".format(java.util.Locale("pt", "BR"), usados, cota / 1000) else "",
                        style = MaterialTheme.typography.labelMedium, color = corTexto,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (exportando.isNotEmpty()) "Exportando $exportando" else "Texto %,d".format(java.util.Locale("pt", "BR"), texto.length),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (prefs.azure) androidx.compose.material3.LinearProgressIndicator(
                    progress = { (usados.toFloat() / cota).coerceIn(0f, 1f) },
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                    color = if (perto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                trackColor = com.lepramim.ui.theme.trilhoBarra(),
                )
            }
            }
            }
        },
        bottomBar = {
            BarraPlayer(
                total = texto.length, trecho = trecho, situacao = situacao, carregando = carregando, cps = cps, exportando = exportando,
                uso = if (prefs.azure) (prefs.portalNoMes ?: prefs.usadosNoMes) to com.lepramim.data.Preferencias.COTA_AZURE else null,
                aoAlternar = {
                    foco.clearFocus()
                    Leitura.comando(contexto, if (situacao == Situacao.TOCANDO) Leitura.ACAO_PAUSAR else Leitura.ACAO_CONTINUAR)
                },
                aoVoltar = { Leitura.comando(contexto, Leitura.ACAO_VOLTAR) },
                aoAvancar = { Leitura.comando(contexto, Leitura.ACAO_AVANCAR) },
                aoIr = { Leitura.ir(contexto, it) },
                velocidade = prefs.velocidade, aoMudarVelocidade = vm::definirVelocidade,
            )
        },
        snackbarHost = { com.lepramim.ui.componentes.HostAvisos(avisos) },
    ) { margens ->
        Editor(
            texto = texto, trecho = trecho, destacar = prefs.destacar, editavel = editando,
            tamanho = prefs.tamanho.sp, fonte = prefs.fonte.familia(),
            aoEditar = { Leitura.editar(contexto, it) },
            aoColar = colar,
            modifier = Modifier.padding(margens),
        )
    }
}
