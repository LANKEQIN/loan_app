package com.superloan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.superloan.app.data.AppSettings
import com.superloan.app.data.ThemeMode

/**
 * 应用主题。
 *
 * 深浅色由设置里的「主题模式」决定（跟随系统 / 浅色 / 深色），
 * 配色由设置里的「配色方案」决定（动态取色或内置色板）。
 */
@Composable
fun SuperLoanTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val palette = PaletteId.fromId(settings.paletteId)
    val colorScheme = remember(palette, darkTheme, context) {
        resolveColorScheme(context, palette, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
