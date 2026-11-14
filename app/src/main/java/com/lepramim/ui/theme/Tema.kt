package com.lepramim.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lepramim.data.CorDestaque

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

private class Destaque(
    val primaria: Color, val naPrimaria: Color,
    val conteiner: Color, val noConteiner: Color,
    val terciaria: Color, val naTerciaria: Color,
    val terciarioConteiner: Color, val noTerciarioConteiner: Color,
    val inversa: Color,
)

private fun ColorScheme.com(d: Destaque) = copy(
    primary = d.primaria, onPrimary = d.naPrimaria,
    primaryContainer = d.conteiner, onPrimaryContainer = d.noConteiner,
    tertiary = d.terciaria, onTertiary = d.naTerciaria,
    tertiaryContainer = d.terciarioConteiner, onTertiaryContainer = d.noTerciarioConteiner,
    inversePrimary = d.inversa,
)

private val Branco = Color.White

private val AzulClaro = BaseClara.com(Destaque(
    Color(0xFF4C7FD8), Branco, Color(0xFFD6E4FF), Color(0xFF0B2A5C),
    Color(0xFF0061A4), Branco, Color(0xFFCFE5FF), Color(0xFF001D36), Color(0xFF8AB4F8),
))
private val AzulEscuro = BaseEscura.com(Destaque(
    Color(0xFF3D7FEF), Branco, Color(0xFF1A5BC4), Color(0xFFE0EBFF),
    Color(0xFF9CCAFF), Color(0xFF003258), Color(0xFF1F5D96), Color(0xFFD5E8FF), Color(0xFF0B57D0),
))

private val TurquesaClaro = BaseClara.com(Destaque(
    Color(0xFF3A98B3), Branco, Color(0xFFCFEFF7), Color(0xFF062C38),
    Color(0xFF00687F), Branco, Color(0xFFBDEEFA), Color(0xFF001F28), Color(0xFF7FD0E6),
))
private val TurquesaEscuro = BaseEscura.com(Destaque(
    Color(0xFF2AA6C0), Branco, Color(0xFF0F6B80), Color(0xFFC4F1FF),
    Color(0xFF8EDCF0), Color(0xFF00343F), Color(0xFF0B5565), Color(0xFFC4F1FF), Color(0xFF00789A),
))

private val LaranjaClaro = BaseClara.com(Destaque(
    Color(0xFFC58A4F), Branco, Color(0xFFFBE2C6), Color(0xFF3A2005),
    Color(0xFF9A4A00), Branco, Color(0xFFFFDBC2), Color(0xFF311300), Color(0xFFFFA654),
))
private val LaranjaEscuro = BaseEscura.com(Destaque(
    Color(0xFFD08A45), Branco, Color(0xFF94601F), Color(0xFFFFE6CF),
    Color(0xFFFFC27A), Color(0xFF4A2800), Color(0xFF8A4F00), Color(0xFFFFE2C2), Color(0xFFB45309),
))

private val VermelhoClaro = BaseClara.com(Destaque(
    Color(0xFFC86A63), Branco, Color(0xFFFADAD6), Color(0xFF4A1511),
    Color(0xFF9C4336), Branco, Color(0xFFFFDAD4), Color(0xFF3B0906), Color(0xFFFFB4A8),
))
private val VermelhoEscuro = BaseEscura.com(Destaque(
    Color(0xFFD0645A), Branco, Color(0xFF9A3C34), Color(0xFFFFDAD4),
    Color(0xFFFFB4A8), Color(0xFF561E17), Color(0xFF8C3A2F), Color(0xFFFFDAD4), Color(0xFFC8372B),
))

private val VerdeClaro = BaseClara.com(Destaque(
    Color(0xFF6A9E4D), Branco, Color(0xFFDDF0CE), Color(0xFF14290A),
    Color(0xFF5E6B00), Branco, Color(0xFFE6F57A), Color(0xFF1B2100), Color(0xFF8FD36A),
))
private val VerdeEscuro = BaseEscura.com(Destaque(
    Color(0xFF6FAE4A), Branco, Color(0xFF3F7226), Color(0xFFE0FFCB),
    Color(0xFFD4E97A), Color(0xFF2A3800), Color(0xFF4A5F10), Color(0xFFEEFFB0), Color(0xFF4C8A2B),
))

private val CinzaClaro = BaseClara.com(Destaque(
    Color(0xFF5F6368), Branco, Color(0xFFDADCE0), Color(0xFF1B1C1E),
    Color(0xFF5F6368), Branco, Color(0xFFE8EAED), Color(0xFF1B1C1E), Color(0xFFDADCE0),
))
private val CinzaEscuro = BaseEscura.com(Destaque(
    Color(0xFFE3E5E8), Color(0xFF2D3033), Color(0xFF5F6368), Color(0xFFF1F3F4),
    Color(0xFFBDC1C6), Color(0xFF2D3033), Color(0xFF4A4E52), Color(0xFFE8EAED), Color(0xFF3C4043),
))

private fun esquemaDe(cor: CorDestaque, escuro: Boolean) = when (cor) {
    CorDestaque.AZUL -> if (escuro) AzulEscuro else AzulClaro
    CorDestaque.TURQUESA -> if (escuro) TurquesaEscuro else TurquesaClaro
    CorDestaque.LARANJA -> if (escuro) LaranjaEscuro else LaranjaClaro
    CorDestaque.VERMELHO -> if (escuro) VermelhoEscuro else VermelhoClaro
    CorDestaque.VERDE -> if (escuro) VerdeEscuro else VerdeClaro
    CorDestaque.CINZA -> if (escuro) CinzaEscuro else CinzaClaro
}

/** Cor da bolinha na escolha do destaque. */
fun amostraDe(cor: CorDestaque): Color = when (cor) {
    CorDestaque.AZUL -> Color(0xFF3D7FEF)
    CorDestaque.TURQUESA -> Color(0xFF2AA6C0)
    CorDestaque.LARANJA -> Color(0xFFD08A45)
    CorDestaque.VERMELHO -> Color(0xFFD0645A)
    CorDestaque.VERDE -> Color(0xFF6FAE4A)
    CorDestaque.CINZA -> Color(0xFFBDC1C6)
}

fun nomeDaCor(cor: CorDestaque) = when (cor) {
    CorDestaque.AZUL -> "Azul"
    CorDestaque.TURQUESA -> "Turquesa"
    CorDestaque.LARANJA -> "Laranja"
    CorDestaque.VERMELHO -> "Vermelho"
    CorDestaque.VERDE -> "Verde"
    CorDestaque.CINZA -> "Cinza"
}

private val base = Typography()

private val Tipografia = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
)

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
fun TemaLeitor(cor: CorDestaque, content: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { janela ->
            WindowCompat.getInsetsController(janela, view).apply {
                isAppearanceLightStatusBars = !escuro
                isAppearanceLightNavigationBars = !escuro
            }
        }
    }
    MaterialTheme(colorScheme = esquemaDe(cor, escuro), typography = Tipografia, shapes = Formas, content = content)
}
