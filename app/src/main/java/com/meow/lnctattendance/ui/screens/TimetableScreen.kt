package com.meow.lnctattendance.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

@Composable
fun TimetableScreen(data: TimetableData) {
    val todayIdx = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            else               -> 0
        }
    }

    var selectedDay by remember { mutableIntStateOf(todayIdx) }

    Column(Modifier.fillMaxSize()) {
        // Day Picker Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DAYS.forEachIndexed { index, day ->
                val selected = selectedDay == index
                val isToday = index == todayIdx
                val bg = if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                val border = if (isToday && !selected) androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f)) else null

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable { selectedDay = index }
                        .then(if (border != null) Modifier.background(Color.Transparent).then(Modifier.background(bg)) else Modifier)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DAY_SHORT[day] ?: day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                        if (isToday) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White else Primary)
                            )
                        }
                    }
                }
            }
        }

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

@Composable
private fun DaySchedule(dayName: String, periods: List<TimetablePeriod>, isToday: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isToday) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Active Schedule — Today",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No classes scheduled for today",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PeriodCard(period: TimetablePeriod) {
    val subjectUpper = period.subject.uppercase()
    val isLunch    = subjectUpper.contains("LUNCH")
    val isLab      = period.subject.contains("-P", ignoreCase = true) ||
                     period.subject.contains("-Lab", ignoreCase = true)
    val isTutorial = period.subject.endsWith("-T", ignoreCase = true)
    val isProject  = subjectUpper.contains("PROJECT") || subjectUpper.contains("MINOR")

    val (bgColor, accentColor, typeLabel) = when {
        isLunch    -> Triple(Amber.copy(alpha = 0.06f), Amber, "Lunch Break")
        isLab      -> Triple(Green.copy(alpha = 0.06f), Green, "Lab / Practical")
        isTutorial -> Triple(Primary.copy(alpha = 0.06f), Primary, "Tutorial Session")
        isProject  -> Triple(Orange.copy(alpha = 0.06f), Orange, "Project Class")
        else       -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.primary, "Lecture")
    }

    val dashIdx   = period.time.indexOf('-')
    val startTime = if (dashIdx >= 0) period.time.substring(0, dashIdx).trim() else period.time.trim()
    val endTime   = if (dashIdx >= 0) period.time.substring(dashIdx + 1).trim() else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(70.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = startTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                )
                if (endTime.isNotEmpty()) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(6.dp)
                            .background(accentColor.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally),
                    )
                    Text(
                        text = endTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Box(
                Modifier
                    .width(2.dp)
                    .height(36.dp)
                    .background(accentColor, RoundedCornerShape(1.dp)),
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = period.subject,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (typeLabel.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = typeLabel,
                        fontSize = 10.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
