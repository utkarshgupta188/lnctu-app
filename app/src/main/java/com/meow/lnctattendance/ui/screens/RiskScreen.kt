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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Overall risk card ─────────────────────────────────────────────
        item {
            val riskColor = when (data.overallRiskStatus) {
                "SAFE" -> Green
                "WARNING" -> Amber
                else -> Red
            }
            GradientCard(
                colors = listOf(riskColor.copy(alpha = 0.7f), riskColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Overall Risk",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    data.overallRiskStatus,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MiniInfoItem("Threshold", "${"%.0f".format(data.threshold)}%", Color.White)
                    MiniInfoItem("At Risk", data.atRiskSubjectsCount.toString(), Color.White)
                    if (data.criticalAlert) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f),
                        ) {
                            Text(
                                "🚨 CRITICAL ALERT",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Subject Risk Analysis") }

        items(data.subjectRisks, key = { it.subject }) { risk ->
            SubjectRiskCard(risk)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MiniInfoItem(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Subject name + badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    risk.subject,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(risk.riskLevel)
            }

            Spacer(Modifier.height(10.dp))
            AttendanceBar(risk.percentage)
            Spacer(Modifier.height(10.dp))

            // Stats row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RiskInfoChip("P: ${risk.present}", Green, Modifier.weight(1f))
                RiskInfoChip("A: ${risk.absent}", Red, Modifier.weight(1f))
                RiskInfoChip("T: ${risk.total}", Primary, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(10.dp))

            if (risk.alreadyBelowThreshold) {
                // Recovery info
                Row(
                    Modifier.fillMaxWidth().background(Red.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("⚠️ Below threshold!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Red)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Need ${risk.consecutivePresentsNeeded} consecutive classes (≈${risk.estimatedDaysToRecover} days) to recover",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Safe — show how many can miss
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    InfoLabel("Can skip", "${risk.absentsAllowedBeforeThreshold} more", riskColor)
                    InfoLabel("If miss 1", "${"%.1f".format(risk.projectedPercentageIfMissOne)}%", Amber)
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
        color = color.copy(alpha = 0.1f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun InfoLabel(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
    }
}
