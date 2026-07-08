package com.meow.lnctattendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.ui.theme.*

// ──────────────────────────────────────────────
// Loading Screen
// ──────────────────────────────────────────────

@Composable
fun LoadingScreen(message: String = "Loading…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Error Screen
// ──────────────────────────────────────────────

@Composable
fun ErrorScreen(message: String, onRetry: (() -> Unit)? = null) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Red,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) {
                    Text("Retry", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Modern Dashboard Attendance Circle
// ──────────────────────────────────────────────

@Composable
fun AttendanceCircle(
    percentage: Double,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
) {
    val color = attendanceColor(percentage)
    val animatedPct by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(1200, easing = EaseOutQuubic),
        label = "pct_arc",
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { (animatedPct / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.matchParentSize(),
            strokeWidth = strokeWidth,
            color = color,
            trackColor = color.copy(alpha = 0.12f),
            strokeCap = StrokeCap.Round,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${"%.1f".format(percentage)}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp,
            )
            Text(
                text = "OVERALL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 1.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Small stat chip
// ──────────────────────────────────────────────

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ──────────────────────────────────────────────
// Risk badge
// ──────────────────────────────────────────────

@Composable
fun RiskBadge(level: String) {
    val (bg, fg, label) = when (level.uppercase()) {
        "CRITICAL" -> Triple(Red.copy(alpha = 0.12f), Red, "CRITICAL")
        "HIGH"     -> Triple(Orange.copy(alpha = 0.12f), Orange, "HIGH")
        "SEVERE"   -> Triple(Red.copy(alpha = 0.12f), Red, "SEVERE")
        "SAFE", "LOW" -> Triple(Green.copy(alpha = 0.12f), Green, "SAFE")
        else       -> Triple(Amber.copy(alpha = 0.12f), Amber, level)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
        modifier = Modifier.wrapContentSize(),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

// ──────────────────────────────────────────────
// Gradient card helper
// ──────────────────────────────────────────────

@Composable
fun GradientCard(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(
                    colors = colors
                )
            ),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

// ──────────────────────────────────────────────
// Section header
// ──────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

// ──────────────────────────────────────────────
// Pill progress bar for percentage
// ──────────────────────────────────────────────

@Composable
fun AttendanceBar(percentage: Double, modifier: Modifier = Modifier) {
    val color = attendanceColor(percentage)
    val animatedWidth by animateFloatAsState(
        targetValue = (percentage / 100f).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = EaseOutQuubic),
        label = "bar",
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Attendance Strength",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${"%.1f".format(percentage)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

// ──────────────────────────────────────────────
// EaseOutCubic Spec replacement helper
// ──────────────────────────────────────────────
val EaseOutQuubic = Easing { fraction ->
    val f = fraction - 1f
    f * f * f + 1f
}

// ──────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────

fun attendanceColor(pct: Double): Color = when {
    pct >= 75 -> Green
    pct >= 65 -> Amber
    else      -> Red
}

fun recommendationColor(rec: String): Color = when (rec.uppercase()) {
    "SAFE"                              -> Green
    "CAUTION", "YOU MAY CONSIDER"      -> Amber
    "RISKY", "NOT_RECOMMENDED"         -> Orange
    "AVOID", "STRONGLY_DISCOURAGED"    -> Red
    else                               -> Amber
}
