package com.xboyrex.untrack

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.abs

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val AppRoundedHeavy = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-rounded"), weight = FontWeight.ExtraBold),
    Font(DeviceFontFamilyName("sans-serif-rounded"), weight = FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.ExtraBold),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Bold),
)

val AppRoundedBody = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-rounded"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif-rounded"), weight = FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif"), weight = FontWeight.Normal),
)

val RoundedHeavy: FontFamily
    get() = if (ThemeSettings.useSystemFont) FontFamily.Default else AppRoundedHeavy

fun untrackTypography(): Typography {
    val base = Typography()
    if (ThemeSettings.useSystemFont) return base
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = AppRoundedHeavy),
        displayMedium = base.displayMedium.copy(fontFamily = AppRoundedHeavy),
        displaySmall = base.displaySmall.copy(fontFamily = AppRoundedHeavy),
        headlineLarge = base.headlineLarge.copy(fontFamily = AppRoundedHeavy),
        headlineMedium = base.headlineMedium.copy(fontFamily = AppRoundedHeavy),
        headlineSmall = base.headlineSmall.copy(fontFamily = AppRoundedHeavy),
        titleLarge = base.titleLarge.copy(fontFamily = AppRoundedHeavy),
        titleMedium = base.titleMedium.copy(fontFamily = AppRoundedHeavy),
        titleSmall = base.titleSmall.copy(fontFamily = AppRoundedHeavy),
        bodyLarge = base.bodyLarge.copy(fontFamily = AppRoundedBody),
        bodyMedium = base.bodyMedium.copy(fontFamily = AppRoundedBody),
        bodySmall = base.bodySmall.copy(fontFamily = AppRoundedBody),
        labelLarge = base.labelLarge.copy(fontFamily = AppRoundedHeavy),
        labelMedium = base.labelMedium.copy(fontFamily = AppRoundedBody),
        labelSmall = base.labelSmall.copy(fontFamily = AppRoundedBody),
    )
}

private fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    var h2 = hex.removePrefix("#")
    if (h2.length == 3) h2 = h2.map { "$it$it" }.joinToString("")
    val r = h2.substring(0, 2).toInt(16) / 255f
    val g = h2.substring(2, 4).toInt(16) / 255f
    val b = h2.substring(4, 6).toInt(16) / 255f
    val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val l = (mx + mn) / 2f
    var h = 0f; var s = 0f; val d = mx - mn
    if (d != 0f) {
        s = d / (1 - abs(2 * l - 1))
        h = when (mx) {
            r -> (((g - b) / d) % 6)
            g -> ((b - r) / d + 2)
            else -> ((r - g) / d + 4)
        } * 60f
        if (h < 0) h += 360f
    }
    return Triple(h, s * 100f, l * 100f)
}

private fun hsl(h: Float, s: Float, l: Float): Color {
    val sN = s.coerceIn(0f, 100f) / 100f
    val lN = l.coerceIn(0f, 100f) / 100f
    val c = (1 - abs(2 * lN - 1)) * sN
    val x = c * (1 - abs(((h / 60f) % 2) - 1))
    val m = lN - c / 2
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}

data class ThemeChoice(val name: String, val hex: String, val isNew: Boolean = false)

const val SEED_UNTRACK_EMERALD = "#38C81E"
const val SEED_CREAM_ROSE = "#833761"
const val SEED_AURORA = "#6C2BD9"
const val SEED_LILAC_AIR = "#7454CE"
const val SEED_JAI = "#D6005C"
const val SEED_PUMPKIN = "#B0714A"
const val SEED_MOONSTONE = "#8A8F98"

val UNTRACK_THEMES = listOf(
    ThemeChoice("Untrack Green", SEED_UNTRACK_EMERALD, isNew = true),
    ThemeChoice("Cream Rose", SEED_CREAM_ROSE, isNew = true),
    ThemeChoice("Aurora", SEED_AURORA, isNew = true),
    ThemeChoice("Lilac Air", SEED_LILAC_AIR, isNew = true),
    ThemeChoice("Jai", SEED_JAI, isNew = true),
    ThemeChoice("Pumpkin", SEED_PUMPKIN, isNew = true),
    ThemeChoice("Moonstone", SEED_MOONSTONE, isNew = true),
    ThemeChoice("Iris", "#6750A4"),
    ThemeChoice("Harbor", "#3B5BDB"),
)

