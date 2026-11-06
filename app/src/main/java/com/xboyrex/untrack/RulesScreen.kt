package com.xboyrex.untrack

import com.xboyrex.untrack.core.domain.sanitizer.Sanitizer
import com.xboyrex.untrack.core.domain.sanitizer.catalog.AllSanitizers

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun RulesScreen(
    contentPadding: PaddingValues,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Built-in Sanitizers, 1 = Custom & Teach-by-Example
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SanitizerCategory?>(null) }

    // Teach-by-Example state
    var showTeachCard by remember { mutableStateOf(false) }
    var beforeExample by remember { mutableStateOf("") }
    var desiredExample by remember { mutableStateOf("") }

    // Manual Rule Creator state
    var showAddManual by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualHost by remember { mutableStateOf("") }
    var manualParams by remember { mutableStateOf("") }

    // Rule Test Lab state
    var testLabInput by remember { mutableStateOf("") }
    var testLabOutput by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(
            title = "Sanitizers & Rules",
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
            // Segmented Sub-Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to "Built-in Sanitizers (115+)",
                    1 to "Custom Rules (${UntrackStore.customRules.size})"
                ).forEach { (idx, label) ->
                    val active = selectedSubTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) c.primary else Color.Transparent)
                            .pressable { selectedSubTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (active) c.onPrimary else c.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedSubTab == 0) {
                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceContainerHigh)
                        .border(1.dp, c.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = c.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = c.onSurface, fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(c.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search sanitizers (Amazon, YouTube, Instagram, UTM...)", fontSize = 13.sp, color = c.onSurfaceVariant.copy(alpha = 0.55f))
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = c.onSurfaceVariant,
                            modifier = Modifier.size(18.dp).clickable { searchQuery = "" }
                        )
                    }
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allSelected = selectedCategory == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (allSelected) c.primary else c.surfaceContainerHigh)
                            .pressable { selectedCategory = null }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "All Categories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allSelected) c.onPrimary else c.onSurfaceVariant
                        )
                    }
                    SanitizerCategory.entries.forEach { cat ->
                        val active = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) c.primary else c.surfaceContainerHigh)
                                .pressable { selectedCategory = if (active) null else cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                cat.label,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) c.onPrimary else c.onSurfaceVariant
                            )
                        }
                    }
                }

                val filtered = SanitizerCatalog.allSanitizers.filter { spec ->
                    (selectedCategory == null || spec.category == selectedCategory) &&
                        (searchQuery.isBlank() ||
                            spec.name.contains(searchQuery, ignoreCase = true) ||
                            spec.description.contains(searchQuery, ignoreCase = true))
                }

                filtered.forEach { spec ->
                    val enabled = UntrackStore.isSanitizerEnabled(spec.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surfaceContainerHigh)
                            .pressable { UntrackStore.setSanitizerEnabled(spec.id, !enabled) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (enabled) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (enabled) c.primary else c.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = spec.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = c.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (spec.isShortLinkResolver) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(c.secondaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Unshortener",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = c.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = spec.description,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = c.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { UntrackStore.setSanitizerEnabled(spec.id, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                        )
                    }
                }

                val coveredNames = remember {
                    SanitizerCatalog.allSanitizers.map { it.name.lowercase() }.toSet() +
                        SanitizerCatalog.allSanitizers.map { it.id.lowercase() }.toSet()
                }
                val uniqueCoreSanitizers: List<Sanitizer> = remember {
                    AllSanitizers.filter { s: Sanitizer ->
                        s.id.value.lowercase() !in coveredNames &&
                            s.name.lowercase() !in coveredNames &&
                            SanitizerCatalog.allSanitizers.none { spec ->
                                spec.name.contains(s.name, ignoreCase = true) || s.name.contains(spec.name, ignoreCase = true)
                            }
                    }
                }
                val domainSanitizers: List<Sanitizer> = uniqueCoreSanitizers.filter { s: Sanitizer ->
                    (selectedCategory == null || selectedCategory == SanitizerCategory.GLOBAL) &&
                        (searchQuery.isBlank() ||
                            s.name.contains(searchQuery, ignoreCase = true) ||
                            s.id.value.contains(searchQuery, ignoreCase = true))
                }

                for (s: Sanitizer in domainSanitizers) {
                    val enabled = UntrackStore.isSanitizerEnabled(s.id.value)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surfaceContainerHigh)
                            .pressable { UntrackStore.setSanitizerEnabled(s.id.value, !enabled) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (enabled) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (enabled) c.primary else c.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = c.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Catalog Sanitizer ID: ${s.id.value} (${s.rules.size} active rule${if (s.rules.size == 1) "" else "s"})",
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { UntrackStore.setSanitizerEnabled(s.id.value, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                        )
                    }
                }
            } else {
                // Custom Rules & Teach-by-Example Studio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.primary)
                            .pressable {
                                showTeachCard = !showTeachCard
                                showAddManual = false
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoFixHigh, null, tint = c.onPrimary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Teach by Example", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = c.onPrimary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.surfaceContainerHigh)
                            .pressable {
                                showAddManual = !showAddManual
                                showTeachCard = false
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, null, tint = c.onSurface, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Custom Rule", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.onSurface)
                        }
                    }
                }

                // Teach by Example Card
                AnimatedVisibility(visible = showTeachCard) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.surfaceContainerHigh)
                            .border(1.dp, c.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Teach Untrack by Example",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = c.primary
                        )
                        Text(
                            "Paste a dirty URL with tracking parameters and the desired clean URL. Untrack automatically infers the parameter or redirect rule!",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant
                        )
                        RuleTextField(
                            value = beforeExample,
                            onValueChange = { beforeExample = it },
                            placeholder = "Before URL (e.g. https://site.com/page?ref=123&id=5)"
                        )
                        RuleTextField(
                            value = desiredExample,
                            onValueChange = { desiredExample = it },
                            placeholder = "Desired Clean URL (e.g. https://site.com/page?id=5)"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.primary)
                                .pressable {
                                    val inferred = CustomUrlRule.inferFromExample(beforeExample, desiredExample)
                                    inferred.onSuccess { rule ->
                                        UntrackStore.addOrUpdateCustomRule(rule)
                                        beforeExample = ""
                                        desiredExample = ""
                                        showTeachCard = false
                                        Toast.makeText(context, "Rule created: ${rule.name}", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Could not infer rule", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Infer & Save Rule", fontWeight = FontWeight.ExtraBold, color = c.onPrimary, fontSize = 13.5.sp)
                        }
                    }
                }

                // Manual Rule Creator Card
                AnimatedVisibility(visible = showAddManual) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.surfaceContainerHigh)
                            .border(1.dp, c.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Create Custom Parameter Rule",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = c.onSurface
                        )
                        RuleTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            placeholder = "Rule Name (e.g. My News Site Tracker)"
                        )
                        RuleTextField(
                            value = manualHost,
                            onValueChange = { manualHost = it },
                            placeholder = "Host domain (e.g. example.com or * for all sites)"
                        )
                        RuleTextField(
                            value = manualParams,
                            onValueChange = { manualParams = it },
                            placeholder = "Comma-separated params to strip (or * for all params)"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.primary)
                                .pressable {
                                    if (manualName.isBlank()) {
                                        Toast.makeText(context, "Enter a rule name", Toast.LENGTH_SHORT).show()
                                        return@pressable
                                    }
                                    val action = if (manualParams.trim() == "*") {
                                        CustomRuleAction.RemoveAllParams
                                    } else {
                                        val list = manualParams.split(",").map(String::trim).filter(String::isNotEmpty)
                                        if (list.isEmpty()) {
                                            Toast.makeText(context, "Enter parameter names or *", Toast.LENGTH_SHORT).show()
                                            return@pressable
                                        }
                                        CustomRuleAction.RemoveParams(list)
                                    }
                                    val rule = CustomUrlRule(
                                        name = manualName.trim(),
                                        hostPattern = manualHost.trim().ifEmpty { "*" },
                                        phase = CustomRulePhase.POST_CLEAN,
                                        action = action
                                    )
                                    UntrackStore.addOrUpdateCustomRule(rule)
                                    manualName = ""
                                    manualHost = ""
                                    manualParams = ""
                                    showAddManual = false
                                    Toast.makeText(context, "Custom rule saved!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save Custom Rule", fontWeight = FontWeight.ExtraBold, color = c.onPrimary, fontSize = 13.5.sp)
                        }
                    }
                }

                // Existing Custom Rules List
                UntrackStore.customRules.forEach { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (rule.enabled) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Rule, null, tint = if (rule.enabled) c.primary else c.onSurfaceVariant, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.onSurface)
                            Text(
                                "Host: ${rule.hostPattern} • ${rule.action.summary}",
                                fontSize = 11.5.sp,
                                color = c.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = c.error,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(4.dp)
                                .clickable { UntrackStore.deleteCustomRule(rule.id) }
                        )
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { UntrackStore.toggleCustomRule(rule.id, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                        )
                    }
                }

                // Interactive Rule Test Lab Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Science, null, tint = c.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Live Rule Test Lab",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = c.onSurface
                        )
                    }
                    RuleTextField(
                        value = testLabInput,
                        onValueChange = { testLabInput = it },
                        placeholder = "Enter sample URL to test against active rules..."
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surfaceContainerHighest)
                            .pressable {
                                if (testLabInput.isNotBlank()) {
                                    scope.launch {
                                        val res = UntrackPipeline.processSingleUrl(testLabInput)
                                        testLabOutput = "${res.cleanedUrl}\nApplied: ${res.appliedRuleNames.joinToString(", ").ifEmpty { "None" }}"
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Run Pipeline Test", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.primary)
                    }
                    testLabOutput?.let { out ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.surfaceContainerLowest)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = out,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = c.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceContainerLowest)
            .border(1.dp, c.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = c.onSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 12.sp, color = c.onSurfaceVariant.copy(alpha = 0.55f))
                }
                inner()
            }
        )
    }
}
