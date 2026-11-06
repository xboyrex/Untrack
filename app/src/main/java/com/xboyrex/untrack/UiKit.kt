package com.xboyrex.untrack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LobedShape(private val lobes: Int, private val depth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val steps = 120
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * PI.toFloat()
            val r = 1f - depth + depth * cos(lobes * t)
            val x = cx + cx * r * cos(t)
            val y = cy + cy * r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

val CookieShape = LobedShape(lobes = 9, depth = 0.075f)
val SunnyShape = LobedShape(lobes = 8, depth = 0.05f)

private val ScreenHeaderHorizontal = 16.dp

@Composable
fun ScreenHeader(
    title: String,
    brandMark: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = ScreenHeaderHorizontal, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (brandMark) {
            BrandMark()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontFamily = RoundedHeavy,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            color = c.onSurface,
            maxLines = 1,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun BackScreenHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = ScreenHeaderHorizontal, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(10.dp))
        content()
    }
}

@Composable
fun BrandMark() {
    Image(
        painter = painterResource(R.drawable.ic_brand),
        contentDescription = null,
        modifier = Modifier.size(26.dp),
    )
}

@Composable
fun CircleAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(38.dp)
            .pressable(onClick = onClick)
            .clip(CircleShape)
            .background(c.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = c.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(SunnyShape)
                .background(c.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.onPrimaryContainer, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = c.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, icon: ImageVector? = null) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .pressable(onClick = onClick)
            .height(44.dp)
            .clip(CircleShape)
            .background(c.primary)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = c.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun BottomGestureGuard(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsBottomHeight(WindowInsets.mandatorySystemGestures)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    )
}
