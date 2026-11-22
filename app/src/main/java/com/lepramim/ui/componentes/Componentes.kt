package com.lepramim.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.lepramim.ui.theme.Espaco

/** Avisos na parte de baixo: caixa própria, do tamanho exato do texto, centralizada. */
@Composable
fun HostAvisos(avisos: SnackbarHostState) {
    SnackbarHost(avisos) { dados ->
        val acao = dados.visuals.actionLabel
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier.padding(start = 20.dp, end = if (acao == null) 20.dp else 8.dp, top = if (acao == null) 12.dp else 4.dp, bottom = if (acao == null) 12.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dados.visuals.message, style = MaterialTheme.typography.bodyMedium)
                    if (acao != null) androidx.compose.material3.TextButton(onClick = { dados.performAction() }) {
                        Text(acao, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Moldura das páginas de Ajustes: barra com seta de voltar e conteúdo rolável. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pagina(
    titulo: String,
    aoVoltar: () -> Unit,
    avisos: SnackbarHostState,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
        snackbarHost = { HostAvisos(avisos) },
    ) { margens ->
        Column(
            Modifier.fillMaxSize().padding(margens).verticalScroll(rememberScrollState()).padding(Espaco.gg),
            verticalArrangement = Arrangement.spacedBy(Espaco.xg),
            content = conteudo,
        )
    }
}

/** Grupo de ajustes com título na cor de destaque. */
@Composable
fun Secao(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Espaco.g)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        conteudo()
    }
}

@Composable
fun LinhaSwitch(titulo: String, marcado: Boolean, aoMudar: (Boolean) -> Unit, resumo: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (resumo != null) Text(resumo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = marcado, onCheckedChange = aoMudar)
    }
}

/** Lista suspensa de escolha única. `opcoes` são pares (id, nome); a escolhida fica em destaque. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Seletor(
    rotulo: String, valor: String, opcoes: List<Pair<String, String>>, escolher: (String) -> Unit,
    favoritas: List<String>? = null, alternarFavorita: ((String) -> Unit)? = null,
) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
        OutlinedTextField(
            value = valor, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = aberto, onDismissRequest = { aberto = false },
            // Com estrelas, a lista fica estreita: nome e estrela perto um do outro.
            modifier = if (favoritas != null) Modifier.width(200.dp) else Modifier,
        ) {
            opcoes.forEach { (id, nome) ->
                val atual = nome == valor
                DropdownMenuItem(
                    text = {
                        Text(
                            nome,
                            color = if (atual) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            fontWeight = if (atual) FontWeight.Bold else null,
                        )
                    },
                    trailingIcon = if (favoritas != null && alternarFavorita != null) {
                        {
                            val fav = id in favoritas
                            IconButton(onClick = { alternarFavorita(id) }) {
                                Icon(
                                    if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorita",
                                    tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else null,
                    onClick = { escolher(id); aberto = false },
                )
            }
        }
    }
}
