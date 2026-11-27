package com.lepramim.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lepramim.data.CorDestaque
import com.lepramim.data.FundoEscuro
import com.lepramim.data.TemaApp

// Mesmo visual do Escala de Trabalho: base neutra no claro, azul-marinho no escuro, destaque à escolha.
private val BaseClara = EsquemaClaro.copy(
    background = Color(0xFFF1F3F4), onBackground = Color(0xFF1B1C1E),
    surface = Color(0xFFF1F3F4), onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE1E3E6), onSurfaceVariant = Color(0xFF44474A),
    outline = Color(0xFF75777B), outlineVariant = Color(0xFFC5C7CA),
    surfaceDim = Color(0xFFD9DADD), surfaceBright = Color(0xFFF8F9FA),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F8F9),
    surfaceContainer = Color(0xFFECEDEF), surfaceContainerHigh = Color(0xFFE6E7EA),
    surfaceContainerHighest = Color(0xFFE0E2E4),
    secondary = Color(0xFF5E6168), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E5E8), onSecondaryContainer = Color(0xFF1B1C1E),
)

private val BaseEscura = EsquemaEscuro.copy(
    background = Color(0xFF171A26), surface = Color(0xFF171A26), surfaceDim = Color(0xFF171A26),
    surfaceContainerLowest = Color(0xFF12141D), surfaceContainerLow = Color(0xFF252A39),
    surfaceContainer = Color(0xFF1F2330), surfaceContainerHigh = Color(0xFF2E3447),
    surfaceContainerHighest = Color(0xFF3A4155), surfaceBright = Color(0xFF444C62),
    surfaceVariant = Color(0xFF3A4155), outlineVariant = Color(0xFF3A4155),
    secondary = Color(0xFFC4C6CA), onSecondary = Color(0xFF2D3033),
    secondaryContainer = Color(0xFF3A4155), onSecondaryContainer = Color(0xFFF2F4F8),
    onBackground = Color(0xFFF2F4F8), onSurface = Color(0xFFF2F4F8), onSurfaceVariant = Color(0xFFC5CAD6),
    outline = Color(0xFF8E9195),
)

/** Fundo e cartões de cada variação do escuro (os mesmos do Ciclo Treino). */
private class Fundo(
    val fundo: Long, val baixissimo: Long, val baixo: Long, val medio: Long, val alto: Long, val altissimo: Long,
    val brilho: Long, val variante: Long, val naSecundaria: Long, val texto: Long, val textoVariante: Long,
)

/** Fundo bem escuro com cartões calculados a partir dele (grafite, verde e roxo profundos). */
private fun derivado(bg: Long): Fundo {
    val c = Color(bg)
    fun m(alvo: Color, x: Float) = lerp(c, alvo, x).toArgb().toLong() and 0xFFFFFFFFL
    return Fundo(
        bg, m(Color.Black, 0.3f), m(Color.White, 0.04f), m(Color.White, 0.07f), m(Color.White, 0.11f),
        m(Color.White, 0.15f), m(Color.White, 0.2f), m(Color.White, 0.15f),
        0xFFF2F4F8, 0xFFF2F4F8, 0xFFC5CAD6,
    )
}

private fun fundoDe(f: FundoEscuro) = when (f) {
    FundoEscuro.VINHO_PROFUNDO -> derivado(0xFF1A1113)
    FundoEscuro.PRETO -> Fundo(0xFF000000, 0xFF000000, 0xFF141516, 0xFF18191B, 0xFF212225, 0xFF2B2C30, 0xFF333438, 0xFF2B2C30, 0xFFE8EAED, 0xFFE3E3E5, 0xFFC4C6CA)
    FundoEscuro.GRAFITE -> derivado(0xFF121416)
    FundoEscuro.PETROLEO -> Fundo(0xFF22272B, 0xFF1C2125, 0xFF273238, 0xFF2C383E, 0xFF354249, 0xFF3F4D54, 0xFF465560, 0xFF3F4D54, 0xFFE6EEF1, 0xFFECF1F3, 0xFFB9C5CA)
    FundoEscuro.MARINHO -> Fundo(0xFF171A26, 0xFF12141D, 0xFF252A39, 0xFF1F2330, 0xFF2E3447, 0xFF3A4155, 0xFF444C62, 0xFF3A4155, 0xFFF2F4F8, 0xFFF2F4F8, 0xFFC5CAD6)
    FundoEscuro.VERDE_PROFUNDO -> derivado(0xFF0B1512)
    FundoEscuro.ROXO_PROFUNDO -> derivado(0xFF14101D)
}

