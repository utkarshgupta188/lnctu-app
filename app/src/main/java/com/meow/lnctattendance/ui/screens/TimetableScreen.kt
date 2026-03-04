package com.meow.lnctattendance.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meow.lnctattendance.data.TimetableData
import com.meow.lnctattendance.data.TimetablePeriod
import com.meow.lnctattendance.ui.theme.*
import java.util.Calendar

private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
private val DAY_SHORT = mapOf(
    "Monday"    to "Mon",
    "Tuesday"   to "Tue",
    "Wednesday" to "Wed",
    "Thursday"  to "Thu",
    "Friday"    to "Fri",
)

// ──────────────────────────────────────────────────────────────────────────────
// Root screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TimetableScreen(data: TimetableData) {
    val todayIdx = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            else               -> 0          // weekend → show Monday
        }
    }

    var selectedDay by remember { mutableIntStateOf(todayIdx) }

    Column(Modifier.fillMaxSize()) {
        // ── Day-picker tab row ────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedDay,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp,
        ) {
            DAYS.forEachIndexed { index, day ->
                val isToday = index == todayIdx
                Tab(
                    selected = selectedDay == index,
                    onClick = { selectedDay = index },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                DAY_SHORT[day] ?: day,
                                fontWeight = if (selectedDay == index) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 13.sp,
                            )
                            // Today dot
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier
                                    .size(if (isToday) 5.dp else 0.dp)
                                    .background(Primary, RoundedCornerShape(50)),
                            )
                        }
                    },
                )
            }
        }

        // ── Content area ──────────────────────────────────────────────────
        AnimatedContent(
            targetState = selectedDay,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "timetable_day",
        ) { dayIdx ->
            val dayName = DAYS.getOrElse(dayIdx) { "Monday" }
            val periods = data.days[dayName] ?: emptyList()
            DaySchedule(dayName = dayName, periods = periods, isToday = dayIdx == todayIdx)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Day schedule list
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DaySchedule(dayName: String, periods: List<TimetablePeriod>, isToday: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isToday) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "📅 Today's Schedule",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                    )
                }
            }
        }

        items(periods, key = { "${dayName}_${it.time}_${it.subject}" }) { period ->
            PeriodCard(period)
        }

        if (periods.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No classes scheduled",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Period card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PeriodCard(period: TimetablePeriod) {
    val subjectUpper = period.subject.uppercase()
    val isLunch    = subjectUpper.contains("LUNCH")
    val isLab      = period.subject.contains("-P", ignoreCase = true) ||
                     period.subject.contains("-Lab", ignoreCase = true)
    val isTutorial = period.subject.endsWith("-T", ignoreCase = true)
    val isProject  = subjectUpper.contains("PROJECT") || subjectUpper.contains("MINOR")

    val (bgColor, accentColor, typeLabel) = when {
        isLunch    -> Triple(Amber.copy(alpha = 0.10f),   Amber,   "🍽  Lunch Break")
        isLab      -> Triple(Green.copy(alpha = 0.10f),   Green,   "🔬 Lab / Practical")
        isTutorial -> Triple(Primary.copy(alpha = 0.10f), Primary, "📝 Tutorial")
        isProject  -> Triple(Orange.copy(alpha = 0.10f),  Orange,  "🛠 Project")
        else       -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary, "")
    }

    // ── Parse time string safely ──────────────────────────────────────────
    // Time format from API: "09:00-09:45" — split on first "-" only, because
    // times themselves never contain a "-", only the separator does.
    val dashIdx   = period.time.indexOf('-')
    val startTime = if (dashIdx >= 0) period.time.substring(0, dashIdx).trim() else period.time.trim()
    val endTime   = if (dashIdx >= 0) period.time.substring(dashIdx + 1).trim() else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Time column (fixed 80 dp — wide enough for "10:30") ───────
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = startTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (endTime.isNotEmpty()) {
                    // Small connector line between start and end
                    Box(
                        Modifier
                            .width(1.5.dp)
                            .height(10.dp)
                            .background(accentColor.copy(alpha = 0.35f), RoundedCornerShape(1.dp))
                            .align(Alignment.CenterHorizontally),
                    )
                    Text(
                        text = endTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // ── Vertical accent bar ───────────────────────────────────────
            Box(
                Modifier
                    .width(3.dp)
                    .height(46.dp)
                    .background(accentColor, RoundedCornerShape(2.dp)),
            )

            Spacer(Modifier.width(14.dp))

            // ── Subject name + type label ─────────────────────────────────
            Column(Modifier.weight(1f)) {
                Text(
                    text = period.subject,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                )
                if (typeLabel.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = typeLabel,
                        fontSize = 11.sp,
                        color = accentColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
