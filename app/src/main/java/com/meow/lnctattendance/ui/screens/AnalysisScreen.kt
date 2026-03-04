package com.meow.lnctattendance.ui.screens

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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Summary card ──────────────────────────────────────────────────
        item {
            val statusColor = when (data.summary.overallStatus) {
                "GOOD" -> Green
                "WARNING" -> Amber
                else -> Red
            }
            GradientCard(
                colors = listOf(statusColor.copy(alpha = 0.7f), statusColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            data.summary.overallStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            data.summary.overallMessage,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStat("At Risk", data.summary.atRiskCount.toString(), Red)
                            MiniStat("Safe", data.summary.safeCount.toString(), Green)
                            MiniStat("Total", data.summary.totalSubjects.toString(), Color.White)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    AttendanceCircle(
                        percentage = data.summary.overallPercentage,
                        modifier = Modifier.size(100.dp),
                    )
                }
            }
        }

        // ── Day leave analysis ────────────────────────────────────────────
        item { SectionHeader("Best Days to Take Leave") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DAY_ORDER.forEach { day ->
                    data.dayAnalysis[day]?.let { dayData ->
                        DayLeaveCard(day, dayData)
                    }
                }
            }
        }

        // ── Predictions ───────────────────────────────────────────────────
        item { SectionHeader("Subject Predictions") }
        items(data.predictions, key = { it.subject }) { pred ->
            PredictionCard(pred)
        }

        // ── At risk subjects ──────────────────────────────────────────────
        if (data.atRiskSubjects.isNotEmpty()) {
            item { SectionHeader("⚠️ At-Risk Subjects") }
            items(data.atRiskSubjects, key = { "risk_${it.name}" }) { subject ->
                SubjectCard(subject)
            }
        }

        // ── Safe subjects ─────────────────────────────────────────────────
        if (data.safeSubjects.isNotEmpty()) {
            item { SectionHeader("✅ Safe Subjects") }
            items(data.safeSubjects, key = { "safe_${it.name}" }) { subject ->
                SubjectCard(subject)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun DayLeaveCard(day: String, data: DayAnalysis) {
    val recColor = when (data.leaveRecommendation) {
        "SAFE" -> Green
        "CAUTION" -> Amber
        else -> Red
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(day, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${data.totalClasses} classes • ${data.atRiskCount} at-risk • ${data.safeCount} safe",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = recColor.copy(alpha = 0.15f),
            ) {
                Text(
                    data.leaveRecommendation,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = recColor,
                )
            }
        }
    }
}

@Composable
private fun PredictionCard(pred: SubjectPrediction) {
    val statusColor = when (pred.status) {
        "SAFE" -> Green
        "WARNING" -> Amber
        else -> Red
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .offset(y = 6.dp),
            ) {
                Surface(
                    Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(50),
                    color = statusColor,
                ) {}
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(pred.subject, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    pred.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
