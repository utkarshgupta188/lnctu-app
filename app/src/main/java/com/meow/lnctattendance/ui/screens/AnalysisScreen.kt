package com.meow.lnctattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.data.AnalysisData
import com.meow.lnctattendance.data.DayAnalysis
import com.meow.lnctattendance.data.SubjectPrediction
import com.meow.lnctattendance.ui.components.*
import com.meow.lnctattendance.ui.theme.*

private val DAY_ORDER = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

@Composable
fun AnalysisScreen(data: AnalysisData, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Summary Card ──────────────────────────────────────────────────
        item {
            val statusColor = when (data.summary.overallStatus.uppercase()) {
                "GOOD" -> Green
                "WARNING" -> Amber
                else -> Red
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "ANALYSIS STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                data.summary.overallStatus,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                data.summary.overallMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        AttendanceCircle(
                            percentage = data.summary.overallPercentage,
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 7.dp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStat("At Risk", data.summary.atRiskCount.toString(), Red)
                        MiniStat("Safe", data.summary.safeCount.toString(), Green)
                        MiniStat("Total Courses", data.summary.totalSubjects.toString(), MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Day Leave Analysis ────────────────────────────────────────────
        item { SectionHeader("Leave Recommendation by Day") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    DAY_ORDER.forEachIndexed { index, day ->
                        data.dayAnalysis[day]?.let { dayData ->
                            DayLeaveCard(day, dayData)
                            if (index < DAY_ORDER.size - 1) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // ── Predictions ───────────────────────────────────────────────────
        item { SectionHeader("Subject Predictions & Forecast") }
        items(data.predictions, key = { it.subject }) { pred ->
            PredictionCard(pred)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayLeaveCard(day: String, data: DayAnalysis) {
    val recColor = when (data.leaveRecommendation.uppercase()) {
        "SAFE" -> Green
        "CAUTION" -> Amber
        else -> Red
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(day, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "${data.totalClasses} classes • ${data.atRiskCount} risk • ${data.safeCount} safe",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = recColor.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, recColor.copy(alpha = 0.25f))
        ) {
            Text(
                data.leaveRecommendation,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = recColor,
            )
        }
    }
}

@Composable
private fun PredictionCard(pred: SubjectPrediction) {
    val statusColor = when (pred.status.uppercase()) {
        "SAFE" -> Green
        "WARNING" -> Amber
        else -> Red
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(statusColor, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(pred.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    pred.message,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
