package com.meow.lnctattendance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.meow.lnctattendance.data.ApiService
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.stringPreferencesKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

class TimetableWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val timetableJson = prefs[stringPreferencesKey("timetable_json")] ?: "[]"
            val status = prefs[stringPreferencesKey("status")] ?: "Tap refresh to load"

            TimetableWidgetContent(timetableJson, status)
        }
    }
}

@Composable
fun TimetableWidgetContent(timetableJson: String, status: String) {
    val periods = try {
        val arr = JSONArray(timetableJson)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            obj.getString("time") to obj.getString("subject")
        }
    } catch (e: Exception) {
        emptyList()
    }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val todayIdx = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        else -> 0
    }
    val todayName = days[todayIdx]

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xFF1A1A27))
            .cornerRadius(16.dp)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today ($todayName)",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            // Custom text refresh button
            Text(
                text = "↻",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshTimetableAction>()).padding(start = 8.dp, bottom = 4.dp, top = 4.dp)
            )
        }
        
        Spacer(modifier = GlanceModifier.height(12.dp))

        if (periods.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = status,
                    style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Gray), fontSize = 12.sp)
                )
            }
        } else {
            periods.forEach { (time, subject) ->
                // Clean the time, e.g. "09:00-09:45" -> "09:00"
                val startTime = time.split("-").firstOrNull()?.trim() ?: time
                
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color(0xFF22223A))
                        .cornerRadius(8.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = startTime,
                        style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.width(44.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    Text(
                        text = subject,
                        style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

class RefreshTimetableAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            updateAppWidgetState(context, glanceId) { state ->
                state[stringPreferencesKey("status")] = "Updating..."
            }
            TimetableWidget().update(context, glanceId)
            
            val data = ApiService.fetchTimetable()
            
            val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
            val todayIdx = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                else -> 0
            }
            val todayName = days[todayIdx]
            
            val todayPeriods = data.days[todayName] ?: emptyList()
            
            val jsonArray = JSONArray()
            todayPeriods.forEach { p ->
                val obj = JSONObject()
                obj.put("time", p.time)
                obj.put("subject", p.subject)
                jsonArray.put(obj)
            }
            
            updateAppWidgetState(context, glanceId) { state ->
                state[stringPreferencesKey("timetable_json")] = jsonArray.toString()
                state[stringPreferencesKey("status")] = "Updated just now"
            }
            TimetableWidget().update(context, glanceId)
        } catch (e: Exception) {
            updateAppWidgetState(context, glanceId) { state ->
                state[stringPreferencesKey("status")] = "Error updating"
            }
            TimetableWidget().update(context, glanceId)
        }
    }
}
