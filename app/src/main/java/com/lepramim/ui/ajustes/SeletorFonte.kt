package com.lepramim.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lepramim.data.FonteTexto
import com.lepramim.ui.theme.familia

private const val AMOSTRA = "Lê pra mim. O texto aparece assim na tela."

/** Linha "Fonte" com a escolhida, prévia ao vivo e, ao tocar, uma folha com todas as fontes escritas em si mesmas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorFonte(atual: FonteTexto, tamanho: Int, aoEscolher: (FonteTexto) -> Unit) {
    var aberto by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Fonte") },
        supportingContent = { Text(atual.nome, fontFamily = atual.familia(), style = MaterialTheme.typography.bodyLarge) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { aberto = true },
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            AMOSTRA,
            Modifier.padding(16.dp),
            fontFamily = atual.familia(),
            fontSize = tamanho.sp,
            lineHeight = (tamanho * 1.6f).sp,
        )
    }

    if (aberto) ModalBottomSheet(onDismissRequest = { aberto = false }) {
        LazyColumn(Modifier.navigationBarsPadding()) {
            for ((titulo, lista) in listOf("Dislexia" to FonteTexto.entries.filter { it.dislexia }, "Legíveis" to FonteTexto.entries.filter { !it.dislexia })) {
                item {
                    Text(
                        titulo,
                        Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(lista) { f ->
                    val escolhida = f == atual
                    Column(
                        Modifier.fillMaxWidth()
                            .then(if (escolhida) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) else Modifier)
                            .clickable { aoEscolher(f); aberto = false }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Text(
                            f.nome,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (escolhida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(AMOSTRA, fontFamily = f.familia(), fontSize = 19.sp, lineHeight = 28.sp)
                    }
                }
            }
        }
    }
}
