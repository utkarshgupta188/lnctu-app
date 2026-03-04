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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
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
import com.meow.lnctattendance.prefs.PreferencesManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import androidx.glance.appwidget.cornerRadius

class AttendanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendanceWidget()
}

class AttendanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val present = prefs[intPreferencesKey("present")] ?: 0
            val absent = prefs[intPreferencesKey("absent")] ?: 0
            val total = prefs[intPreferencesKey("total")] ?: 0
            val percentage = prefs[doublePreferencesKey("percentage")] ?: 0.0
            val status = prefs[stringPreferencesKey("status")] ?: "Tap refresh to load"

            AttendanceWidgetContent(
                present = present,
                absent = absent,
                total = total,
                percentage = percentage,
                status = status
            )
        }
    }
}

@Composable
fun AttendanceWidgetContent(present: Int, absent: Int, total: Int, percentage: Double, status: String) {
    val color = when {
        percentage >= 75 -> Color(0xFF4CAF50)
        percentage >= 65 -> Color(0xFFFFC107)
        else -> Color(0xFFE53935)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xFF1A1A27))
            .cornerRadius(16.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LNCT Attendance",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            // Custom text refresh button (avoids ugly default button styles)
            Text(
                text = "↻",
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(actionRunCallback<RefreshAttendanceAction>()).padding(start = 8.dp, bottom = 4.dp, top = 4.dp)
            )
        }
        
        Spacer(modifier = GlanceModifier.height(12.dp))
        
        // Large Percentage
        Text(
            text = "${"%.1f".format(percentage)}%",
            style = TextStyle(color = androidx.glance.unit.ColorProvider(color), fontSize = 36.sp, fontWeight = FontWeight.Bold)
        )
        
        Spacer(modifier = GlanceModifier.height(16.dp))
        
        // Stats
        Row(
            modifier = GlanceModifier.fillMaxWidth().background(Color(0xFF22223A)).cornerRadius(12.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactStat("Present", present.toString(), Color(0xFF4CAF50), GlanceModifier.defaultWeight())
            CompactStat("Absent", absent.toString(), Color(0xFFE53935), GlanceModifier.defaultWeight())
            CompactStat("Total", total.toString(), Color(0xFF6C63FF), GlanceModifier.defaultWeight())
        }
        
        if (status.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = status,
                style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Gray), fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun CompactStat(label: String, value: String, color: Color, modifier: GlanceModifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = TextStyle(color = androidx.glance.unit.ColorProvider(color), fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Text(text = label, style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.Gray), fontSize = 11.sp))
    }
}

class RefreshAttendanceAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefsManager = PreferencesManager(context)
        val authState = prefsManager.authState.firstOrNull()
        
        if (authState is com.meow.lnctattendance.prefs.AuthState.Authenticated) {
            val username = authState.login.username
            val password = authState.login.password
            
            try {
                updateAppWidgetState(context, glanceId) { state ->
                    state[stringPreferencesKey("status")] = "Updating..."
                }
                AttendanceWidget().update(context, glanceId)
                
                val data = ApiService.fetchAttendance(username, password)
                
                updateAppWidgetState(context, glanceId) { state ->
                    state[intPreferencesKey("present")] = data.present
                    state[intPreferencesKey("absent")] = data.absent
                    state[intPreferencesKey("total")] = data.totalClasses
                    state[doublePreferencesKey("percentage")] = data.percentage
                    state[stringPreferencesKey("status")] = "Updated just now"
                }
                AttendanceWidget().update(context, glanceId)
            } catch (e: Exception) {
                updateAppWidgetState(context, glanceId) { state ->
                    state[stringPreferencesKey("status")] = "Error updating"
                }
                AttendanceWidget().update(context, glanceId)
            }
        } else {
            updateAppWidgetState(context, glanceId) { state ->
                state[stringPreferencesKey("status")] = "Please login in the app first"
            }
            AttendanceWidget().update(context, glanceId)
        }
    }
}
