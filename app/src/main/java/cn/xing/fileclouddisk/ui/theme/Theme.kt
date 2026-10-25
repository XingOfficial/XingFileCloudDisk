package cn.xing.fileclouddisk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF2F6BFF)
private val BlueDark = Color(0xFF0D47A1)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = BlueDark
)

private val DarkColors = darkColorScheme(
    primary = Blue,
    secondary = BlueDark
)

@Composable
fun CloudDiskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
