package com.lepramim.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val Escuro = darkColorScheme(
    primary = Color(0xFF5EE0A8), onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1F5A40), onPrimaryContainer = Color(0xFFCDF5E1),
    secondaryContainer = Color(0xFF1F5A40), onSecondaryContainer = Color(0xFFCDF5E1),
    background = Color(0xFF0F1115), surface = Color(0xFF0F1115),
    surfaceContainerLow = Color(0xFF15181D), surfaceContainer = Color(0xFF181B21),
    surfaceContainerHigh = Color(0xFF1F232A), surfaceContainerHighest = Color(0xFF292D35),
)

private val Claro = lightColorScheme(
    primary = Color(0xFF1B7F57), onPrimary = Color.White,
    primaryContainer = Color(0xFFBDF2D8), onPrimaryContainer = Color(0xFF05392A),
    secondaryContainer = Color(0xFFBDF2D8), onSecondaryContainer = Color(0xFF05392A),
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
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
fun TemaLeitor(content: @Composable () -> Unit) {
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
    MaterialTheme(colorScheme = if (escuro) Escuro else Claro, shapes = Formas, content = content)
}
