package com.lepramim.ui.leitor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lepramim.leitura.Leitura
import com.lepramim.ui.theme.Espaco

/**
 * O texto do app: um campo de texto de tela cheia. Dá para digitar, colar e selecionar,
 * e a frase lida no momento fica destacada e acompanhada pela rolagem.
 */
@Composable
fun Editor(
    texto: String,
    trecho: IntRange?,
    destacar: Boolean,
    tamanho: Int,
    fonte: androidx.compose.ui.text.font.FontFamily,
    aoEditar: (String) -> Unit,
    aoColar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var valor by remember { mutableStateOf(TextFieldValue(texto)) }
    // Mudanças de fora (colar, limpar, serviço) entram no campo; o que foi digitado aqui já está nele.
    LaunchedEffect(texto) {
        val atual = Leitura.texto.value
        if (atual != valor.text) valor = TextFieldValue(atual, TextRange(valor.selection.end.coerceAtMost(atual.length)))
    }

    val corFundo = MaterialTheme.colorScheme.primaryContainer
    val corTexto = MaterialTheme.colorScheme.onPrimaryContainer
    val destaque = remember(trecho, destacar, corFundo, corTexto) {
        VisualTransformation { t ->
            // Com o destaque desligado a rolagem continua seguindo a leitura, só não pinta.
            val r = trecho
            val anotado = if (!destacar || r == null || r.first >= t.length) t else buildAnnotatedString {
                append(t)
                addStyle(SpanStyle(background = corFundo, color = corTexto), r.first, (r.last + 1).coerceAtMost(t.length))
            }
            TransformedText(anotado, OffsetMapping.Identity)
        }
    }

    val rolagem = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // Mantém a frase lida visível, um pouco abaixo do topo.
    LaunchedEffect(trecho?.first, layout) {
        val l = layout ?: return@LaunchedEffect
        val inicio = trecho?.first ?: return@LaunchedEffect
        if (inicio >= l.layoutInput.text.length) return@LaunchedEffect
        val linha = l.getLineForOffset(inicio)
        val topo = l.getLineTop(linha).toInt()
        val base = l.getLineBottom(linha).toInt()
        val altura = rolagem.viewportSize
        if (topo < rolagem.value || base > rolagem.value + altura) {
            rolagem.animateScrollTo((topo - altura / 4).coerceAtLeast(0))
        }
    }

    val estilo = MaterialTheme.typography.bodyLarge.copy(
        fontSize = tamanho.sp, lineHeight = (tamanho * 1.6f).sp, fontFamily = fonte,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BoxWithConstraints(modifier.fillMaxSize()) {
        val alturaTela = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rolagem)) {
            BasicTextField(
                value = valor,
                onValueChange = { valor = it; aoEditar(it.text) },
                textStyle = estilo,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = destaque,
                onTextLayout = { layout = it },
                // O campo ocupa a tela toda: tocar em qualquer ponto vazio leva o cursor ao fim.
                modifier = Modifier.fillMaxWidth().heightIn(min = alturaTela).padding(Espaco.gg),
                decorationBox = { campo ->
                    Box {
                        if (valor.text.isEmpty()) {
                            Column {
                                Text("Cole ou digite o texto", style = estilo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.size(Espaco.xg))
                                FilledTonalButton(onClick = aoColar) {
                                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(Espaco.m))
                                    Text("Colar")
                                }
                            }
                        }
                        campo()
                    }
                },
            )
        }
    }
}
