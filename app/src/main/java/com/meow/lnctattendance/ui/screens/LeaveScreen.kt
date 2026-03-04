package com.meow.lnctattendance.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.UiState
import com.meow.lnctattendance.data.LeaveSimulatorData
import com.meow.lnctattendance.data.SubjectSimulation
import com.meow.lnctattendance.data.WeekSimulatorData
import com.meow.lnctattendance.ui.components.*
import com.meow.lnctattendance.ui.theme.*

private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

@Composable
fun LeaveScreen(
    weekState: UiState<WeekSimulatorData>,
    dayState: UiState<LeaveSimulatorData>,
    onSimulateDay: (String) -> Unit,
    onLoadWeek: () -> Unit,
) {
    // 0 = Day Simulator, 1 = Week View
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Day Simulator") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    if (weekState is UiState.Idle) {
                        onLoadWeek()
                    }
                },
                text = { Text("Week View") },
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "tab_anim",
        ) { tab ->
            when (tab) {
                0 -> DaySimulatorContent(dayState, onSimulateDay)
                else -> WeekSimulatorContent(weekState, onLoadWeek)
            }
        }
    }
}

// ──────────────────────────────────────────────
// Single day simulator
// ──────────────────────────────────────────────

@Composable
private fun DaySimulatorContent(
    state: UiState<LeaveSimulatorData>,
    onSimulate: (String) -> Unit,
) {
    var selectedDay by remember { mutableStateOf("Monday") }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Simulate Taking Leave On:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DAYS.take(3).forEach { day ->
                                DayChip(
                                    day = day,
                                    selected = selectedDay == day,
                                    onClick = { selectedDay = day },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DAYS.drop(3).forEach { day ->
                                DayChip(
                                    day = day,
                                    selected = selectedDay == day,
                                    onClick = { selectedDay = day },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (DAYS.drop(3).size < 3) {
                                Spacer(Modifier.weight((3 - DAYS.drop(3).size).toFloat()))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { onSimulate(selectedDay) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Simulate $selectedDay", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            when (state) {
                is UiState.Idle -> {
                    // Do nothing, wait for user to press simulate
                }
                is UiState.Loading -> {
                    item { LoadingScreen("Simulating $selectedDay…") }
                }
                is UiState.Error -> {
                    item { ErrorScreen(state.message) { onSimulate(selectedDay) } }
                }
                is UiState.Success -> {
                    val sim = state.data
                    item { SimulationResultCard(sim) }
                    if (sim.subjectSimulations.isNotEmpty()) {
                        item { SectionHeader("Subject Impact Details") }
                        items(sim.subjectSimulations, key = { it.subject }) { sub ->
                            SubjectImpactCard(sub)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DayChip(day: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                day.take(3),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun SimulationResultCard(data: LeaveSimulatorData) {
    val recColor = recommendationColor(data.recommendation)
    GradientCard(
        colors = listOf(recColor.copy(alpha = 0.75f), recColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(data.recommendation.replace("_", " "), fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
        Spacer(Modifier.height(4.dp))
        Text(data.advice, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ResultStat(
                "Overall Now",
                "${"%.1f".format(data.overallAttendance.current)}%",
                Color.White,
            )
            ResultStat(
                "After Leave",
                "${"%.1f".format(data.overallAttendance.projected)}%",
                Color.White,
            )
            ResultStat(
                "Drop",
                "-${"%.1f".format(data.overallAttendance.drop)}%",
                Color.White,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${data.totalClassesOnDay} class units • ${data.affectedSubjectsCount} subjects affected",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.75f))
    }
}

@Composable
private fun SubjectImpactCard(sub: SubjectSimulation) {
    val impactColor = when (sub.impactLevel.uppercase()) {
        "SEVERE" -> Red
        "HIGH" -> Orange
        else -> Amber
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sub.subject, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${sub.classesOnThisDay} class unit(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (sub.willFallBelow75) {
                    Spacer(Modifier.height(4.dp))
                    Text("⚠️ Will fall below 75%!", fontSize = 11.sp, color = Red, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                RiskBadge(sub.impactLevel)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${"%.1f".format(sub.currentPercentage)}% → ${"%.1f".format(sub.projectedPercentage)}%",
                    fontSize = 12.sp,
                    color = impactColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Week view
// ──────────────────────────────────────────────

@Composable
private fun WeekSimulatorContent(
    state: UiState<WeekSimulatorData>,
    onRefresh: () -> Unit,
) {
    when (state) {
        is UiState.Idle -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onRefresh) { Text("Load Week Simulation") }
            }
        }
        is UiState.Loading -> {
            LoadingScreen("Simulating week…")
        }
        is UiState.Error -> {
            ErrorScreen(state.message, onRefresh)
        }
        is UiState.Success -> {
            val data = state.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    GradientCard(
                        colors = listOf(Primary.copy(alpha = 0.8f), PrimaryDark),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Current Attendance", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
                        Text(
                            "${"%.1f".format(data.currentOverallPercentage)}%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(Modifier.height(12.dp))
                        Text("If you miss the whole week:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            WholeWeekStat("Projected", "${"%.1f".format(data.wholeWeekLeave.projectedOverallPercentage)}%", Color.White)
                            WholeWeekStat("Drop", "-${"%.1f".format(data.wholeWeekLeave.overallDrop)}%", Red.copy(alpha = 0.9f))
                            WholeWeekStat("Absences", "${data.wholeWeekLeave.totalAbsences}", Amber)
                        }
                    }
                }

                item { SectionHeader("Day-by-Day Impact") }

                items(data.weekSimulation, key = { it.day }) { day ->
                    DaySimCard(day.day, recommendation = day.recommendation, advice = day.advice, projectedPct = day.projectedOverallPercentage, drop = day.overallDrop, totalClasses = day.totalClassUnits, topSubs = day.subjectSimulations)
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun WholeWeekStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun DaySimCard(
    day: String,
    recommendation: String,
    advice: String,
    projectedPct: Double,
    drop: Double,
    totalClasses: Int,
    topSubs: List<SubjectSimulation>,
) {
    val recColor = recommendationColor(recommendation)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(day, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$totalClasses class units • $advice",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = recColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            recommendation,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = recColor,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "→ ${"%.1f".format(projectedPct)}% (-${"%.1f".format(drop)}%)",
                        fontSize = 12.sp,
                        color = Red,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Expandable subject impacts
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("Most impacted subjects:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    topSubs.forEach { sub ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(sub.subject, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(
                                "${"%.1f".format(sub.currentPercentage)}% → ${"%.1f".format(sub.projectedPercentage)}%",
                                fontSize = 12.sp,
                                color = Red,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
