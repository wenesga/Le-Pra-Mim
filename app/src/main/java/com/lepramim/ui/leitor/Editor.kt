package com.lepramim.ui.leitor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.launch
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
    palavra: IntRange?,
    destacar: Boolean,
    editavel: Boolean,
    tamanho: Int,
    fonte: androidx.compose.ui.text.font.FontFamily,
    aoEditar: (String) -> Unit,
    aoColar: () -> Unit,
    aoTocar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var valor by remember { mutableStateOf(TextFieldValue(texto)) }
    // Mudanças de fora (colar, limpar, serviço) entram no campo; o que foi digitado aqui já está nele.
    LaunchedEffect(texto) {
        val atual = Leitura.texto.value
        if (atual != valor.text) valor = TextFieldValue(atual, TextRange(valor.selection.end.coerceAtMost(atual.length)))
    }

    val corFundo = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val corTexto = androidx.compose.ui.graphics.Color.Unspecified
    val corPalavra = MaterialTheme.colorScheme.primary
    val corTextoPalavra = MaterialTheme.colorScheme.onPrimary
    val destaque = remember(trecho, palavra, destacar, corFundo, corTexto, corPalavra) {
        VisualTransformation { t ->
            // Com o destaque desligado a rolagem continua seguindo a leitura, só não pinta.
            val r = trecho
            val anotado = if (!destacar || r == null || r.first >= t.length) t else buildAnnotatedString {
                append(t)
                addStyle(SpanStyle(background = corFundo, color = corTexto), r.first, (r.last + 1).coerceAtMost(t.length))
                // Palavra atual: destaque forte por cima da frase (só se estiver dentro dela).
                if (palavra != null && palavra.first in r && palavra.first < t.length) {
                    addStyle(SpanStyle(background = corPalavra, color = corTextoPalavra), palavra.first, (palavra.last + 1).coerceAtMost(t.length))
                }
            }
            TransformedText(anotado, OffsetMapping.Identity)
        }
    }

    val rolagem = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val escopo = androidx.compose.runtime.rememberCoroutineScope()
    // Dois toques: a frase tocada sobe para o topo da tela, mesmo que já esteja visível.
    val subirFrase: (Int) -> Unit = { posicao ->
        val l = layout
        if (l != null) {
            val inicio = com.lepramim.leitura.LeituraServico.dividir(Leitura.texto.value)
                .lastOrNull { it.first <= posicao }?.first ?: posicao
            val topo = l.getLineTop(l.getLineForOffset(inicio.coerceAtMost(l.layoutInput.text.length - 1).coerceAtLeast(0))).toInt()
            escopo.launch { rolagem.animateScrollTo(topo) }
        }
    }
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
        fontSize = tamanho.sp, lineHeight = (tamanho * 1.5f).sp, fontFamily = fonte,
        color = MaterialTheme.colorScheme.onSurface,
    )
    // Só com a edição ligada o campo pega o foco (e o teclado abre); ao desligar, solta o foco e fecha o teclado.
    val pedirFoco = remember { androidx.compose.ui.focus.FocusRequester() }
    val foco = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(editavel) { if (editavel) runCatching { pedirFoco.requestFocus() } else foco.clearFocus() }

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
                // Fora da edição o campo fica desligado: sem seleção nem cursor. Para copiar, abre o lápis.
                enabled = editavel,
                readOnly = !editavel,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().heightIn(min = alturaTela).padding(Espaco.gg)
                    // Fora da edição, dois toques no texto leem dali na hora.
                    .pointerInput(editavel) {
                        if (editavel) return@pointerInput
                        // Dois toques seguidos no mesmo lugar leem dali; um só não faz nada (evita toque sem querer).
                        var tempoAnterior = 0L
                        var posAnterior = androidx.compose.ui.geometry.Offset.Zero
                        awaitEachGesture {
                            val baixo = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val cima = waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
                            val curto = cima.uptimeMillis - baixo.uptimeMillis < viewConfiguration.longPressTimeoutMillis
                            val parado = (cima.position - baixo.position).getDistance() < viewConfiguration.touchSlop
                            val l = layout ?: return@awaitEachGesture
                            // Toque abaixo da última linha não conta.
                            if (!curto || !parado || cima.position.y > l.size.height) { tempoAnterior = 0L; return@awaitEachGesture }
                            val segundo = tempoAnterior != 0L &&
                                cima.uptimeMillis - tempoAnterior < viewConfiguration.doubleTapTimeoutMillis &&
                                (cima.position - posAnterior).getDistance() < viewConfiguration.touchSlop * 4
                            if (segundo) {
                                tempoAnterior = 0L
                                val posicao = l.getOffsetForPosition(cima.position)
                                aoTocar(posicao)
                                subirFrase(posicao)
                            } else {
                                tempoAnterior = cima.uptimeMillis
                                posAnterior = cima.position
                            }
                        }
                    }
                    .focusRequester(pedirFoco),
                decorationBox = { campo ->
                    Box {
                        if (valor.text.isEmpty()) {
                            Column {
                                Text(if (editavel) "Digite ou cole o texto" else "Cole o texto", style = estilo, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        // Marcador de posição na lateral direita: mostra onde o trecho visível está no texto todo.
        var arrastando by remember { mutableStateOf(false) }
        val vivo by androidx.compose.animation.core.animateFloatAsState(
            if (rolagem.isScrollInProgress || arrastando) 0.8f else 0.35f, label = "marcador",
        )
        val cor = MaterialTheme.colorScheme.onSurfaceVariant
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val total = rolagem.maxValue
            val vista = rolagem.viewportSize
            if (total <= 0 || vista <= 0) return@Canvas
            val altura = (vista.toFloat() * vista / (vista + total)).coerceAtLeast(32.dp.toPx())
            val topo = rolagem.value.toFloat() / total * (vista - altura)
            val largura = 4.dp.toPx()
            drawRoundRect(
                color = cor.copy(alpha = vivo),
                topLeft = androidx.compose.ui.geometry.Offset(size.width - largura - 2.dp.toPx(), topo),
                size = androidx.compose.ui.geometry.Size(largura, altura),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(largura / 2),
            )
        }
        // Faixa na borda direita: arrastar por ela rola o texto (a barra acompanha o dedo).
        Box(
            Modifier.align(androidx.compose.ui.Alignment.CenterEnd).fillMaxHeight().width(28.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { arrastando = true },
                        onDragEnd = { arrastando = false },
                        onDragCancel = { arrastando = false },
                    ) { _, dy ->
                        val total = rolagem.maxValue
                        val vista = rolagem.viewportSize
                        if (total <= 0 || vista <= 0) return@detectVerticalDragGestures
                        val altura = (vista.toFloat() * vista / (vista + total)).coerceAtLeast(32.dp.toPx())
                        val livre = (vista - altura).coerceAtLeast(1f)
                        rolagem.dispatchRawDelta(dy * total / livre)
                    }
                },
        )
    }
}
