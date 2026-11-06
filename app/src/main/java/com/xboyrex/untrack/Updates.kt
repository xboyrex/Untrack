package com.xboyrex.untrack

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File

@Immutable
data class UpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

@Immutable
data class ChangelogRelease(
    val version: String,
    val date: String,
    val badge: String,
    val highlights: List<String>
)

internal val UNTRACK_CHANGELOG_HISTORY = listOf(
    ChangelogRelease(
        version = "v1.0.0",
        date = "Latest Release",
        badge = "STABLE",
        highlights = listOf(
            "Unified URL Sanitizer Engine with 95+ built-in tracking removal rules across global ad networks, social platforms, shopping sites, and search redirects.",
            "Multi-Platform Alternative Privacy Frontends & Rich Media Embeds for X/Twitter (fixupx, fxtwitter, vxtwitter, xcancel, nitter), Instagram (toinstagram, kittygram), TikTok (tnktok, tfxktok), Reddit (redlib, safereddit), Bluesky (fxbsky, skylib), YouTube (Invidious), Pinterest, and Threads.",
            "Privacy-Preserving Short-Link Unshortener: Resolves amzn.to, vm.tiktok.com, redd.it, share.google, maps.app.goo.gl, bit.ly, t.co, and tinyurl.com hop-by-hop without loading destination pages.",
            "Custom URL Rules Engine + Teach-by-Example Inference & Live Rule Test Lab.",
            "Private Link Guard: Detects leaked credentials, emails, JWT tokens, API keys, and GPS coordinates before you share.",
            "Liquid Glass UI & Navigation Bar with refractive shaders, AMOLED dark mode, 9 accent palettes, and in-app GitHub Release OTA APK Updater."
        )
    )
)

internal val DEFAULT_RELEASE_NOTES = UNTRACK_CHANGELOG_HISTORY.first().highlights.joinToString("\n") { "• $it" }

@Stable
class UpdateState(private val scope: CoroutineScope) {

    var info by mutableStateOf<UpdateInfo?>(null)
        private set
    var visible by mutableStateOf(false)
        private set
    var changelogVisible by mutableStateOf(false)
    var checking by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var downloaded by mutableStateOf<File?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    private var downloadJob: Job? = null

    fun check(manual: Boolean = false) {
        if (checking) return
        checking = true
        scope.launch {
            val latest = withContext(Dispatchers.IO) { runCatching { fetchLatest() }.getOrNull() }
            checking = false
            if (latest == null) {
                if (manual) message = "You're on the latest version (v${BuildConfig.VERSION_NAME})."
                return@launch
            }
            if (isNewerVersion(latest.version, BuildConfig.VERSION_NAME)) {
                info = latest
                error = null
                visible = true
            } else if (manual) {
                message = "You're on the latest version (v${BuildConfig.VERSION_NAME})."
            }
        }
    }

    fun previewLatestUpdateDialog() {
        info = UpdateInfo(
            version = BuildConfig.VERSION_NAME,
            tag = "v${BuildConfig.VERSION_NAME}",
            notes = DEFAULT_RELEASE_NOTES,
            apkUrl = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest/download/Untrack.apk",
            sizeBytes = 6_450_000L
        )
        error = null
        visible = true
    }

    fun download(context: Context) {
        val update = info ?: return
        if (downloading) return
        downloading = true
        error = null
        progress = 0f
        downloadJob = scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { downloadApk(context, update) { p -> progress = p } }.getOrNull()
            }
            downloading = false
            downloadJob = null
            if (file != null) {
                downloaded = file
                install(context)
            } else {
                error = "Download failed. Check your connection or GitHub release assets."
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloading = false
        progress = 0f
    }

    fun install(context: Context) {
        val file = downloaded ?: return
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            error = "Allow installs from Untrack, then tap Install again."
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        error = null
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            error = "Couldn't open the system package installer."
        }
    }

    fun dismiss() {
        if (!downloading) visible = false
    }

    private fun fetchLatest(): UpdateInfo? {
        val apiUrl = "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Untrack/${BuildConfig.VERSION_NAME}")
            .build()
        NetClient.downloadHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body?.string().orEmpty())
            val tag = json.optString("tag_name").ifBlank { return null }
            val version = tag.removePrefix("v").removePrefix("V").trim()
            if (version.isBlank()) return null
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val candidate = asset.optString("browser_download_url")
                if (candidate.isBlank()) continue
                if (apkUrl == null || name.equals("Untrack.apk", ignoreCase = true)) {
                    apkUrl = candidate
                    size = asset.optLong("size")
                }
                if (name.equals("Untrack.apk", ignoreCase = true)) break
            }
            val url = apkUrl ?: return null
            return UpdateInfo(
                version = version,
                tag = tag,
                notes = json.optString("body").orEmpty(),
                apkUrl = url,
                sizeBytes = size,
            )
        }
    }

    private suspend fun downloadApk(
        context: Context,
        update: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "Untrack.apk")
        if (target.exists()) target.delete()
        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "Untrack/${BuildConfig.VERSION_NAME}")
            .build()
        NetClient.downloadHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: update.sizeBytes
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastTick = 0L
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val p = (done.toFloat() / total).coerceIn(0f, 1f)
                            val now = System.currentTimeMillis()
                            if (now - lastTick > 80 || p >= 1f) {
                                lastTick = now
                                withContext(Dispatchers.Main) { onProgress(p) }
                            }
                        }
                    }
                }
            }
        }
        if (target.length() <= 0L) error("Empty download")
        return target
    }
}

