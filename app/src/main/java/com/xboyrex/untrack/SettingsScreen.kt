package com.xboyrex.untrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class SettingsFloatingSection {
    APPEARANCE,
    GENERAL_MODS,
    LANGUAGE,
    STORAGE_USAGE,
}

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onCheckUpdate: () -> Unit = {},
    onOpenChangelog: () -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeFloatingSection by remember { mutableStateOf<SettingsFloatingSection?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateFeedback by remember { mutableStateOf<String?>(null) }

    val isOverlayOpen = activeFloatingSection != null || checkingUpdate || updateFeedback != null
    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen && Build.VERSION.SDK_INT >= 31) 16.dp else 0.dp,
        animationSpec = tween(220),
        label = "settingsBlur",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface)
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
        ) {
            ScreenHeader(
                title = I18n.t("settings", "Settings"),
                brandMark = false
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
            ) {
                Spacer(Modifier.height(2.dp))

                // Expandable Profile & Branding Card
                SettingsProfileCard(context = context)

                SettingsSectionHeader(I18n.t("customization", "Customization"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = I18n.t("appearance_theme", "Appearance & Theme"),
                        subtitle = I18n.t("appearance_sub", "Theme mode, color palette and Liquid Glass style"),
                        onClick = { activeFloatingSection = SettingsFloatingSection.APPEARANCE },
                    )
                }

                SettingsSectionHeader("Engine & Interface")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Tune,
                        title = I18n.t("general_modification", "General Modification"),
                        subtitle = "URL cleaners, short-link resolver, Browser Shield & Private Link Guard",
                        onClick = { activeFloatingSection = SettingsFloatingSection.GENERAL_MODS },
                    )
                }

                SettingsSectionHeader(I18n.t("language_section", "Language"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Translate,
                        title = I18n.t("language", "Language"),
                        subtitle = LanguageSettings.currentDisplay(),
                        onClick = { activeFloatingSection = SettingsFloatingSection.LANGUAGE },
                    )
                }

                SettingsSectionHeader(I18n.t("data_storage", "Data & Storage"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.DataUsage,
                        title = I18n.t("storage_usage", "Storage Usage"),
                        subtitle = "See phone storage, export/restore JSON backups & clear Untrack cache",
                        onClick = { activeFloatingSection = SettingsFloatingSection.STORAGE_USAGE },
                    )
                }

                SettingsSectionHeader(I18n.t("about", "About"))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = I18n.t("version", "Version"),
                        subtitle = "Untrack ${BuildConfig.VERSION_NAME}",
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF132818))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2ECC71))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = I18n.t("up_to_date", "Up to date"),
                                        color = Color(0xFF2ECC71),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    )
                    SettingsRow(
                        icon = Icons.Rounded.FileDownload,
                        title = I18n.t("check_updates", "Check for updates"),
                        subtitle = I18n.t("check_updates_sub", "New GitHub releases install from inside the app"),
                        onClick = {
                            if (checkingUpdate) return@SettingsRow
                            checkingUpdate = true
                            updateFeedback = null
                            scope.launch {
                                val start = System.currentTimeMillis()
                                onCheckUpdate()
                                val elapsed = System.currentTimeMillis() - start
                                if (elapsed < 1200) delay(1200 - elapsed)
                                checkingUpdate = false
                                updateFeedback = "You're on the latest version (${BuildConfig.VERSION_NAME})"
                            }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "What's New & Changelogs",
                        subtitle = "View release notes and feature highlights",
                        onClick = onOpenChangelog
                    )
                    val openUrl: (String) -> Unit = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    val repoUrl = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = I18n.t("open_source", "Open Source"),
                        subtitle = I18n.t("open_source_sub", "View source code on GitHub"),
                        onClick = { openUrl(repoUrl) },
                    )
                    val openTelegram: () -> Unit = {
                        val app = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=MayankDroid7"))
                        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MayankDroid7"))
                        runCatching { context.startActivity(app) }
                            .onFailure { runCatching { context.startActivity(web) } }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Person,
                        title = I18n.t("developer", "Developer"),
                        subtitle = "Mayank (@xboyrex)",
                        onClick = openTelegram,
                    )
                }
            }
        }

        if (isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }

        AnimatedContent(
            targetState = activeFloatingSection,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.90f) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(130), targetScale = 0.92f) + fadeOut(tween(130)))
            },
            label = "settingsFloatingSection",
        ) { section ->
            when (section) {
                SettingsFloatingSection.APPEARANCE -> {
                    SettingsFloatingDialog(
                        title = I18n.t("appearance_theme", "Appearance & Theme"),
                        subtitle = I18n.t("appearance_sub", "Theme mode, color palette & UI style"),
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        AppearanceThemeContent(context = context)
                    }
                }
                SettingsFloatingSection.GENERAL_MODS -> {
                    SettingsFloatingDialog(
                        title = I18n.t("general_modification", "General Modification"),
                        subtitle = "Font, URL cleaners, short-links, Browser Shield & Guard",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        GeneralModificationContent(context = context)
                    }
                }
                SettingsFloatingSection.STORAGE_USAGE -> {
                    SettingsFloatingDialog(
                        title = I18n.t("storage_usage", "Storage Usage"),
                        subtitle = "Device memory, JSON backup & Untrack data",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        StorageUsageContent(context = context)
                    }
                }
                SettingsFloatingSection.LANGUAGE -> {
                    SettingsFloatingDialog(
                        title = I18n.t("language", "Language"),
                        subtitle = I18n.t("language_sub", "Choose application language"),
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        LanguageContent(context = context) {
                            activeFloatingSection = null
                        }
                    }
                }
                null -> {}
            }
        }

        AnimatedVisibility(
            visible = checkingUpdate || updateFeedback != null,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.88f) + fadeIn(tween(160)),
            exit = scaleOut(tween(120), targetScale = 0.92f) + fadeOut(tween(120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!checkingUpdate) updateFeedback = null
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 350.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surfaceContainerHighest)
                        .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(
                            color = c.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Checking for updates…",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Connecting to GitHub releases…",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                        )
                    } else if (updateFeedback != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF132818)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF2ECC71),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Up to date",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = updateFeedback.orEmpty(),
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.primary)
                                .pressable { updateFeedback = null }
                                .padding(horizontal = 22.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("OK", color = c.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsProfileCard(context: Context) {
    val c = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh)
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CookieShape)
                    .background(c.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "M",
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = c.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mayank (@xboyrex)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.Rounded.Verified,
                        contentDescription = "Verified Developer",
                        tint = c.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "Creator & Maintainer • Tap for official channels",
                    fontSize = 11.5.sp,
                    color = c.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = c.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(4.dp)
                    .size(18.dp)
                    .graphicsLayer {
                        rotationZ = if (expanded) 90f else 0f
                    },
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandVertically(tween(260)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 6.dp))
                listOf(
                    Triple("GitHub (@xboyrex)", "github.com/xboyrex", "https://github.com/xboyrex"),
                    Triple("Telegram Channel (@MayankDroid7)", "t.me/MayankDroid7", "https://t.me/MayankDroid7"),
                    Triple("Onyx Hub (@OnyxHub7)", "t.me/OnyxHub7", "https://t.me/OnyxHub7"),
                    Triple("Instagram (@mayank.zx7)", "instagram.com/mayank.zx7", "https://www.instagram.com/mayank.zx7"),
                    Triple("YouTube (@mayankdroid)", "youtube.com/@mayankdroid", "https://youtube.com/@mayankdroid")
                ).forEachIndexed { idx, (title, handle, url) ->
                    val background by animateColorAsState(
                        targetValue = if (idx == 0) c.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                        animationSpec = tween(220),
                        label = "socialBg"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(background)
                            .pressable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(handle, fontSize = 11.sp, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Rounded.OpenInNew, null, tint = c.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceThemeContent(context: Context) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        SettingsSwitchRow(
            title = I18n.t("liquid_glass", "Liquid Glass"),
            subtitle = if (GlassSettings.enabled) {
                I18n.t("liquid_glass_sub_on", "Frosted, see-through surfaces with depth and blur")
            } else {
                I18n.t("liquid_glass_sub_off", "Using solid Material surfaces with no transparency")
            },
            checked = GlassSettings.enabled,
            onCheckedChange = { GlassSettings.updateEnabled(it) },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(I18n.t("theme_mode", "Theme mode"), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(I18n.t("theme_mode_sub", "Choose how the app looks"), color = c.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "system" to I18n.t("theme_system", "System"),
                    "dark" to I18n.t("theme_dark", "Dark"),
                    "light" to I18n.t("theme_light", "Light"),
                    "amoled" to I18n.t("theme_amoled", "AMOLED"),
                ).forEach { (mode, label) ->
                    val on = ThemeSettings.mode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressable { ThemeSettings.chooseMode(mode) }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) c.primary else c.surfaceContainerHigh)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (on) c.onPrimary else c.onSurfaceVariant,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(I18n.t("color_palette", "Color palette"), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(I18n.t("color_palette_sub", "Choose an accent for the app"), color = c.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))

            val dark = isDarkTheme()
            val choices = buildList {
                if (ThemeSettings.wallpaperAvailable) add(null to I18n.t("wallpaper_accent", "Wallpaper"))
                UNTRACK_THEMES.forEach { add(it.hex to it.name) }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                choices.forEach { (hex, name) ->
                    val selected = ThemeSettings.seed?.lowercase() == hex?.lowercase()
                    val scheme = schemeFor(context, hex, dark)
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .pressable { ThemeSettings.choose(hex) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selected) scheme.primaryContainer else Color.Transparent)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(scheme.primary, scheme.primaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, "Selected", tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            name,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) c.onSurface else c.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralModificationContent(context: Context) {
    val c = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSwitchRow(
            title = I18n.t("use_system_font", "Use System Font"),
            subtitle = I18n.t("use_system_font_sub", "Use device default font throughout the app"),
            checked = ThemeSettings.useSystemFont,
            onCheckedChange = { ThemeSettings.updateUseSystemFont(it) },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Strip Tracking Parameters",
            subtitle = "Run all 93+ built-in sanitizers, deep cleaners & parameter rules",
            checked = UntrackStore.cleanTrackersEnabled,
            onCheckedChange = { UntrackStore.setCleanTrackers(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Automatic Frontend & Embed Conversion",
            subtitle = "Convert X/Twitter, Instagram, TikTok, Reddit & Bluesky links to selected frontends",
            checked = UntrackStore.convertFrontendsEnabled,
            onCheckedChange = { UntrackStore.setConvertFrontends(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Resolve Short Links Over Network",
            subtitle = "Unshorten amzn.to, vm.tiktok.com, bit.ly, share.google & redd.it hop-by-hop",
            checked = UntrackStore.resolveShortLinksEnabled,
            onCheckedChange = { UntrackStore.setResolveShortLinks(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Extract Bare URL Only",
            subtitle = "Discard surrounding prose when sharing messages and output only the clean link",
            checked = UntrackStore.extractUrlOnly,
            onCheckedChange = { UntrackStore.updateExtractUrlOnly(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Decode Percent-Encoded URLs",
            subtitle = "Convert %XX escape sequences into readable UTF-8 characters safely",
            checked = UntrackStore.decodeUrlEnabled,
            onCheckedChange = { UntrackStore.setDecodeUrl(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Private Link Guard",
            subtitle = "Scan URLs for leaked credentials, emails, JWTs, API keys & GPS coordinates",
            checked = UntrackStore.linkGuardEnabled,
            onCheckedChange = { UntrackStore.setLinkGuard(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Register as Browser Link Shield",
            subtitle = "Allow Android to route clicked HTTP/HTTPS links through Untrack",
            checked = UntrackStore.registeredAsBrowser,
            onCheckedChange = { UntrackStore.setBrowserInterceptor(context, it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Open Links in Chrome Custom Tabs",
            subtitle = "Use fast in-app Custom Tabs when opening cleaned URLs",
            checked = UntrackStore.useCustomTabs,
            onCheckedChange = { UntrackStore.setCustomTabs(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Protect Screen in Recents (FLAG_SECURE)",
            subtitle = "Hide Untrack preview in Android recent apps switcher and block screenshots",
            checked = UntrackStore.protectScreenEnabled,
            onCheckedChange = { UntrackStore.setProtectScreen(it) }
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

        Text("Action When Link Shared From Another App", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = c.primary)
        Spacer(Modifier.height(6.dp))
        PostCleanAction.entries.forEach { act ->
            val selected = UntrackStore.sharePostAction == act
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) c.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                    .pressable { UntrackStore.updateSharePostAction(act) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(act.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (selected) c.primary else c.onSurface)
                    Text(act.subtitle, fontSize = 11.sp, color = c.onSurfaceVariant)
                }
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = c.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private data class StorageData(
    val deviceTotal: Long,
    val deviceUsed: Long,
    val deviceFree: Long,
    val appBinary: Long,
    val appCache: Long,
    val appRulesAndHistory: Long,
    val appTotal: Long,
)

private fun getFolderSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) getFolderSize(file) else file.length()
    }
    return size
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups >= 3) {
        "%.1f %s".format(value, units[digitGroups])
    } else {
        "%.0f %s".format(value, units[digitGroups])
    }
}

private var cachedStorageData: StorageData? = null

private fun calculateStorageData(context: Context): StorageData {
    return runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = (total - free).coerceAtLeast(0L)

        val appSize = runCatching { File(context.packageCodePath).length() }.getOrDefault(8_400_000L)
        val cacheSize = getFolderSize(context.cacheDir) + (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
        val dataSize = getFolderSize(context.filesDir) + getFolderSize(File(context.applicationInfo.dataDir, "shared_prefs"))

        StorageData(
            deviceTotal = total,
            deviceUsed = used,
            deviceFree = free,
            appBinary = appSize,
            appCache = cacheSize,
            appRulesAndHistory = dataSize,
            appTotal = appSize + cacheSize + dataSize,
        )
    }.getOrDefault(
        StorageData(128_000_000_000L, 45_000_000_000L, 83_000_000_000L, 8_400_000L, 120_000L, 64_000L, 8_584_000L)
    )
}

@Composable
private fun StorageUsageContent(context: Context) {
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var storageData by remember { mutableStateOf(cachedStorageData ?: calculateStorageData(context)) }
    var isCalculating by remember { mutableStateOf(cachedStorageData == null) }
    var isClearing by remember { mutableStateOf(false) }
    var clearedNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fresh = calculateStorageData(context)
            cachedStorageData = fresh
            withContext(Dispatchers.Main) {
                storageData = fresh
                isCalculating = false
            }
        }
    }

    val usedGb = storageData.deviceUsed / 1_000_000_000.0
    val totalGb = storageData.deviceTotal / 1_000_000_000.0
    val freeGb = storageData.deviceFree / 1_000_000_000.0
    val progress = (storageData.deviceUsed.toFloat() / storageData.deviceTotal.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = I18n.t("device_storage", "Device storage"),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        var animatedTarget by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(progress) {
            animatedTarget = progress
        }
        val animatedProgress by animateFloatAsState(
            targetValue = animatedTarget,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
            label = "storageProgress",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(c.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                c.primary,
                                c.tertiary,
                                c.secondary,
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${"%.1f".format(usedGb)} GB ${I18n.t("used_of", "used of")} ${"%.1f".format(totalGb)} GB",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
            )
            Text(
                text = "${"%.1f".format(freeGb)} GB ${I18n.t("free", "free")}",
                fontSize = 11.5.sp,
                color = c.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Used by Untrack",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = c.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (isCalculating) {
                CircularProgressIndicator(
                    color = c.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${I18n.t("total_space", "Total space")}: ${formatStorageBytes(storageData.appTotal)}",
            fontSize = 12.sp,
            color = c.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(10.dp))

        StorageItemRow(label = I18n.t("app_core_engine", "App & Core engine"), sizeStr = formatStorageBytes(storageData.appBinary))
        StorageItemRow(label = "Cleaner & APK update cache", sizeStr = formatStorageBytes(storageData.appCache))
        StorageItemRow(label = "Custom rules, proxy roster & history", sizeStr = formatStorageBytes(storageData.appRulesAndHistory))

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

        Text("Local JSON Backup & Restore", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = c.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primary)
                    .pressable {
                        val json = UntrackStore.exportBackupJson()
                        copyToClipboard(context, json, "Untrack Backup JSON")
                        Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Export Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c.onPrimary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceContainerHigh)
                    .pressable {
                        val clip = readFromClipboard(context)
                        if (!clip.isNullOrBlank() && UntrackStore.importBackupJson(clip)) {
                            Toast.makeText(context, "Backup restored from clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Copy a valid Untrack backup JSON first", Toast.LENGTH_LONG).show()
                        }
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Restore Clipboard", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c.onSurface)
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E1517))
                .pressable(enabled = !isClearing) {
                    isClearing = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.cacheDir.deleteRecursively()
                                context.externalCacheDir?.deleteRecursively()
                            }
                            val fresh = calculateStorageData(context)
                            cachedStorageData = fresh
                            withContext(Dispatchers.Main) {
                                UntrackStore.clearHistory()
                                storageData = fresh
                                isClearing = false
                                clearedNotice = "Cache & history cleared!"
                            }
                        }
                    }
                }
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isClearing) {
                    CircularProgressIndicator(
                        color = Color(0xFFEF5350),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = clearedNotice ?: "Clear Untrack Cache & History",
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

@Composable
private fun StorageItemRow(label: String, sizeStr: String) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = c.onSurfaceVariant)
        Text(sizeStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
    }
}

@Composable
private fun LanguageContent(context: Context, onLanguageSelected: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val currentCode = LanguageSettings.currentCode
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            LanguageSettings.ALL_LANGUAGES
        } else {
            LanguageSettings.ALL_LANGUAGES.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.nativeName.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceContainerLow)
                .border(1.dp, c.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search",
                tint = c.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(
                    color = c.onSurface,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(c.primary),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = I18n.t("search_languages", "Search languages..."),
                            color = c.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 13.5.sp,
                        )
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = c.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { searchQuery = "" },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredLanguages, key = { it.code }) { item ->
                val isSelected = currentCode.equals(item.code, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) c.primary.copy(alpha = 0.14f) else c.surfaceContainerLow.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) c.primary.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            LanguageSettings.choose(context, item.code)
                            onLanguageSelected()
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.nativeName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (isSelected) c.primary else c.onSurface,
                        )
                        Text(
                            text = if (item.code == "auto") I18n.t("follow_system", "Follow device language") else item.name,
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(c.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = c.onPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val c = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = c.onSurfaceVariant.copy(alpha = 0.72f),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(c.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.onSurface,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = c.onSurface,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = c.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
                tint = c.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsFloatingDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 410.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceContainerHighest)
                .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            HorizontalDivider(
                color = c.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable { onCheckedChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = c.onSurface,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = c.onSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary),
        )
    }
}
