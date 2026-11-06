package com.xboyrex.untrack

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun copyToClipboard(context: Context, text: String, label: String = "Cleaned URL") {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun readFromClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount <= 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}

fun shareText(context: Context, text: String) {
    if (text.isBlank()) return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(sendIntent, "Share Clean Link").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}

fun openUrlExternal(context: Context, url: String) {
    if (url.isBlank()) return
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    if (UntrackStore.useCustomTabs) {
        val launched = runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
        }.isSuccess
        if (launched) return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun CleanStudioScreen(
    incomingText: String?,
    onConsumeIncomingText: () -> Unit,
    onOpenChangelog: () -> Unit,
    onNavigateTab: (UntrackTab) -> Unit,
    contentPadding: PaddingValues,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var batchResult by remember { mutableStateOf<UntrackBatchResult?>(null) }
    var convertFrontendDirection by remember { mutableStateOf(UntrackStore.convertFrontendsEnabled) }
    var showQuickOptions by remember { mutableStateOf(false) }
    var manualParamRevision by remember { mutableIntStateOf(0) }

    val manuallyKeptParams = remember { mutableStateListOf<String>() }
    val manuallyRemovedParams = remember { mutableStateListOf<String>() }

    fun saveReportToHistoryIfModified(rep: CleanedUrlReport?) {
        if (rep != null && rep.wasModified) {
            UntrackStore.recordCleanResult(
                HistoryItem(
                    originalUrl = rep.originalUrl,
                    cleanedUrl = rep.cleanedUrl,
                    domain = rep.parsedCleaned.site,
                    removedParamsCount = rep.parameterRows.count { it.isRemoved },
                    frontendConverted = rep.convertedFrontendDomain,
                    shortLinkResolved = rep.shortLinkResolved
                )
            )
        }
    }

    // Real-time automatic URL cleaning whenever inputText or options change
    LaunchedEffect(
        inputText,
        convertFrontendDirection,
        manualParamRevision,
        UntrackStore.cleanTrackersEnabled,
        UntrackStore.resolveShortLinksEnabled,
        UntrackStore.extractUrlOnly,
        UntrackStore.decodeUrlEnabled,
        UntrackStore.customRulesEnabled
    ) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            batchResult = null
            isProcessing = false
            return@LaunchedEffect
        }
        delay(35)
        isProcessing = true
        val res = UntrackPipeline.processText(
            rawText = inputText,
            convertToAlternativeOverride = convertFrontendDirection,
            manuallyKeptParams = manuallyKeptParams.toSet(),
            manuallyRemovedParams = manuallyRemovedParams.toSet()
        )
        batchResult = res
        isProcessing = false
    }

    // Handle shared / incoming links from Android Share Sheet or Browser Shield
    LaunchedEffect(incomingText) {
        if (!incomingText.isNullOrBlank()) {
            inputText = incomingText
            manuallyKeptParams.clear()
            manuallyRemovedParams.clear()
            convertFrontendDirection = UntrackStore.convertFrontendsEnabled
            isProcessing = true
            val res = UntrackPipeline.processText(incomingText, convertFrontendDirection)
            batchResult = res
            isProcessing = false
            saveReportToHistoryIfModified(res.primaryReport)
            when (UntrackStore.sharePostAction) {
                PostCleanAction.AUTO_COPY -> copyToClipboard(context, res.outputText)
                PostCleanAction.AUTO_SHARE -> shareText(context, res.outputText)
                PostCleanAction.AUTO_OPEN -> res.primaryReport?.cleanedUrl?.let { openUrlExternal(context, it) }
                PostCleanAction.SHOW_IN_APP -> Unit
            }
            onConsumeIncomingText()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        // BrandMark appears ONLY here in the main Home header (just like YTXTube)
        ScreenHeader(
            title = "Untrack",
            brandMark = true
        ) {
            CircleAction(
                icon = Icons.Rounded.AutoAwesome,
                contentDescription = "What's New",
                onClick = onOpenChangelog
            )
            CircleAction(
                icon = Icons.Rounded.Tune,
                contentDescription = "Quick Options",
                onClick = { showQuickOptions = !showQuickOptions }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = contentPadding.calculateBottomPadding() + 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Collapsible Quick Pipeline Options
            AnimatedVisibility(
                visible = showQuickOptions,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Pipeline Controls",
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = c.primary
                    )
                    MiniSwitchRow("Strip Tracking Parameters", UntrackStore.cleanTrackersEnabled) {
                        UntrackStore.setCleanTrackers(it)
                    }
                    MiniSwitchRow("Convert to Privacy / Embed Frontends", convertFrontendDirection) {
                        convertFrontendDirection = it
                        UntrackStore.setConvertFrontends(it)
                    }
                    MiniSwitchRow("Resolve Short Links (amzn.to, vm.tiktok, bit.ly)", UntrackStore.resolveShortLinksEnabled) {
                        UntrackStore.setResolveShortLinks(it)
                    }
                    MiniSwitchRow("Extract Bare URL Only (Strip Surrounding Text)", UntrackStore.extractUrlOnly) {
                        UntrackStore.updateExtractUrlOnly(it)
                    }
                    MiniSwitchRow("Decode Percent-Encoded Characters", UntrackStore.decodeUrlEnabled) {
                        UntrackStore.setDecodeUrl(it)
                    }
                }
            }

            // Input Studio Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.surfaceContainerHigh)
                    .border(1.dp, c.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Input URL or Shared Text",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = c.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = c.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (inputText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.surfaceContainerHighest)
                                    .pressable {
                                        inputText = ""
                                        batchResult = null
                                        manuallyKeptParams.clear()
                                        manuallyRemovedParams.clear()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Close, null, tint = c.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = c.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceContainerLowest)
                        .border(1.dp, c.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            manuallyKeptParams.clear()
                            manuallyRemovedParams.clear()
                        },
                        textStyle = TextStyle(
                            color = c.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(c.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        decorationBox = { inner ->
                            if (inputText.isEmpty()) {
                                Text(
                                    "Paste or type any link here — Untrack cleans tracking parameters, redirects, and social embeds automatically in real time...",
                                    color = c.onSurfaceVariant.copy(alpha = 0.55f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Paste & Clean Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (inputText.isBlank()) c.primary else c.surfaceContainerHighest)
                            .pressable {
                                val clip = readFromClipboard(context)
                                if (!clip.isNullOrBlank()) {
                                    inputText = clip
                                    manuallyKeptParams.clear()
                                    manuallyRemovedParams.clear()
                                    scope.launch {
                                        val res = UntrackPipeline.processText(clip, convertFrontendDirection)
                                        batchResult = res
                                        saveReportToHistoryIfModified(res.primaryReport)
                                    }
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.ContentPaste,
                            null,
                            tint = if (inputText.isBlank()) c.onPrimary else c.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Paste & Clean",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (inputText.isBlank()) c.onPrimary else c.onSurface
                        )
                    }

                    // Copy Clean Link Button (enabled only when there is a valid result)
                    val hasValidOutput = inputText.isNotBlank() && batchResult != null
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (hasValidOutput) c.primary else c.surfaceContainerHighest.copy(alpha = 0.55f))
                            .pressable(enabled = hasValidOutput) {
                                val currentRes = batchResult ?: return@pressable
                                saveReportToHistoryIfModified(currentRes.primaryReport)
                                copyToClipboard(context, currentRes.outputText)
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            null,
                            tint = if (hasValidOutput) c.onPrimary else c.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Copy Clean Link",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = if (hasValidOutput) c.onPrimary else c.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Results Section (Updates in Real Time)
            val currentBatch = batchResult
            if (currentBatch != null && currentBatch.urlReports.isNotEmpty()) {
                val primary = currentBatch.primaryReport!!

                // Private Link Guard Alert Banner (if sensitive leak detected)
                if (primary.leakWarnings.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.errorContainer)
                            .border(1.dp, c.error.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WarningAmber, null, tint = c.onErrorContainer, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Private Link Guard Warning",
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = c.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.error)
                                    .pressable {
                                        val redacted = LinkLeakGuard.redactLeaks(primary.parsedCleaned).toString()
                                        inputText = redacted
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Redact Now", color = c.onError, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        primary.leakWarnings.forEach { w ->
                            Text(
                                "• ${w.type.label}${w.parameterName?.let { " (?$it)" } ?: ""}: ${w.type.description}",
                                color = c.onErrorContainer,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Live Result Card (distinct styling for Modified vs Already Clean)
                val isModified = primary.wasModified
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = if (isModified) c.primary.copy(alpha = 0.45f) else c.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isModified) c.primary.copy(alpha = 0.16f)
                                    else c.surfaceContainerHighest
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isModified) Icons.Rounded.VerifiedUser else Icons.Rounded.CheckCircle,
                                null,
                                tint = if (isModified) c.primary else c.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isModified) "Cleaned & Untracked Result" else "Already Clean Link",
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = c.onSurface
                            )
                            val badgeSummary = buildList {
                                val removedCount = primary.parameterRows.count { it.isRemoved }
                                if (removedCount > 0) add("$removedCount tracker${if (removedCount == 1) "" else "s"} removed")
                                if (primary.convertedFrontendDomain != null) add("Frontend: ${primary.convertedFrontendDomain}")
                                if (primary.shortLinkResolved) add("Short link unshortened")
                                if (isEmpty()) {
                                    if (isModified) add("Normalized clean URL")
                                    else add("Zero tracking parameters detected")
                                }
                            }.joinToString(" • ")
                            Text(
                                badgeSummary,
                                fontSize = 11.5.sp,
                                color = if (isModified) c.primary else c.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Bidirectional Frontend Toggle Button (Convert <-> Restore Original Domain)
                        if (primary.detectedPlatform != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.surfaceContainerHighest)
                                    .pressable {
                                        convertFrontendDirection = !convertFrontendDirection
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.SwapHoriz, null, tint = c.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (convertFrontendDirection) "Original Host" else "Embed Host",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = c.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.surfaceContainerLowest)
                                .border(
                                    1.dp,
                                    if (isModified) c.primary.copy(alpha = 0.25f) else c.onSurface.copy(alpha = 0.08f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    saveReportToHistoryIfModified(primary)
                                    copyToClipboard(context, currentBatch.outputText)
                                }
                                .padding(14.dp)
                        ) {
                            Text(
                                text = currentBatch.outputText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                color = if (isModified) c.primary else c.onSurface
                            )
                        }
                    }

                    // Quick Platform Proxy Switcher Pills (if a supported platform is detected)
                    primary.detectedPlatform?.let { platform ->
                        val targets = FrontendCatalog.targetsFor(
                            platform,
                            UntrackStore.customFrontendDomains[platform].orEmpty()
                        )
                        if (targets.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Switch ${platform.title} Target Frontend:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                targets.forEach { target ->
                                    val selected = UntrackStore.getSelectedDomain(platform)
                                        .equals(target.domain, ignoreCase = true) && convertFrontendDirection
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) c.primary else c.surfaceContainerHighest)
                                            .pressable {
                                                UntrackStore.setPlatformEnabled(platform, true)
                                                UntrackStore.setSelectedDomain(platform, target.domain)
                                                convertFrontendDirection = true
                                                manualParamRevision++
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = target.domain,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (selected) c.onPrimary else c.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Action Buttons Row: Copy, Share, Open, Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButtonChip(
                            icon = Icons.Rounded.ContentCopy,
                            label = "Copy",
                            primary = true,
                            modifier = Modifier.weight(1f)
                        ) {
                            saveReportToHistoryIfModified(primary)
                            copyToClipboard(context, currentBatch.outputText)
                        }
                        ActionButtonChip(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            primary = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            saveReportToHistoryIfModified(primary)
                            shareText(context, currentBatch.outputText)
                        }
                        ActionButtonChip(
                            icon = Icons.Rounded.OpenInBrowser,
                            label = "Open",
                            primary = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            saveReportToHistoryIfModified(primary)
                            openUrlExternal(context, primary.cleanedUrl)
                        }
                        ActionButtonChip(
                            icon = Icons.Rounded.Refresh,
                            label = "Reset",
                            primary = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            inputText = ""
                            batchResult = null
                            manuallyKeptParams.clear()
                            manuallyRemovedParams.clear()
                        }
                    }
                }

                // Interactive Changes & Parameter Inspector Card
                if (primary.parameterRows.isNotEmpty() || primary.appliedRuleNames.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(c.surfaceContainerHigh)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Interactive Parameter & Rule Inspector",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = c.onSurface
                        )
                        Text(
                            "Uncheck any removed parameter to keep it, or check any remaining parameter to strip it (remembered per site).",
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        if (primary.appliedRuleNames.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                primary.appliedRuleNames.forEach { ruleName ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(c.primaryContainer.copy(alpha = 0.6f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = ruleName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = c.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        if (primary.parameterRows.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))
                            Spacer(Modifier.height(6.dp))

                            primary.parameterRows.forEach { row ->
                                val pName = row.paramName ?: return@forEach
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val newRemove = !row.isRemoved
                                            if (newRemove) {
                                                manuallyKeptParams.remove(pName)
                                                if (pName !in manuallyRemovedParams) manuallyRemovedParams.add(pName)
                                            } else {
                                                manuallyRemovedParams.remove(pName)
                                                if (pName !in manuallyKeptParams) manuallyKeptParams.add(pName)
                                            }
                                            UntrackStore.setSiteParamDecision(primary.parsedCleaned.site, pName, newRemove)
                                            manualParamRevision++
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = row.isRemoved,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                manuallyKeptParams.remove(pName)
                                                if (pName !in manuallyRemovedParams) manuallyRemovedParams.add(pName)
                                            } else {
                                                manuallyRemovedParams.remove(pName)
                                                if (pName !in manuallyKeptParams) manuallyKeptParams.add(pName)
                                            }
                                            UntrackStore.setSiteParamDecision(primary.parsedCleaned.site, pName, checked)
                                            manualParamRevision++
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = c.primary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.label,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = if (row.isRemoved) c.error else c.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = row.detail,
                                            fontSize = 11.sp,
                                            color = c.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Clean Empty State (YTXTube SunnyShape style, no duplicate square logo)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(SunnyShape)
                            .background(c.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = c.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Real-Time URL Sanitizer & Privacy Engine",
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = c.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Share any link from any app to Untrack, highlight text anywhere and tap 'Untrack Link', or paste above to strip 115+ tracking systems, unwrap shortlinks, and switch social links to privacy frontends.",
                        fontSize = 13.sp,
                        lineHeight = 18.5.sp,
                        color = c.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickJumpCard(
                            title = "9 Platforms",
                            subtitle = "32 Privacy & Embed Frontends",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(UntrackTab.Frontends) }
                        )
                        QuickJumpCard(
                            title = "115+ Filters",
                            subtitle = "Built-in + RE2J Custom Rules",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTab(UntrackTab.Rules) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable { onCheckedChange(!checked) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = c.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
        )
    }
}

@Composable
private fun ActionButtonChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) c.primary else c.surfaceContainerHighest)
            .pressable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (primary) c.onPrimary else c.onSurface,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (primary) c.onPrimary else c.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickJumpCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfaceContainerHighest)
            .pressable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontFamily = RoundedHeavy,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = c.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 11.5.sp,
            color = c.onSurfaceVariant
        )
    }
}