/** Tons de uma cor no tema atual: botão, texto no botão, suave e texto no suave. A Automática vem do papel de parede. */
private class Tons(val botao: Color, val noBotao: Color, val suave: Color, val noSuave: Color)

private fun tonsDe(c: CorDestaque, escuro: Boolean, contexto: Context): Tons {
    if (c == CorDestaque.AUTOMATICA && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        fun t(id: Int) = Color(contexto.getColor(id))
        return if (escuro) Tons(t(android.R.color.system_accent1_200), t(android.R.color.system_accent1_900), t(android.R.color.system_accent1_800), t(android.R.color.system_accent1_100))
        else Tons(t(android.R.color.system_accent1_600), Color.White, t(android.R.color.system_accent1_100), t(android.R.color.system_accent1_900))
    }
    val m = if (escuro) c.escuro else c.claro
    return Tons(Color(m.botao), Color(m.noBotao), Color(m.suave), Color(m.noSuave))
}

private fun escuroDe(f: FundoEscuro, t: Tons): ColorScheme {
    val d = fundoDe(f)
    val fundo = Color(d.fundo)
    return EsquemaEscuro.copy(
        background = fundo, surface = fundo, surfaceDim = fundo,
        surfaceContainerLowest = Color(d.baixissimo), surfaceContainerLow = Color(d.baixo),
        surfaceContainer = Color(d.medio), surfaceContainerHigh = Color(d.alto),
        surfaceContainerHighest = Color(d.altissimo), surfaceBright = Color(d.brilho),
        surfaceVariant = Color(d.variante), outlineVariant = Color(d.variante),
        onBackground = Color(d.texto), onSurface = Color(d.texto), onSurfaceVariant = Color(d.textoVariante),
        outline = Color(0xFF8E9195),
    ).comTons(t)
}

/** Botão no `primary` (com o texto próprio), suave no `primaryContainer`; seleções usam o botão. */
private fun ColorScheme.comTons(t: Tons) = copy(
    primary = t.botao, onPrimary = t.noBotao,
    primaryContainer = t.suave, onPrimaryContainer = t.noSuave,
    secondary = t.botao, onSecondary = t.noBotao,
    secondaryContainer = t.botao, onSecondaryContainer = t.noBotao,
    tertiary = t.botao, onTertiary = t.noBotao,
    tertiaryContainer = t.suave, onTertiaryContainer = t.noSuave,
    inversePrimary = t.botao,
)

private fun claroDe(t: Tons) = BaseClara.comTons(t)

/** Cor da bolinha na escolha do destaque: o tom do botão no modo atual (Automática: a do papel de parede). */
fun amostraDe(cor: CorDestaque, escuro: Boolean, contexto: Context): Color = tonsDe(cor, escuro, contexto).botao

fun nomeDaCor(cor: CorDestaque) = cor.nome

/** Cor da bolinha na escolha do fundo escuro. */
fun amostraDoFundo(f: FundoEscuro): Color = Color(fundoDe(f).let { if (f == FundoEscuro.PRETO) it.fundo else it.baixo })

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Espaçamentos em múltiplos de 4 dp. */
object Espaco {
    val p = 4.dp
    val m = 8.dp
    val g = 12.dp
    val gg = 16.dp
    val xg = 24.dp
}

@Composable
fun TemaLeitor(cor: CorDestaque, tema: TemaApp, fundo: FundoEscuro, content: @Composable () -> Unit) {
    val escuro = when (tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.CLARO -> false
        TemaApp.ESCURO -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { janela ->
            WindowCompat.getInsetsController(janela, view).apply {
                isAppearanceLightStatusBars = !escuro
                isAppearanceLightNavigationBars = !escuro
            }
        }
    }
    val tons = tonsDe(cor, escuro, LocalContext.current)
    val esquema = if (escuro) escuroDe(fundo, tons) else claroDe(tons)
    MaterialTheme(colorScheme = esquema, shapes = Formas, content = content)
}

/** Botão cheio: tom do botão com o texto próprio. */
@androidx.compose.runtime.Composable
fun corBotao() = androidx.compose.material3.ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
)

/** Controle deslizante: parte feita no tom do botão, parte restante no suave. */
@androidx.compose.runtime.Composable
fun corSlider() = androidx.compose.material3.SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
    inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
)

/** Trilho das barras de uso: o suave da cor. */
@androidx.compose.runtime.Composable
fun trilhoBarra() = MaterialTheme.colorScheme.primaryContainer
