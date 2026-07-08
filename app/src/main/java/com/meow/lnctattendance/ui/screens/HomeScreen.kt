package com.meow.lnctattendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    var datewiseExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "At Risk", "Safe"

    val sortedDatewise = remember(data.datewise) { data.datewise.reversed() }
    val displayedDatewise = if (datewiseExpanded) sortedDatewise else sortedDatewise.take(5)

    val filteredSubjects = remember(data.subjects, selectedFilter) {
        when (selectedFilter) {
            "At Risk" -> data.subjects.filter { it.percentage < 75 }
            "Safe"    -> data.subjects.filter { it.percentage >= 75 }
            else      -> data.subjects
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Dashboard Premium Header ────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Initials Circle
                            val initials = (studentName ?: username).take(2).uppercase()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome back,",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = studentName?.takeIf { it.isNotBlank() } ?: username.ifBlank { "Student" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // Overall gauge
                        AttendanceCircle(
                            percentage = data.percentage,
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 7.dp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    val statusText = when {
                        data.percentage >= 75 -> "You are doing great! Keep attending your classes."
                        data.percentage >= 65 -> "Caution: Attendance is close to the threshold limit."
                        else                  -> "Warning: You are below the required 75% limit!"
                    }
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // ── Stat Row ──────────────────────────────────────────────────────
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

        // ── Date-wise Attendance ──────────────────────────────────────────
        if (sortedDatewise.isNotEmpty()) {
            datewiseSection(
                records = sortedDatewise,
                displayed = displayedDatewise,
                expanded = datewiseExpanded,
                onToggleExpanded = { datewiseExpanded = !datewiseExpanded },
            )
        }

        // ── Filter Options Chips ──────────────────────────────────────────
        if (data.subjects.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader("Courses Overview")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "At Risk", "Safe").forEach { tag ->
                            val selected = selectedFilter == tag
                            val bg = if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant
                            val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { selectedFilter = tag }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                            }
                        }
                    }
                }
            }

            // Subject grid/list
            items(filteredSubjects, key = { it.name }) { subject ->
                SubjectCard(subject)
            }
        }

        // ── Quick Refresh Actions ──────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sync Attendance Data", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Date-wise section
// ──────────────────────────────────────────────────────────────────────────────

private fun LazyListScope.datewiseSection(
    records: List<DatewiseRecord>,
    displayed: List<DatewiseRecord>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    item(key = "datewise_header") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "History Logs",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${records.size} sessions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }

    items(displayed, key = { "dw_${it.date}_${it.lecture}_${it.subject}" }) { record ->
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                DatewiseRow(record = record)
            }
        }
    }

    item(key = "datewise_footer") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                if (records.size > 5) {
                    TextButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = if (expanded) "Show Less ▲" else "View Full Log (${records.size}) ▼",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DatewiseRow(record: DatewiseRecord) {
    val statusTrimmed = record.status.trim()
    val isPresent = statusTrimmed.equals("Present", ignoreCase = true)
            || statusTrimmed.equals("P", ignoreCase = true)
    val statusColor = if (isPresent) Green else Red
    val rowBg = if (isPresent) Green.copy(alpha = 0.05f) else Red.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(rowBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(2.5f)) {
            Text(
                text = record.date,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Lec: ${record.lecture}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = record.subject,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(3.5f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isPresent) "P" else "A",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
            )
        }
    }
}

@Composable
fun SubjectCard(subject: Subject) {
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
                    text = subject.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(
                    if (subject.percentage >= 75) "SAFE"
                    else if (subject.percentage >= 65) "HIGH"
                    else "CRITICAL"
                )
            }
            Spacer(Modifier.height(12.dp))
            AttendanceBar(subject.percentage)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Attended: ${subject.present}", fontSize = 11.sp, color = Green, fontWeight = FontWeight.Bold)
                    Text("Missed: ${subject.absent}", fontSize = 11.sp, color = Red, fontWeight = FontWeight.Bold)
                }
                Text("Total sessions: ${subject.total}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}
