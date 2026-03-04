package com.meow.lnctattendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.data.AttendanceData
import com.meow.lnctattendance.data.Subject
import com.meow.lnctattendance.ui.components.*
import com.meow.lnctattendance.ui.theme.*

@Composable
fun HomeScreen(
    data: AttendanceData,
    username: String,
    studentName: String? = null,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Overall card ─────────────────────────────────────────────────
        item {
            GradientCard(
                colors = listOf(
                    Primary.copy(alpha = 0.85f),
                    PrimaryDark,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            studentName?.takeIf { it.isNotBlank() } ?: username.ifBlank { "Student" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "LNCT University",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Spacer(Modifier.height(16.dp))
                        val statusText = when {
                            data.percentage >= 75 -> "✅ You're on track!"
                            data.percentage >= 65 -> "⚠️ Getting close to the limit"
                            else -> "🚨 Below safe attendance!"
                        }
                        Text(statusText, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                    Spacer(Modifier.width(16.dp))
                    AttendanceCircle(
                        percentage = data.percentage,
                        modifier = Modifier.size(110.dp),
                    )
                }
            }
        }

        // ── Stat row ─────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatChip("Present", data.present.toString(), Green, Modifier.weight(1f))
                StatChip("Absent", data.absent.toString(), Red, Modifier.weight(1f))
                StatChip("Total", data.totalClasses.toString(), Primary, Modifier.weight(1f))
            }
        }

        // ── Subjects header ───────────────────────────────────────────────
        if (data.subjects.isNotEmpty()) {
            item { SectionHeader("Subject-wise Attendance") }

            items(data.subjects, key = { it.name }) { subject ->
                SubjectCard(subject)
            }
        }

        // ── Refresh button ────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh Attendance", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SubjectCard(subject: Subject) {
    val color = attendanceColor(subject.percentage)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    subject.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(if (subject.percentage >= 75) "SAFE" else if (subject.percentage >= 65) "HIGH" else "CRITICAL")
            }
            Spacer(Modifier.height(10.dp))
            AttendanceBar(subject.percentage)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("P: ${subject.present}", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Medium)
                Text("A: ${subject.absent}", fontSize = 12.sp, color = Red, fontWeight = FontWeight.Medium)
                Text("T: ${subject.total}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
