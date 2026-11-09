package com.leitorvoz.ui.theme

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
    primary = Color(0xFFB9C8FF), onPrimary = Color(0xFF0F1D4A),
    primaryContainer = Color(0xFF2A3F86), onPrimaryContainer = Color(0xFFDBE2FF),
    secondaryContainer = Color(0xFF2A3F86), onSecondaryContainer = Color(0xFFDBE2FF),
    background = Color(0xFF0F1115), surface = Color(0xFF0F1115),
    surfaceContainerLow = Color(0xFF15181D), surfaceContainer = Color(0xFF181B21),
    surfaceContainerHigh = Color(0xFF1F232A), surfaceContainerHighest = Color(0xFF292D35),
)

private val Claro = lightColorScheme(
    primary = Color(0xFF2F5BD3), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE2FF), onPrimaryContainer = Color(0xFF0C2A7A),
    secondaryContainer = Color(0xFFDBE2FF), onSecondaryContainer = Color(0xFF0C2A7A),
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
