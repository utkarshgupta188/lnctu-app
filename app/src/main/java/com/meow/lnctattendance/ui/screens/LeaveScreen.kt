package com.meow.lnctattendance.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Day Simulator", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    if (weekState is UiState.Idle) {
                        onLoadWeek()
                    }
                },
                text = { Text("Whole Week View", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_anim",
        ) { tab ->
            when (tab) {
                0 -> DaySimulatorContent(dayState, onSimulateDay)
                else -> WeekSimulatorContent(weekState, onLoadWeek)
            }
        }
    }
}

@Composable
private fun DaySimulatorContent(
    state: UiState<LeaveSimulatorData>,
    onSimulate: (String) -> Unit,
) {
    var selectedDay by remember { mutableStateOf("Monday") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Leave Simulator",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Predict the impact of missing a specific day on your overall attendance metrics.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DAYS.forEach { day ->
                            val selected = selectedDay == day
                            val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .clickable { selectedDay = day }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.take(3),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fg
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { onSimulate(selectedDay) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Run Simulation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        when (state) {
            is UiState.Idle -> {}
            is UiState.Loading -> {
                item { LoadingScreen("Simulating $selectedDay...") }
            }
            is UiState.Error -> {
                item { ErrorScreen(state.message) { onSimulate(selectedDay) } }
            }
            is UiState.Success -> {
                val sim = state.data
                item { SimulationResultCard(sim) }
                if (sim.subjectSimulations.isNotEmpty()) {
                    item { SectionHeader("Class-level Impact Details") }
                    items(sim.subjectSimulations, key = { it.subject }) { sub ->
                        SubjectImpactCard(sub)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SimulationResultCard(data: LeaveSimulatorData) {
    val recColor = recommendationColor(data.recommendation)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, recColor.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "SIMULATION RESULT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = recColor,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(data.advice, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultStat("Overall Current", "${"%.1f".format(data.overallAttendance.current)}%", MaterialTheme.colorScheme.onSurface)
                ResultStat("Projected", "${"%.1f".format(data.overallAttendance.projected)}%", recColor)
                ResultStat("Expected Drop", "-${"%.1f".format(data.overallAttendance.drop)}%", Red)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "${data.totalClassesOnDay} scheduled class units • ${data.affectedSubjectsCount} subjects affected",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectImpactCard(sub: SubjectSimulation) {
    val impactColor = when (sub.impactLevel.uppercase()) {
        "SEVERE", "HIGH" -> Red
        else -> Amber
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = sub.subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${sub.classesOnThisDay} class period(s) on day",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (sub.willFallBelow75) {
                    Spacer(Modifier.height(4.dp))
                    Text("⚠️ Will breach 75% limit!", fontSize = 11.sp, color = Red, fontWeight = FontWeight.ExtraBold)
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
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun WeekSimulatorContent(
    state: UiState<WeekSimulatorData>,
    onRefresh: () -> Unit,
) {
    when (state) {
        is UiState.Idle -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onRefresh, shape = RoundedCornerShape(14.dp)) {
                    Text("Compute Week Impact")
                }
            }
        }
        is UiState.Loading -> {
            LoadingScreen("Running weekly forecasts...")
        }
        is UiState.Error -> {
            ErrorScreen(state.message, onRefresh)
        }
        is UiState.Success -> {
            val data = state.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "WHOLE WEEK SUMMARY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Current Overall: ${"%.1f".format(data.currentOverallPercentage)}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "If you skip the entire week:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                WholeWeekStat("Projected Pct", "${"%.1f".format(data.wholeWeekLeave.projectedOverallPercentage)}%", MaterialTheme.colorScheme.onSurface)
                                WholeWeekStat("Total Drop", "-${"%.1f".format(data.wholeWeekLeave.overallDrop)}%", Red)
                                WholeWeekStat("Total Absences", "${data.wholeWeekLeave.totalAbsences}", Amber)
                            }
                        }
                    }
                }

                item { SectionHeader("Day-by-Day Forecast Timeline") }

                items(data.weekSimulation, key = { it.day }) { day ->
                    DaySimCard(
                        day = day.day,
                        recommendation = day.recommendation,
                        advice = day.advice,
                        projectedPct = day.projectedOverallPercentage,
                        drop = day.overallDrop,
                        totalClasses = day.totalClassUnits,
                        topSubs = day.subjectSimulations
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun WholeWeekStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(day, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$totalClasses periods • $advice",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = recColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, recColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            recommendation,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = recColor,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "→ ${"%.1f".format(projectedPct)}% (-${"%.1f".format(drop)}%)",
                        fontSize = 11.sp,
                        color = Red,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(Modifier.height(10.dp))
                    Text("Subject level breakdowns:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    topSubs.forEach { sub ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(sub.subject, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${"%.1f".format(sub.currentPercentage)}% → ${"%.1f".format(sub.projectedPercentage)}%",
                                fontSize = 12.sp,
                                color = Red,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
