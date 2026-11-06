package com.xboyrex.untrack

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FrontendsScreen(
    contentPadding: PaddingValues,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var expandedPlatform by remember { mutableStateOf<FrontendPlatform?>(FrontendPlatform.X) }
    var customDomainInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(
            title = "Frontends & Proxies",
            brandMark = false
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = contentPadding.calculateBottomPadding() + 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master conversion switch card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SwapCalls, null, tint = c.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Automatic Frontend Conversion",
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = c.onSurface
                    )
                    Text(
                        "Convert supported social links to your preferred embed or privacy domain",
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant
                    )
                }
                Switch(
                    checked = UntrackStore.convertFrontendsEnabled,
                    onCheckedChange = { UntrackStore.setConvertFrontends(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                )
            }

            // Platform Cards
            FrontendPlatform.entries.forEach { platform ->
                val enabled = UntrackStore.isPlatformEnabled(platform)
                val selectedDomain = UntrackStore.getSelectedDomain(platform)
                val customList = UntrackStore.customFrontendDomains[platform].orEmpty()
                val targets = FrontendCatalog.targetsFor(platform, customList)
                val isExpanded = expandedPlatform == platform

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = if (isExpanded) c.primary.copy(alpha = 0.45f) else c.outlineVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedPlatform = if (isExpanded) null else platform
                                customDomainInput = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (enabled) c.primaryContainer else c.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = platform.monogram,
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (enabled) c.onPrimaryContainer else c.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = platform.title,
                                fontFamily = RoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = c.onSurface
                            )
                            Text(
                                text = if (enabled && selectedDomain.isNotBlank()) {
                                    "${platform.sourceDomains.first()} → $selectedDomain"
                                } else {
                                    "Tracking removal only (keep ${platform.sourceDomains.first()})"
                                },
                                fontSize = 12.sp,
                                color = if (enabled) c.primary else c.onSurfaceVariant,
                                fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { UntrackStore.setPlatformEnabled(platform, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))
                            Spacer(Modifier.height(2.dp))

                            targets.forEach { target ->
                                val chosen = selectedDomain.equals(target.domain, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (chosen) c.primaryContainer.copy(alpha = 0.55f) else c.surfaceContainerLowest)
                                        .border(
                                            1.dp,
                                            if (chosen) c.primary.copy(alpha = 0.6f) else c.onSurface.copy(alpha = 0.06f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .pressable {
                                            UntrackStore.setSelectedDomain(platform, target.domain)
                                            UntrackStore.setPlatformEnabled(platform, true)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = if (chosen) c.primary else c.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = target.domain + (target.pathPrefix ?: ""),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (chosen) c.primary else c.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(c.surfaceContainerHighest)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = target.role.badge,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = c.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = target.description,
                                            fontSize = 11.sp,
                                            color = c.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (target.isCustom) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .pressable {
                                                    UntrackStore.removeCustomDomain(platform, target.domain)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Delete, "Remove custom domain", tint = c.error, modifier = Modifier.size(16.dp))
                                        }
                                    } else if (chosen) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(c.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Check, null, tint = c.onPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            // Add custom proxy domain input
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.surfaceContainerLowest)
                                        .border(1.dp, c.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    BasicTextField(
                                        value = customDomainInput,
                                        onValueChange = { customDomainInput = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = c.onSurface,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        cursorBrush = SolidColor(c.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (customDomainInput.isEmpty()) {
                                                Text(
                                                    "Add custom ${platform.title} domain (e.g. myproxy.org)...",
                                                    fontSize = 12.sp,
                                                    color = c.onSurfaceVariant.copy(alpha = 0.55f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.primary)
                                        .pressable {
                                            if (UntrackStore.addCustomDomain(platform, customDomainInput)) {
                                                customDomainInput = ""
                                                Toast.makeText(context, "Custom proxy added!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Enter a valid hostname (e.g. nitter.example.com)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Add, null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = c.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
