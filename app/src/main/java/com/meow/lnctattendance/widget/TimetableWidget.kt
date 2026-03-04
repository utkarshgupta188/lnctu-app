package com.meow.lnctattendance.widget

import android.content.Context
import android.util.Log
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

private const val TAG = "TimetableWidget"
private val KEY_JSON   = stringPreferencesKey("timetable_json")
private val KEY_STATUS = stringPreferencesKey("status")

private suspend fun fetchTodayPeriods(): String = withContext(Dispatchers.IO) {
    val conn = URL("https://lnctu.vercel.app/timetable").openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 20_000
    conn.readTimeout    = 25_000
    conn.setRequestProperty("Accept", "application/json")
    val code = conn.responseCode
    val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
    conn.disconnect()
    if (code !in 200..299) throw Exception("HTTP $code")

    val data = JSONObject(body).getJSONObject("data")
    val todayName = getTodayName()
    val matchedKey = data.keys().asSequence()
        .firstOrNull { it.equals(todayName, ignoreCase = true) }
        ?: return@withContext "[]"

    val arr = data.getJSONArray(matchedKey)
    Log.d(TAG, "Fetched ${arr.length()} periods for $matchedKey")
    arr.toString()
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

class TimetableWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val periodsJson = fetchTodayPeriods()
            updateAppWidgetState(context, id) { state ->
                state[KEY_JSON]   = periodsJson
                state[KEY_STATUS] = "Updated just now"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed: ${e.message}", e)
            updateAppWidgetState(context, id) { state ->
                state[KEY_STATUS] = "Tap ↻ to retry"
            }
        }

        provideContent {
            val prefs  = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val json   = prefs[KEY_JSON]   ?: "[]"
            val status = prefs[KEY_STATUS] ?: "Tap ↻ to load"
            TimetableWidgetContent(json, status)
        }
    }
}

fun getTodayName(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY    -> "Monday"
    Calendar.TUESDAY   -> "Tuesday"
    Calendar.WEDNESDAY -> "Wednesday"
    Calendar.THURSDAY  -> "Thursday"
    Calendar.FRIDAY    -> "Friday"
    Calendar.SATURDAY  -> "Saturday"
    Calendar.SUNDAY    -> "Sunday"
    else               -> "Monday"
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

    // Fixed row height so 7 rows fit comfortably.
    // Row height = vertical padding (4+4) + text (~14sp) ≈ 30dp
    // 7 × 30dp + header ~22dp + spacer 5dp + wrapper padding 16dp = ~210dp
    // Fits within minHeight="220dp" and targetCellHeight="4"
    val rowHeight = 30.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xFF1A1A27))
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // ── Header ─────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today  •  ${getTodayName()}",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "↻",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshTimetableAction>())
                    .padding(start = 8.dp, top = 2.dp, end = 0.dp, bottom = 2.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(5.dp))

        if (periods.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.Gray),
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            // Nested Column — max 7 rows, no Spacers between them.
            // Each row has a fixed height so they all fit without overflowing.
            Column(modifier = GlanceModifier.fillMaxSize()) {
                periods.take(7).forEach { (time, subject) ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .background(Color(0xFF22223A))
                            .cornerRadius(6.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = time.substringBefore("-").trim(),
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.width(38.dp)
                        )
                        Spacer(
                            modifier = GlanceModifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color(0xFF6C63FF))
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = subject,
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color.White),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
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
                state[KEY_STATUS] = "Updating..."
            }
            TimetableWidget().update(context, glanceId)

            val periodsJson = fetchTodayPeriods()
            updateAppWidgetState(context, glanceId) { state ->
                state[KEY_JSON]   = periodsJson
                state[KEY_STATUS] = "Updated just now"
            }
            TimetableWidget().update(context, glanceId)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed: ${e.message}", e)
            updateAppWidgetState(context, glanceId) { state ->
                state[KEY_STATUS] = "Error – tap ↻ to retry"
            }
            TimetableWidget().update(context, glanceId)
        }
    }
}
