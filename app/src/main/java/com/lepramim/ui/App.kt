package com.lepramim.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.lepramim.ui.ajustes.PaginaAparencia
import com.lepramim.ui.ajustes.PaginaAtalhos
import com.lepramim.ui.ajustes.PaginaAzure
import com.lepramim.ui.ajustes.PaginaFiltros
import com.lepramim.ui.ajustes.PaginaVoz
import com.lepramim.ui.ajustes.TelaAjustes
import com.lepramim.ui.leitor.TelaLeitor

/** Raiz do app: escolhe a tela pela pilha de rotas e mostra os avisos do ViewModel. */
@Composable
fun AppLeitor(vm: LeitorViewModel) {
    val avisos = remember { SnackbarHostState() }
    val aviso by vm.aviso.collectAsStateWithLifecycle()
    LaunchedEffect(aviso) { aviso?.let { avisos.showSnackbar(it); vm.avisoLido() } }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_START) { com.lepramim.leitura.Leitura.appVisivel = true }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { com.lepramim.leitura.Leitura.appVisivel = false }
    LaunchedEffect(Unit) { com.lepramim.leitura.Leitura.avisos.collect { avisos.showSnackbar(it) } }

    // Cada vez que o app volta para a frente, busca de novo o consumo do portal.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val p = vm.prefs.value
        if (p.azure && p.monitorPronto) vm.atualizarPortal(mostrarErro = false)
    }

    var pilha by rememberSaveable { mutableStateOf(listOf(Rota.LEITOR)) }
    val navegar = { r: Rota -> pilha = pilha + r }
    val voltar = { if (pilha.size > 1) pilha = pilha.dropLast(1) }
    BackHandler(enabled = pilha.size > 1) { voltar() }
    // O pager fica aqui para guardar a aba escolhida ao ir para outras telas e voltar.
    val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val escopoApp = androidx.compose.runtime.rememberCoroutineScope()

    Crossfade(pilha.last(), label = "rota") { rota ->
        when (rota) {
            Rota.LEITOR -> Inicio(vm, avisos, navegar, pager)
            Rota.AJUSTES -> TelaAjustes(vm, avisos, navegar, voltar)
            Rota.VOZ -> PaginaVoz(vm, avisos, navegar, voltar)
            Rota.AZURE -> PaginaAzure(vm, avisos, voltar)
            Rota.FILTROS -> PaginaFiltros(vm, avisos, voltar)
            Rota.APARENCIA -> PaginaAparencia(vm, avisos, voltar)
            Rota.ATALHOS -> PaginaAtalhos(avisos, voltar)
            Rota.ESCREVER -> com.lepramim.ui.leitor.TelaEscrever(vm, voltar) { voltar(); escopoApp.launch { pager.scrollToPage(0) } }
        }
    }
}

/** Início: abas Leitura e Recursos no topo de tudo; também dá para deslizar de uma para a outra. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun Inicio(vm: LeitorViewModel, avisos: SnackbarHostState, navegar: (Rota) -> Unit, pager: androidx.compose.foundation.pager.PagerState) {
    val escopo = androidx.compose.runtime.rememberCoroutineScope()
    val irPara: (Int) -> Unit = { escopo.launch { pager.animateScrollToPage(it) } }
    BackHandler(enabled = pager.currentPage == 1) { irPara(0) }
    androidx.compose.foundation.layout.Column(
        androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
    ) {
        // Abas e, na direita, a engrenagem dos Ajustes (vale para o app todo).
        androidx.compose.foundation.layout.Row(
            androidx.compose.ui.Modifier.fillMaxWidth().statusBarsPadding()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
        androidx.compose.material3.SecondaryTabRow(
            selectedTabIndex = pager.currentPage,
            modifier = androidx.compose.ui.Modifier.weight(1f),
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = {
                // Fino e arredondado em cima, na cor forte do tema.
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.tabIndicatorOffset(pager.currentPage)
                        .height(3.dp)
                        .background(
                            androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                        ),
                )
            },
        ) {
            listOf("Leitura", "Recursos").forEachIndexed { i, nome ->
                val ativa = pager.currentPage == i
                // A aba selecionada tem o fundo na cor do tema (suave); a outra fica sem fundo.
                val fundo by androidx.compose.animation.animateColorAsState(
                    if (ativa) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    label = "aba",
                )
                androidx.compose.material3.Tab(
                    selected = ativa, onClick = { irPara(i) },
                    modifier = androidx.compose.ui.Modifier.background(fundo),
                    text = { androidx.compose.material3.Text(nome) },
                    unselectedContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        androidx.compose.material3.IconButton(onClick = { navegar(Rota.AJUSTES) }) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Settings, contentDescription = "Ajustes", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        }
        }
        androidx.compose.foundation.pager.HorizontalPager(
            pager, beyondViewportPageCount = 1,
            // A barra de status já é coberta pelas abas; as páginas não somam o espaço de novo.
            modifier = androidx.compose.ui.Modifier.weight(1f).consumeWindowInsets(androidx.compose.foundation.layout.WindowInsets.statusBars),
        ) { pagina ->
            if (pagina == 0) TelaLeitor(vm, avisos, navegar) else com.lepramim.ui.leitor.PaginaRecursos(vm, avisos, navegar) { irPara(0) }
        }
    }
}
