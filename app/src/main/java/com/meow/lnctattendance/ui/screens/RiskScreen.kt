package com.meow.lnctattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.data.RiskEngineData
import com.meow.lnctattendance.data.SubjectRisk
import com.meow.lnctattendance.ui.components.*
import com.meow.lnctattendance.ui.theme.*

@Composable
fun RiskScreen(data: RiskEngineData, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Overall Risk Card ─────────────────────────────────────────────
        item {
            val riskColor = when (data.overallRiskStatus.uppercase()) {
                "SAFE" -> Green
                "WARNING" -> Amber
                else -> Red
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.25f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Risk Engine Status",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        data.overallRiskStatus,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniInfoItem("Target Threshold", "${"%.0f".format(data.threshold)}%", MaterialTheme.colorScheme.primary)
                        MiniInfoItem("At-Risk Courses", data.atRiskSubjectsCount.toString(), Red)
                        if (data.criticalAlert) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Red.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    "CRITICAL ALERT",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Red,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Course Vulnerabilities") }

        items(data.subjectRisks, key = { it.subject }) { risk ->
            SubjectRiskCard(risk)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MiniInfoItem(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectRiskCard(risk: SubjectRisk) {
    val riskColor = when (risk.riskLevel.uppercase()) {
        "CRITICAL" -> Red
        "HIGH" -> Orange
        else -> Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    risk.subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(risk.riskLevel)
            }

            Spacer(Modifier.height(12.dp))
            AttendanceBar(risk.percentage)
            Spacer(Modifier.height(12.dp))

            // Attendance details
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RiskInfoChip("Present: ${risk.present}", Green, Modifier.weight(1f))
                RiskInfoChip("Absent: ${risk.absent}", Red, Modifier.weight(1f))
                RiskInfoChip("Total: ${risk.total}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }

            if (risk.alreadyBelowThreshold) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Red.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Recovery Action Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Red)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Attend ${risk.consecutivePresentsNeeded} consecutive classes (approx. ${risk.estimatedDaysToRecover} days) to climb back to ${"%.0f".format(75.0)}%.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    InfoLabel("Safe to Skip", "${risk.absentsAllowedBeforeThreshold} periods", riskColor)
                    InfoLabel("Projected on Next Miss", "${"%.1f".format(risk.projectedPercentageIfMissOne)}%", Amber)
                }
            }
        }
    }
}

@Composable
private fun RiskInfoChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun InfoLabel(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color)
    }
}
