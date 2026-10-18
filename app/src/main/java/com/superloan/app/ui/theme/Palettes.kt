package com.superloan.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * 配色方案。
 *
 * - DYNAMIC：跟随系统壁纸取色（Material You），需要 Android 12 及以上
 * - 其余为内置色板，各由一个种子色生成整套浅色 / 深色配色
 */
enum class PaletteId(val id: String, val title: String, val seed: Color) {
    DYNAMIC("dynamic", "动态取色", Color(0xFF6B7280)),
    BRAND_GREEN("brand_green", "品牌绿", Color(0xFF1F6F4A)),
    TECH_BLUE("tech_blue", "科技蓝", Color(0xFF2563EB)),
    DUSK_PURPLE("dusk_purple", "暮紫", Color(0xFF7C3AED)),
    WARM_ORANGE("warm_orange", "暖橙", Color(0xFFEA580C)),
    ROSE_PINK("rose_pink", "玫红", Color(0xFFDB2777)),
    TEAL("teal", "青碧", Color(0xFF0D9488)),
    GRAPHITE("graphite", "石墨", Color(0xFF475569));

    /** 是否为动态取色 */
    val isDynamic: Boolean get() = this == DYNAMIC

    /** 色板圆点展示色 */
    val swatch: Color get() = seed

    companion object {
        fun fromId(id: String?): PaletteId = entries.firstOrNull { it.id == id } ?: BRAND_GREEN
    }
}

/** 动态取色是否可用（Android 12 / API 31 起） */
val isDynamicColorAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * 解析出最终使用的配色。
 * 动态取色不可用时自动回退到品牌绿。
 */
fun resolveColorScheme(context: Context, palette: PaletteId, dark: Boolean): ColorScheme {
    if (palette.isDynamic && isDynamicColorAvailable) {
        return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val seed = if (palette.isDynamic) PaletteId.BRAND_GREEN.seed else palette.seed
    return buildColorScheme(seed, dark)
}

/**
 * 由一个种子色生成整套 Material 3 配色。
 *
 * 做法：取种子色的色相作为主色相，再围绕它旋转得到次级 / 三级色相，
 * 各角色按固定的饱和度与明度取值，从而保证浅色 / 深色两种模式下的可读性与统一感。
 */
fun buildColorScheme(seed: Color, dark: Boolean): ColorScheme {
    val baseHue = hueOf(seed)
    val secondaryHue = rotate(baseHue, 40f)
    val tertiaryHue = rotate(baseHue, 320f)

    return if (dark) {
        darkColorScheme(
            primary = Color.hsl(baseHue, 0.40f, 0.76f),
            onPrimary = Color.hsl(baseHue, 0.60f, 0.16f),
            primaryContainer = Color.hsl(baseHue, 0.42f, 0.30f),
            onPrimaryContainer = Color.hsl(baseHue, 0.50f, 0.90f),
            secondary = Color.hsl(secondaryHue, 0.18f, 0.76f),
            onSecondary = Color.hsl(secondaryHue, 0.25f, 0.18f),
            secondaryContainer = Color.hsl(secondaryHue, 0.20f, 0.30f),
            onSecondaryContainer = Color.hsl(secondaryHue, 0.22f, 0.90f),
            tertiary = Color.hsl(tertiaryHue, 0.25f, 0.78f),
            onTertiary = Color.hsl(tertiaryHue, 0.30f, 0.18f),
            tertiaryContainer = Color.hsl(tertiaryHue, 0.26f, 0.30f),
            onTertiaryContainer = Color.hsl(tertiaryHue, 0.30f, 0.90f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color.hsl(baseHue, 0.10f, 0.07f),
            onBackground = Color.hsl(baseHue, 0.08f, 0.92f),
            surface = Color.hsl(baseHue, 0.10f, 0.07f),
            onSurface = Color.hsl(baseHue, 0.08f, 0.92f),
            surfaceVariant = Color.hsl(baseHue, 0.12f, 0.17f),
            onSurfaceVariant = Color.hsl(baseHue, 0.10f, 0.80f),
            outline = Color.hsl(baseHue, 0.10f, 0.55f),
            outlineVariant = Color.hsl(baseHue, 0.10f, 0.28f)
        )
    } else {
        lightColorScheme(
            primary = Color.hsl(baseHue, 0.55f, 0.32f),
            onPrimary = Color.White,
            primaryContainer = Color.hsl(baseHue, 0.55f, 0.90f),
            onPrimaryContainer = Color.hsl(baseHue, 0.60f, 0.16f),
            secondary = Color.hsl(secondaryHue, 0.20f, 0.38f),
            onSecondary = Color.White,
            secondaryContainer = Color.hsl(secondaryHue, 0.25f, 0.91f),
            onSecondaryContainer = Color.hsl(secondaryHue, 0.25f, 0.18f),
            tertiary = Color.hsl(tertiaryHue, 0.28f, 0.38f),
            onTertiary = Color.White,
            tertiaryContainer = Color.hsl(tertiaryHue, 0.30f, 0.91f),
            onTertiaryContainer = Color.hsl(tertiaryHue, 0.30f, 0.18f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color.hsl(baseHue, 0.22f, 0.985f),
            onBackground = Color.hsl(baseHue, 0.12f, 0.12f),
            surface = Color.hsl(baseHue, 0.22f, 0.985f),
            onSurface = Color.hsl(baseHue, 0.12f, 0.12f),
            surfaceVariant = Color.hsl(baseHue, 0.18f, 0.92f),
            onSurfaceVariant = Color.hsl(baseHue, 0.12f, 0.32f),
            outline = Color.hsl(baseHue, 0.10f, 0.50f),
            outlineVariant = Color.hsl(baseHue, 0.14f, 0.80f)
        )
    }
}

/** 取色的色相（0..360） */
private fun hueOf(color: Color): Float {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    return hsl[0]
}

/** 色相旋转，结果保证落在 [0, 360) */
private fun rotate(hue: Float, delta: Float): Float {
    val value = (hue + delta) % 360f
    return if (value < 0f) value + 360f else value
}
