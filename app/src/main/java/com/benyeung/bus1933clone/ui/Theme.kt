package com.benyeung.bus1933clone.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KmbRed = Color(0xFFC62828)
private val DarkScheme = darkColorScheme(
    primary = KmbRed,
    secondary = Color(0xFFFFB300),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)
private val LightScheme = lightColorScheme(
    primary = KmbRed,
    secondary = Color(0xFFFF8F00),
)

@Composable
fun Bus1933Theme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}