fun isNewerVersion(remote: String, current: String): Boolean {
    fun parts(value: String) = value.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }
    val r = parts(remote)
    val c = parts(current)
    for (i in 0 until maxOf(r.size, c.size)) {
        val a = r.getOrElse(i) { 0 }
        val b = c.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

private fun formatUpdateSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

@Composable
fun UpdateOverlay(state: UpdateState, backdrop: Backdrop) {
    val context = LocalContext.current
    val update = state.info
    val c = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = state.visible && update != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { state.dismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = state.visible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                    initialScale = 0.86f,
                ) + fadeIn(tween(220)),
                exit = scaleOut(tween(140), targetScale = 0.94f) + fadeOut(tween(140)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .widthIn(max = 410.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .liquidGlass(
                            backdrop,
                            glassSpec(
                                shape = RoundedCornerShape(30.dp),
                                tint = glassFill(0.92f),
                                blur = 26.dp,
                                refractionHeight = 16.dp,
                                refractionAmount = 24.dp,
                            ),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Update available",
                                color = c.onSurface,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Untrack ${update?.version.orEmpty()}" +
                                    formatUpdateSize(update?.sizeBytes ?: 0L).let { if (it.isBlank()) "" else " · $it" },
                                color = c.onSurfaceVariant,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .pressable(onClick = { state.dismiss() }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close update dialog",
                                tint = c.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "What's new",
                        color = c.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        update?.notes?.trim()?.ifBlank { DEFAULT_RELEASE_NOTES } ?: DEFAULT_RELEASE_NOTES,
                        color = c.onSurface.copy(alpha = 0.92f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .verticalScroll(rememberScrollState()),
                    )

                    state.error?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            message,
                            color = c.error,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (state.downloading) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = c.primary,
                            trackColor = c.onSurface.copy(alpha = 0.12f),
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "${(state.progress * 100).toInt()}% · downloading Untrack.apk",
                            color = c.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                if (state.downloading) state.cancelDownload() else state.dismiss()
                            },
                        ) {
                            Text(
                                "Cancel",
                                color = c.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (state.downloaded != null) state.install(context) else state.download(context)
                            },
                            enabled = !state.downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.primary,
                                contentColor = c.onPrimary,
                            ),
                        ) {
                            if (state.checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = c.onPrimary,
                                )
                            } else {
                                Text(
                                    "Install Update",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Downloaded from GitHub Releases · ${update?.tag.orEmpty()}",
                        color = c.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun ChangelogOverlay(
    visible: Boolean,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.80f, stiffness = 380f),
                    initialScale = 0.88f,
                ) + fadeIn(tween(200)),
                exit = scaleOut(tween(140), targetScale = 0.94f) + fadeOut(tween(140)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(max = 420.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .liquidGlass(
                            backdrop,
                            glassSpec(
                                shape = RoundedCornerShape(30.dp),
                                tint = glassFill(0.94f),
                                blur = 26.dp,
                                refractionHeight = 16.dp,
                                refractionAmount = 24.dp,
                            ),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "What's New in Untrack",
                                fontFamily = RoundedHeavy,
                                color = c.onSurface,
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                "Release Changelogs & Build Notes",
                                color = c.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Box(
                            Modifier
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
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UNTRACK_CHANGELOG_HISTORY.forEach { rel ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(c.surfaceContainerHigh.copy(alpha = 0.7f))
                                    .border(1.dp, c.primary.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.NewReleases,
                                        contentDescription = null,
                                        tint = c.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Untrack ${rel.version}",
                                        fontFamily = RoundedHeavy,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = c.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(c.primary.copy(alpha = 0.16f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = rel.badge,
                                            color = c.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                rel.highlights.forEach { bullet ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            color = c.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = bullet,
                                            color = c.onSurface.copy(alpha = 0.92f),
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            onDismiss()
                            onCheckUpdate()
                        }) {
                            Text("Check for Updates", color = c.primary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.primary,
                                contentColor = c.onPrimary
                            )
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
