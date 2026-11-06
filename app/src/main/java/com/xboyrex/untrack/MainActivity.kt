package com.xboyrex.untrack

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay

enum class UntrackTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Clean("Clean", Icons.Outlined.AutoFixHigh, Icons.Rounded.AutoFixHigh),
    Frontends("Frontends", Icons.Outlined.Public, Icons.Rounded.Public),
    Rules("Rules", Icons.Outlined.Rule, Icons.Rounded.Rule),
    History("History", Icons.Outlined.History, Icons.Rounded.History),
    Settings("Settings", Icons.Outlined.Settings, Icons.Rounded.Settings);
}

private val NavHeight = 64.dp
private val NavWidth = 384.dp
private val NavLift = 10.dp

class MainActivity : ComponentActivity() {
    internal var incomingIntentCallback: ((Intent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setHighestRefreshRate()

        setContent {
            UntrackTheme {
                UntrackApp(activity = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setHighestRefreshRate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentCallback?.invoke(intent)
    }

    private fun setHighestRefreshRate() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val disp = display ?: return
                val currentMode = disp.mode
                val bestMode = disp.supportedModes
                    .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                    .maxByOrNull { it.refreshRate }
                    ?: disp.supportedModes.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = bestMode.modeId
                    lp.preferredRefreshRate = bestMode.refreshRate
                    window.attributes = lp
                }
            }
        }
    }
}

@Composable
fun UntrackApp(activity: MainActivity) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reduced = rememberReducedMotion()

    var tab by rememberSaveable { mutableStateOf(UntrackTab.Clean) }
    var sharedInputText by remember { mutableStateOf<String?>(null) }
    val updateState = remember { UpdateState(scope) }

    // Apply FLAG_SECURE when Protect Screen is enabled
    LaunchedEffect(UntrackStore.protectScreenEnabled) {
        runCatching {
            if (UntrackStore.protectScreenEnabled) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Automatic GitHub update check on startup
    LaunchedEffect(Unit) {
        delay(1_400)
        updateState.check(manual = false)
    }

    LaunchedEffect(updateState.message) {
        updateState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            updateState.message = null
        }
    }

    fun extractIntentText(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.dataString
        }?.takeIf { it.isNotBlank() }
    }

    DisposableEffect(activity) {
        extractIntentText(activity.intent)?.let { text ->
            sharedInputText = text
            tab = UntrackTab.Clean
        }
        activity.incomingIntentCallback = { newIntent ->
            extractIntentText(newIntent)?.let { text ->
                sharedInputText = text
                tab = UntrackTab.Clean
            }
        }
        onDispose { activity.incomingIntentCallback = null }
    }

    BackHandler(enabled = updateState.changelogVisible) {
        updateState.changelogVisible = false
    }
    BackHandler(enabled = !updateState.changelogVisible && updateState.visible) {
        updateState.dismiss()
    }
    BackHandler(enabled = !updateState.changelogVisible && !updateState.visible && tab != UntrackTab.Clean) {
        tab = UntrackTab.Clean
    }

    val pageBackdrop = rememberLayerBackdrop()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val stackBottom = navBottom.coerceAtLeast(8.dp) +
        if (WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding() > 0.dp) 10.dp else 0.dp
    val navTop = stackBottom + NavLift + NavHeight
    val contentPadding = PaddingValues(bottom = navTop + 16.dp)

    BoxWithConstraints(Modifier.fillMaxSize().background(c.surface)) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .background(c.surface)
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight(),
                transitionSpec = {
                    if (reduced) fadeIn(snap()) togetherWith fadeOut(snap())
                    else (fadeIn(tween(Motion.PageEnterMs, easing = Motion.Emphasized)) +
                        slideInVertically(tween(Motion.PageRiseMs, easing = Motion.Emphasized)) { (it / 35).coerceIn(8, 24) }) togetherWith
                        fadeOut(tween(Motion.PageExitMs))
                },
                label = "untrackTabs",
            ) { currentTab ->
                when (currentTab) {
                    UntrackTab.Clean -> CleanStudioScreen(
                        incomingText = sharedInputText,
                        onConsumeIncomingText = { sharedInputText = null },
                        onOpenChangelog = { updateState.changelogVisible = true },
                        onNavigateTab = { tab = it },
                        contentPadding = contentPadding
                    )
                    UntrackTab.Frontends -> FrontendsScreen(
                        contentPadding = contentPadding
                    )
                    UntrackTab.Rules -> RulesScreen(
                        contentPadding = contentPadding
                    )
                    UntrackTab.History -> HistoryScreen(
                        contentPadding = contentPadding
                    )
                    UntrackTab.Settings -> SettingsScreen(
                        contentPadding = contentPadding,
                        onCheckUpdate = { updateState.check(manual = true) },
                        onOpenChangelog = { updateState.changelogVisible = true }
                    )
                }
            }

            BottomGestureGuard(Modifier.align(Alignment.BottomCenter))
        }

        // Liquid Glass Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = stackBottom + NavLift)
                .widthIn(max = NavWidth)
                .fillMaxWidth()
        ) {
            LiquidBottomBar(
                selectedTabIndex = { UntrackTab.entries.indexOf(tab) },
                onTabSelected = { index -> UntrackTab.entries.getOrNull(index)?.let { tab = it } },
                backdrop = pageBackdrop,
                modifier = Modifier.fillMaxWidth(),
            ) {
                UntrackTab.entries.forEach { t ->
                    LiquidNavTab(
                        tab = t,
                        selected = t == tab,
                        onClick = { tab = t }
                    )
                }
            }
        }

        UpdateOverlay(state = updateState, backdrop = pageBackdrop)
        ChangelogOverlay(
            visible = updateState.changelogVisible,
            backdrop = pageBackdrop,
            onDismiss = { updateState.changelogVisible = false },
            onCheckUpdate = { updateState.check(manual = true) }
        )
    }
}
