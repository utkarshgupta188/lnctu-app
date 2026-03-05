package com.meow.lnctattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.data.AttendanceData
import com.meow.lnctattendance.data.DatewiseRecord
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
    // Track expanded state here so LazyColumn scope extensions can read it
    var datewiseExpanded by remember { mutableStateOf(false) }

    // Reverse once — latest date at the top
    val sortedDatewise = remember(data.datewise) { data.datewise.reversed() }
    val displayedDatewise = if (datewiseExpanded) sortedDatewise else sortedDatewise.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Overall card ──────────────────────────────────────────────────
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
                            else                  -> "🚨 Below safe attendance!"
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

        // ── Stat row ──────────────────────────────────────────────────────
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatChip("Present", data.present.toString(), Green,   Modifier.weight(1f))
                StatChip("Absent",  data.absent.toString(),  Red,     Modifier.weight(1f))
                StatChip("Total",   data.totalClasses.toString(), Primary, Modifier.weight(1f))
            }
        }

        // ── Date-wise Attendance ──────────────────────────────────────────
        if (sortedDatewise.isNotEmpty()) {
            datewiseSection(
                records          = sortedDatewise,
                displayed        = displayedDatewise,
                expanded         = datewiseExpanded,
                onToggleExpanded = { datewiseExpanded = !datewiseExpanded },
            )
        }

        // ── Subjects header + list ────────────────────────────────────────
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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

// ──────────────────────────────────────────────────────────────────────────────
// Date-wise section — rendered directly into LazyListScope so every row is
// its own lazy item (no nested Column with unbounded height).
// ──────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.datewiseSection(
    records: List<DatewiseRecord>,
    displayed: List<DatewiseRecord>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    // ── Card header ───────────────────────────────────────────────────────
    item(key = "datewise_header") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color    = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                // Title row
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text       = "Date-wise Attendance",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text     = "${records.size} records",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Column labels
                Row(Modifier.fillMaxWidth()) {
                    Text("Date",    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2.2f), fontWeight = FontWeight.SemiBold)
                    Text("Lecture", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold)
                    Text("Subject", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(3f),   fontWeight = FontWeight.SemiBold)
                    Text("Status",  fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }

    // ── Each record row ───────────────────────────────────────────────────
    items(displayed, key = { "dw_${it.date}_${it.lecture}_${it.subject}" }) { record ->
        Surface(
            color    = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                DatewiseRow(record = record)
            }
        }
    }

    // ── Card footer — show more / less button ────────────────────────────
    item(key = "datewise_footer") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            color    = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (records.size > 10) {
                    TextButton(
                        onClick  = onToggleExpanded,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text       = if (expanded) "Show Less ▲" else "Show All ${records.size} ▼",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Primary,
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Single date-wise row
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DatewiseRow(record: DatewiseRecord) {
    val statusTrimmed = record.status.trim()
    val isPresent = statusTrimmed.equals("Present", ignoreCase = true)
            || statusTrimmed.equals("P", ignoreCase = true)
    val statusColor = if (isPresent) Green else Red
    val rowBg       = if (isPresent) Green.copy(alpha = 0.06f) else Red.copy(alpha = 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = record.date,
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text     = record.lecture,
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text     = record.subject,
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(3f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier          = Modifier.weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text       = if (isPresent) "P" else "A",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = statusColor,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Subject card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SubjectCard(subject: Subject) {
    val color = attendanceColor(subject.percentage)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    subject.name,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(
                    if (subject.percentage >= 75) "SAFE"
                    else if (subject.percentage >= 65) "HIGH"
                    else "CRITICAL"
                )
            }
            Spacer(Modifier.height(10.dp))
            AttendanceBar(subject.percentage)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("P: ${subject.present}", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Medium)
                Text("A: ${subject.absent}",  fontSize = 12.sp, color = Red,   fontWeight = FontWeight.Medium)
                Text("T: ${subject.total}",   fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