// Neutral carbon surfaces (like YTXTube) with crisp Untrack Green accent
private fun untrackEmeraldScheme(dark: Boolean): ColorScheme = if (!dark) lightColorScheme(
    primary = Color(0xFF289E12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF8D5),
    onPrimaryContainer = Color(0xFF0A3803),
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E4E4),
    onSecondaryContainer = Color(0xFF121212),
    tertiary = Color(0xFF289E12),
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF121212),
    background = Color.White,
    onBackground = Color(0xFF0F0F0F),
    surface = Color.White,
    onSurface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    outline = Color(0xFF8C8C8C),
    outlineVariant = Color(0xFFE2E2E2),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410E0B)
) else darkColorScheme(
    primary = Color(0xFF42C820),
    onPrimary = Color(0xFF061F02),
    primaryContainer = Color(0xFF183A12),
    onPrimaryContainer = Color(0xFFC8F7BC),
    secondary = Color(0xFFAAAAAA),
    onSecondary = Color(0xFF0F0F0F),
    secondaryContainer = Color(0xFF383838),
    onSecondaryContainer = Color(0xFFF1F1F1),
    tertiary = Color(0xFF5CE038),
    tertiaryContainer = Color(0xFF262626),
    onTertiaryContainer = Color(0xFFF1F1F1),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF0F0F0F),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFAAAAAA),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF343434),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

fun seedScheme(seedHex: String, dark: Boolean): ColorScheme {
    if (seedHex.equals(SEED_UNTRACK_EMERALD, ignoreCase = true) || seedHex.equals("#1DB954", ignoreCase = true)) {
        return untrackEmeraldScheme(dark)
    }
    val (h, s, _) = hexToHsl(seedHex)
    val isMoonstone = seedHex.equals(SEED_MOONSTONE, ignoreCase = true)
    val isPumpkin = seedHex.equals(SEED_PUMPKIN, ignoreCase = true)
    val accent = when {
        isMoonstone -> 7f
        isPumpkin -> 37f
        else -> maxOf(s, 48f)
    }
    val surf = when {
        isMoonstone -> 3f
        isPumpkin -> 24f
        else -> if (dark) 12f else 14f
    }
    return if (!dark) lightColorScheme(
        primary = hsl(h, accent, 42f), onPrimary = Color.White,
        primaryContainer = hsl(h, minOf(accent, 68f), 90f), onPrimaryContainer = hsl(h, accent, 16f),
        secondary = hsl(h, 26f, 40f), onSecondary = Color.White,
        secondaryContainer = hsl(h, 22f, 88f), onSecondaryContainer = hsl(h, 26f, 20f),
        tertiary = hsl((h + 40f) % 360f, 40f, 45f),
        background = hsl(h, surf, 98f), onBackground = hsl(h, 10f, 12f),
        surface = hsl(h, surf, 98f), onSurface = hsl(h, 10f, 12f),
        surfaceVariant = hsl(h, surf, 90f), onSurfaceVariant = hsl(h, minOf(12f, surf), 36f),
        surfaceContainerLowest = hsl(h, surf, 100f),
        surfaceContainerLow = hsl(h, surf, 96f),
        surfaceContainer = hsl(h, surf, 95f),
        surfaceContainerHigh = hsl(h, surf, 93f),
        surfaceContainerHighest = hsl(h, surf, 90f),
        outline = hsl(h, minOf(10f, surf), 54f), outlineVariant = hsl(h, minOf(10f, surf), 84f),
        error = Color(0xFFB3261E), onError = Color.White, errorContainer = Color(0xFFFFDAD5), onErrorContainer = Color(0xFF410E0B)
    ) else darkColorScheme(
        primary = hsl(h, minOf(accent, 70f), 78f), onPrimary = hsl(h, accent, 16f),
        primaryContainer = hsl(h, minOf(accent, 58f), 30f), onPrimaryContainer = hsl(h, 60f, 90f),
        secondary = hsl(h, 18f, 80f), onSecondary = hsl(h, 20f, 20f),
        secondaryContainer = hsl(h, 16f, 30f), onSecondaryContainer = hsl(h, 18f, 88f),
        tertiary = hsl((h + 40f) % 360f, 35f, 80f),
        background = hsl(h, surf, 7f), onBackground = hsl(h, 8f, 91f),
        surface = hsl(h, surf, 7f), onSurface = hsl(h, 8f, 91f),
        surfaceVariant = hsl(h, surf, 28f), onSurfaceVariant = hsl(h, minOf(10f, surf), 80f),
        surfaceContainerLowest = hsl(h, surf, 4f),
        surfaceContainerLow = hsl(h, surf, 10f),
        surfaceContainer = hsl(h, surf, 12f),
        surfaceContainerHigh = hsl(h, surf, 17f),
        surfaceContainerHighest = hsl(h, surf, 22f),
        outline = hsl(h, minOf(8f, surf), 60f), outlineVariant = hsl(h, minOf(8f, surf), 32f),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
    )
}

