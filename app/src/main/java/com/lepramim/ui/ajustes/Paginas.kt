package com.lepramim.ui.ajustes

import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lepramim.atalhos.MarcarTela
import com.lepramim.data.CorDestaque
import com.lepramim.data.FundoEscuro
import com.lepramim.data.Preferencias
import com.lepramim.data.TemaApp
import com.lepramim.ui.LeitorViewModel
import com.lepramim.ui.Rota
import com.lepramim.ui.componentes.LinhaSwitch
import com.lepramim.ui.componentes.Pagina
import com.lepramim.ui.componentes.Secao
import com.lepramim.ui.theme.Espaco
import com.lepramim.ui.theme.familia
import com.lepramim.ui.theme.amostraDe
import com.lepramim.ui.theme.amostraDoFundo
import com.lepramim.ui.theme.nomeDaCor
import java.util.Locale

private val PT = Locale("pt", "BR")

private fun acessibilidadeLigada(c: Context): Boolean {
    val lista = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    return lista.split(':').any { ComponentName.unflattenFromString(it) == ComponentName(c, MarcarTela::class.java) }
}

/** Estado da acessibilidade, atualizado ao voltar das configurações do Android. */
@Composable
private fun rememberAcessibilidade(): Boolean {
    val contexto = LocalContext.current
    var ligada by remember { mutableStateOf(acessibilidadeLigada(contexto)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ligada = acessibilidadeLigada(contexto) }
    return ligada
}

// ---------------------------------------------------------------- Lista

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAjustes(vm: LeitorViewModel, avisos: SnackbarHostState, navegar: (Rota) -> Unit, voltar: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val acessibilidade = rememberAcessibilidade()
    val itens: List<Triple<Rota, ImageVector, String>> = listOf(
        Triple(Rota.VOZ, Icons.AutoMirrored.Outlined.VolumeUp,
            (if (prefs.azure) "Azure" else "Aparelho") + " · " + formatarVelocidade(prefs.velocidade)),
        Triple(Rota.AZURE, Icons.Outlined.Cloud, if (prefs.chave.isEmpty()) "Sem chave" else "Chave configurada"),
        Triple(Rota.FILTROS, Icons.Outlined.FilterAlt,
            (if (prefs.markdown) "Markdown" else "Sem Markdown") + " · " + prefs.filtros.size + " próprios"),
        Triple(Rota.APARENCIA, Icons.Outlined.Palette,
            when (prefs.tema) { TemaApp.SISTEMA -> "Sistema"; TemaApp.CLARO -> "Claro"; TemaApp.ESCURO -> "Escuro" } + " · " + nomeDaCor(prefs.cor)),
        Triple(Rota.ATALHOS, Icons.Outlined.TouchApp, "Marcar na tela: " + if (acessibilidade) "ligado" else "desligado"),
    )
    Pagina(Rota.AJUSTES.titulo, voltar, avisos) {
        Column {
            itens.forEach { (rota, icone, resumo) ->
                ListItem(
                    modifier = Modifier.clickable { navegar(rota) },
                    leadingContent = { Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text(rota.titulo) },
                    supportingContent = { Text(resumo) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Voz

@Composable
fun PaginaVoz(vm: LeitorViewModel, avisos: SnackbarHostState, navegar: (Rota) -> Unit, voltar: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    Pagina(Rota.VOZ.titulo, voltar, avisos) {
        Secao("Motor") { SeletorMotor(vm) }
        Secao("Voz") {
            if (prefs.azure) {
                if (prefs.chave.isEmpty()) {
                    Text("Falta a chave da Azure.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { navegar(Rota.AZURE) }) { Text("Configurar") }
                }
                SeletorVozAzure(vm)
            } else SeletorVozAparelho(vm)
            ControleVelocidade(vm)
        }
        Secao("Leitura") {
            LinhaSwitch("Destacar", prefs.destacar, vm::definirDestacar, resumo = "Marca o que está sendo lido. Frase na Azure; palavra, quando o motor do aparelho informa.")
        }
    }
}

// ---------------------------------------------------------------- Azure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginaAzure(vm: LeitorViewModel, avisos: SnackbarHostState, voltar: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var regiao by remember(prefs.regiao) { mutableStateOf(prefs.regiao) }

    Pagina(Rota.AZURE.titulo, voltar, avisos) {
        Secao("Conexão") {
            OutlinedTextField(
                value = regiao, onValueChange = { regiao = it; vm.definirRegiao(it) },
                singleLine = true, label = { Text("Região") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = if (prefs.chave.isEmpty()) "" else "••••••••••••",
                onValueChange = {}, readOnly = true, singleLine = true,
                label = { Text("Chave") }, placeholder = { Text("Cole a chave") },
                isError = prefs.chaveVelha,
                supportingText = when {
                    prefs.chaveVelha -> ({ Text("Mais de 10 meses: troque no portal.") })
                    mensagem.isNotEmpty() -> ({ Text(mensagem) })
                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (prefs.chave.isEmpty()) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    val cm = contexto.getSystemService(ClipboardManager::class.java)
                    val t = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
                    if (t.isNotEmpty()) { vm.colarChave(t); cm.clearPrimaryClip() }
                }) { Text("Colar") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
                    Button(modifier = Modifier.weight(1f), onClick = vm::testar) { Text("Testar") }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = vm::removerChave) { Text("Remover") }
                }
            }
        }
        Secao("Uso do mês") {
            val usados = prefs.portalNoMes ?: prefs.usadosNoMes
            val cota = Preferencias.COTA_AZURE
            val perto = usados > cota * 0.9
            LinearProgressIndicator(
                progress = { (usados.toFloat() / cota).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (perto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text(
                "%,d de %,d caracteres".format(PT, usados, cota),
                style = MaterialTheme.typography.bodySmall,
                color = if (perto) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        var avancado by remember { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().clickable { avancado = !avancado },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Avançado", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Consumo lido do portal da Azure",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (avancado) ModalBottomSheet(onDismissRequest = { avancado = false }) {
          Column(
            Modifier.navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
          ) {
          Secao("Consumo no portal") {
            val mensagemMonitor by vm.mensagemMonitor.collectAsStateWithLifecycle()
            val travado = prefs.monSegredo.isNotEmpty()
            var app by remember(prefs.monApp) { mutableStateOf(prefs.monApp) }
            var tenant by remember(prefs.monTenant) { mutableStateOf(prefs.monTenant) }
            var assinatura by remember(prefs.monAssinatura) { mutableStateOf(prefs.monAssinatura) }
            var recurso by remember(prefs.monRecurso) { mutableStateOf(prefs.monRecurso) }
            OutlinedTextField(
                value = app, readOnly = travado, onValueChange = { app = it; vm.definirMonApp(it) },
                singleLine = true, label = { Text("ID do aplicativo") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tenant, readOnly = travado, onValueChange = { tenant = it; vm.definirMonTenant(it) },
                singleLine = true, label = { Text("ID do diretório") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = recurso, readOnly = travado, onValueChange = { recurso = it; vm.definirMonRecurso(it) },
                singleLine = true, label = { Text("Recurso") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = assinatura, readOnly = travado, onValueChange = { assinatura = it; vm.definirMonAssinatura(it) },
                singleLine = true, label = { Text("ID da assinatura") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = if (prefs.monSegredo.isEmpty()) "" else "••••••••••••",
                onValueChange = {}, readOnly = true, singleLine = true,
                label = { Text("Valor") }, placeholder = { Text("Cole o valor") },
                supportingText = if (mensagemMonitor.isNotEmpty()) ({ Text(mensagemMonitor) }) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (prefs.monSegredo.isEmpty()) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    val cm = contexto.getSystemService(ClipboardManager::class.java)
                    val t = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
                    if (t.isNotEmpty()) { vm.colarSegredo(t); cm.clearPrimaryClip() }
                }) { Text("Colar") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
                    Button(modifier = Modifier.weight(1f), onClick = { vm.atualizarPortal() }) { Text("Atualizar") }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = vm::removerMonitor) { Text("Remover") }
                }
            }
          }
          }
        }
    }
}

// ---------------------------------------------------------------- Filtros

@Composable
fun PaginaFiltros(vm: LeitorViewModel, avisos: SnackbarHostState, voltar: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    var novo by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    val adicionar = {
        erro = vm.adicionarFiltro(novo)
        if (erro == null) novo = ""
    }
    Pagina(Rota.FILTROS.titulo, voltar, avisos) {
        Secao("Padrão") {
            LinhaSwitch("Ignorar Markdown", prefs.markdown, vm::definirMarkdown, resumo = "Não lê asteriscos, crases, # de título nem marcas de lista.")
        }
        Secao("Seus filtros") {
            Text(
                "Cada filtro é uma expressão regular. O que ela encontrar no texto não é lido.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            prefs.filtros.forEach { f ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(f, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.removerFiltro(f) }) { Icon(Icons.Outlined.Close, contentDescription = "Remover") }
                }
            }
            OutlinedTextField(
                value = novo, onValueChange = { novo = it; erro = null },
                singleLine = true, label = { Text("Expressão regular") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                isError = erro != null,
                supportingText = { Text(erro ?: "Só a expressão, sem barras. Exemplo: #+") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { adicionar() }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar") }
        }
    }
}

// ---------------------------------------------------------------- Aparência

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginaAparencia(vm: LeitorViewModel, avisos: SnackbarHostState, voltar: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    Pagina(Rota.APARENCIA.titulo, voltar, avisos) {
        Secao("Texto") {
            val tamanhos = com.lepramim.data.TamanhoTexto.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                tamanhos.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = prefs.tamanho == t,
                        onClick = { vm.definirTamanho(t) },
                        shape = SegmentedButtonDefaults.itemShape(i, tamanhos.size),
                        icon = {},
                    ) { Text(t.nome, maxLines = 1) }
                }
            }
            SeletorFonte(prefs.fonte, prefs.tamanho.sp) { vm.definirFonte(it) }
        }
        Secao("Tema") {
            val temas = listOf(TemaApp.SISTEMA to "Sistema", TemaApp.CLARO to "Claro", TemaApp.ESCURO to "Escuro")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                temas.forEachIndexed { i, (tema, nome) ->
                    SegmentedButton(
                        selected = prefs.tema == tema,
                        onClick = { vm.definirTema(tema) },
                        shape = SegmentedButtonDefaults.itemShape(i, temas.size),
                        icon = {},
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                    ) { Text(nome, maxLines = 1, softWrap = false) }
                }
            }
            val escuroAgora = prefs.tema == TemaApp.ESCURO || (prefs.tema == TemaApp.SISTEMA && androidx.compose.foundation.isSystemInDarkTheme())
            if (escuroAgora) {
                Text("Fundo escuro · " + prefs.fundo.nome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FundoEscuro.entries.forEach { opcao ->
                        val escolhida = opcao == prefs.fundo
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).background(amostraDoFundo(opcao), CircleShape)
                                .border(
                                    if (escolhida) 3.dp else 1.dp,
                                    if (escolhida) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                )
                                .clickable { vm.definirFundo(opcao) },
                        ) {}
                    }
                }
            }
            Text("Destaque · " + nomeDaCor(prefs.cor), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CorDestaque.entries.forEach { opcao ->
                    val escolhida = opcao == prefs.cor
                    Box(
                        Modifier.size(44.dp).background(amostraDe(opcao, escuroAgora, androidx.compose.ui.platform.LocalContext.current), CircleShape)
                            .then(if (escolhida) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { vm.definirCor(opcao) },
                    ) {}
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Atalhos

@Composable
fun PaginaAtalhos(avisos: SnackbarHostState, voltar: () -> Unit) {
    val contexto = LocalContext.current
    val ligada = rememberAcessibilidade()
    val semFundo = ListItemDefaults.colors(containerColor = Color.Transparent)
    Pagina(Rota.ATALHOS.titulo, voltar, avisos) {
        Column {
            ListItem(
                colors = semFundo,
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
                colors = semFundo,
                leadingContent = { Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text("Botão Ler") },
                supportingContent = { Text("Para ler o texto copiado: puxe a Central de Controle, toque em Editar e arraste o botão Ler.") },
            )
        }
    }
}
