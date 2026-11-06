package com.xboyrex.untrack

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val items = UntrackStore.history

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(
            title = "Clean History",
            brandMark = false
        ) {
            if (items.isNotEmpty()) {
                CircleAction(
                    icon = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear All History",
                    onClick = { UntrackStore.clearHistory() }
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentPadding.calculateBottomPadding()),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "No Cleaned Links Yet",
                    body = "Links you clean in Untrack Studio or via the system Share menu will appear here for instant copying and sharing."
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val formatter = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.surfaceContainerHigh)
                            .border(1.dp, c.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c.primary.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.domain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = c.primary
                                )
                            }
                            if (item.removedParamsCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(c.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "-${item.removedParamsCount} trackers",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = c.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = formatter.format(Date(item.timestamp)),
                                fontSize = 11.sp,
                                color = c.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = item.cleanedUrl,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = c.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                copyToClipboard(context, item.cleanedUrl)
                            }
                        )

                        if (item.originalUrl != item.cleanedUrl) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "From: ${item.originalUrl}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = c.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HistoryIconAction(Icons.Rounded.ContentCopy, "Copy") {
                                copyToClipboard(context, item.cleanedUrl)
                            }
                            Spacer(Modifier.width(8.dp))
                            HistoryIconAction(Icons.Rounded.Share, "Share") {
                                shareText(context, item.cleanedUrl)
                            }
                            Spacer(Modifier.width(8.dp))
                            HistoryIconAction(Icons.Rounded.OpenInBrowser, "Open") {
                                openUrlExternal(context, item.cleanedUrl)
                            }
                            Spacer(Modifier.width(8.dp))
                            HistoryIconAction(Icons.Rounded.Delete, "Delete") {
                                UntrackStore.deleteHistoryItem(item.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceContainerHighest)
            .pressable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = c.onSurface, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
    }
}
