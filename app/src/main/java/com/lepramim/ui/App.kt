package com.lepramim.ui

import androidx.activity.compose.BackHandler
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

    // Cada vez que o app volta para a frente, busca de novo o consumo do portal.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val p = vm.prefs.value
        if (p.azure && p.monitorPronto) vm.atualizarPortal(mostrarErro = false)
    }

    var pilha by rememberSaveable { mutableStateOf(listOf(Rota.LEITOR)) }
    val navegar = { r: Rota -> pilha = pilha + r }
    val voltar = { if (pilha.size > 1) pilha = pilha.dropLast(1) }
    BackHandler(enabled = pilha.size > 1) { voltar() }

    Crossfade(pilha.last(), label = "rota") { rota ->
        when (rota) {
            Rota.LEITOR -> TelaLeitor(vm, avisos, navegar)
            Rota.AJUSTES -> TelaAjustes(vm, avisos, navegar, voltar)
            Rota.VOZ -> PaginaVoz(vm, avisos, navegar, voltar)
            Rota.AZURE -> PaginaAzure(vm, avisos, voltar)
            Rota.FILTROS -> PaginaFiltros(vm, avisos, voltar)
            Rota.APARENCIA -> PaginaAparencia(vm, avisos, voltar)
            Rota.ATALHOS -> PaginaAtalhos(avisos, voltar)
        }
    }
}