object ThemeSettings {
    private var prefs: SharedPreferences? = null
    val wallpaperAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var seed by mutableStateOf<String?>(SEED_UNTRACK_EMERALD)
        private set

    var mode by mutableStateOf("dark")
        private set
    var useSystemFont by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences("appearance", Context.MODE_PRIVATE).also { prefs = it }
        seed = if (p.contains("seed")) p.getString("seed", null) else SEED_UNTRACK_EMERALD
        mode = p.getString("mode", "dark") ?: "dark"
        useSystemFont = p.getBoolean("use_system_font", false)
    }

    fun choose(seedHex: String?) {
        seed = seedHex
        prefs?.edit()?.putString("seed", seedHex)?.apply()
    }

    fun chooseMode(value: String) {
        mode = value
        prefs?.edit()?.putString("mode", value)?.apply()
    }

    fun updateUseSystemFont(enabled: Boolean) {
        useSystemFont = enabled
        prefs?.edit()?.putBoolean("use_system_font", enabled)?.apply()
    }
}

fun schemeFor(context: Context, seedHex: String?, dark: Boolean): ColorScheme =
    if (seedHex == null && ThemeSettings.wallpaperAvailable) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        seedScheme(seedHex ?: SEED_UNTRACK_EMERALD, dark)
    }

@Composable
fun isDarkTheme(): Boolean = when (ThemeSettings.mode) {
    "dark", "amoled" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UntrackTheme(content: @Composable () -> Unit) {
    val dark = isDarkTheme()
    val isAmoled = ThemeSettings.mode == "amoled"
    val context = LocalContext.current
    val baseScheme = schemeFor(context, ThemeSettings.seed, dark)
    val scheme = if (isAmoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF141414),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF060606),
            surfaceContainer = Color(0xFF0C0C0C),
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1E1E1E),
            onBackground = Color(0xFFF5F5F5),
            onSurface = Color(0xFFF5F5F5),
            outlineVariant = Color(0xFF1F1F1F),
        )
    } else baseScheme

    SystemBars(dark, scheme.surface)
    MaterialTheme(colorScheme = scheme, shapes = ExpressiveShapes, typography = untrackTypography()) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null,
            LocalContentColor provides scheme.onSurface,
            content = content,
        )
    }
}

@Composable
private fun SystemBars(dark: Boolean, windowBg: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context as? ComponentActivity
    SideEffect {
        val window = activity?.window ?: (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        if (activity != null) {
            val barStyle = if (dark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            runCatching {
                activity.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    DisposableEffect(windowBg) {
        (view.context as? Activity)?.window?.setBackgroundDrawable(ColorDrawable(windowBg.toArgb()))
        onDispose { }
    }
}
