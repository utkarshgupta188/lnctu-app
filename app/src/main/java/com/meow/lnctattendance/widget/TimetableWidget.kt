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
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.GlanceTheme
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
import androidx.glance.material3.ColorProviders
import com.meow.lnctattendance.prefs.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import com.meow.lnctattendance.ui.theme.*

private const val TAG = "TimetableWidget"
private val KEY_JSON   = stringPreferencesKey("timetable_json")
private val KEY_STATUS = stringPreferencesKey("status")

private val LightWidgetColorScheme = androidx.compose.material3.lightColorScheme(
    primary              = Primary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFF5DDBE),
    onPrimaryContainer   = Color(0xFF2C1A0E),
    secondary            = Secondary,
    onSecondary          = Color.White,
    background           = LightBackground,
    surface              = LightSurface,
    surfaceVariant       = LightCard,
    onBackground         = Color(0xFF211B15),
    onSurface            = Color(0xFF211B15),
    onSurfaceVariant     = Color(0xFF534639),
    outline              = Color(0xFF857463),
)

private val DarkWidgetColorScheme = androidx.compose.material3.darkColorScheme(
    primary              = Color(0xFFE3BD9A),
    onPrimary            = Color(0xFF4A2B14),
    primaryContainer     = Color(0xFF6F4E37),
    onPrimaryContainer   = Color(0xFFFBEFE3),
    secondary            = Color(0xFFD6B599),
    onSecondary          = Color(0xFF4A2B14),
    background           = DarkBackground,
    surface              = DarkSurface,
    surfaceVariant       = DarkCard,
    onBackground         = Color(0xFFEDE0D4),
    onSurface            = Color(0xFFEDE0D4),
    onSurfaceVariant     = Color(0xFFCFBFB0),
    outline              = Color(0xFF9C8A7B),
)

private val WidgetColors = ColorProviders(
    light = LightWidgetColorScheme,
    dark = DarkWidgetColorScheme
)

private val TranslucentSurface = androidx.glance.color.ColorProvider(
    day = Color.Black.copy(alpha = 0.06f),
    night = Color.White.copy(alpha = 0.12f)
)
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

private fun getWidgetColorScheme(theme: String, isDark: Boolean): androidx.compose.material3.ColorScheme {
    return if (isDark) {
        when (theme) {
            "sage_green" -> androidx.compose.material3.darkColorScheme(
                primary = Color(0xFF81C784),
                primaryContainer = Color(0xFF1B5E20),
                onPrimaryContainer = Color(0xFFE8F5E9),
                background = Color(0xFF121B13),
                surface = Color(0xFF1E281F),
                surfaceVariant = Color(0xFF2D3B2F),
                onBackground = Color(0xFFE8F5E9),
                onSurface = Color(0xFFE8F5E9),
                onSurfaceVariant = Color(0xFFA5D6A7)
            )
            "purple_aurora" -> androidx.compose.material3.darkColorScheme(
                primary = Color(0xFFCE93D8),
                primaryContainer = Color(0xFF4A148C),
                onPrimaryContainer = Color(0xFFF3E5F5),
                background = Color(0xFF1C1221),
                surface = Color(0xFF2A1B30),
                surfaceVariant = Color(0xFF382540),
                onBackground = Color(0xFFF3E5F5),
                onSurface = Color(0xFFF3E5F5),
                onSurfaceVariant = Color(0xFFE1BEE7)
            )
            "gold_mustard" -> androidx.compose.material3.darkColorScheme(
                primary = Color(0xFFFFD54F),
                primaryContainer = Color(0xFFF57F17),
                onPrimaryContainer = Color(0xFFFFFDE7),
                background = Color(0xFF1D1B12),
                surface = Color(0xFF2A281E),
                surfaceVariant = Color(0xFF3B382B),
                onBackground = Color(0xFFFFFDE7),
                onSurface = Color(0xFFFFFDE7),
                onSurfaceVariant = Color(0xFFFFE082)
            )
            else -> DarkWidgetColorScheme
        }
    } else {
        when (theme) {
            "sage_green" -> androidx.compose.material3.lightColorScheme(
                primary = Color(0xFF2E6B40),
                primaryContainer = Color(0xFFC2F0C2),
                onPrimaryContainer = Color(0xFF1B5E20),
                background = Color(0xFFF0FDF4),
                surface = Color(0xFFE8F8EE),
                surfaceVariant = Color(0xFFD0ECD8),
                onBackground = Color(0xFF1B5E20),
                onSurface = Color(0xFF1B5E20),
                onSurfaceVariant = Color(0xFF2E6B40)
            )
            "purple_aurora" -> androidx.compose.material3.lightColorScheme(
                primary = Color(0xFF6A1B9A),
                primaryContainer = Color(0xFFE8D0FF),
                onPrimaryContainer = Color(0xFF4A148C),
                background = Color(0xFFFAF5FF),
                surface = Color(0xFFF3E8FF),
                surfaceVariant = Color(0xFFE0C3FC),
                onBackground = Color(0xFF4A148C),
                onSurface = Color(0xFF4A148C),
                onSurfaceVariant = Color(0xFF6A1B9A)
            )
            "gold_mustard" -> androidx.compose.material3.lightColorScheme(
                primary = Color(0xFFB58900),
                primaryContainer = Color(0xFFFFF0C2),
                onPrimaryContainer = Color(0xFFF57F17),
                background = Color(0xFFFFFDF5),
                surface = Color(0xFFFFF9E6),
                surfaceVariant = Color(0xFFFFF1CC),
                onBackground = Color(0xFFF57F17),
                onSurface = Color(0xFFF57F17),
                onSurfaceVariant = Color(0xFFB58900)
            )
            else -> LightWidgetColorScheme
        }
    }
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

class TimetableWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val periodsJson = fetchTodayPeriods()
            // Save to offline cache
            com.meow.lnctattendance.database.OfflineCacheHelper(context).saveTimetable(periodsJson)

            updateAppWidgetState(context, id) { state ->
                state[KEY_JSON]   = periodsJson
                state[KEY_STATUS] = "Updated just now"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed: ${e.message}", e)
            val cachedJson = com.meow.lnctattendance.database.OfflineCacheHelper(context).getTimetableJson()
            updateAppWidgetState(context, id) { state ->
                state[KEY_JSON]   = cachedJson
                state[KEY_STATUS] = "Cached"
            }
        }

        val prefsManager = PreferencesManager(context)
        val appDarkMode = prefsManager.darkMode.firstOrNull()
        val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val useDarkMode = appDarkMode ?: isSystemDark
        val widgetTheme = prefsManager.widgetTheme.firstOrNull() ?: "default"

        provideContent {
            val scheme = getWidgetColorScheme(widgetTheme, useDarkMode)
            val forcedColors = ColorProviders(light = scheme, dark = scheme)
            GlanceTheme(colors = forcedColors) {
                val prefs  = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
                val json   = prefs[KEY_JSON]   ?: "[]"
                val status = prefs[KEY_STATUS] ?: "Tap ↻ to load"
                TimetableWidgetContent(json, status)
            }
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
    val rowHeight = 36.dp
    val isSyncing = status.contains("Updating") || status.contains("Syncing")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .cornerRadius(28.dp) // Large M3 corners
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // ── Header ─────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today  •  ${getTodayName()}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            // Styled refresh icon as a small pill button
            Box(
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshTimetableAction>())
                    .background(if (isSyncing) GlanceTheme.colors.surface else GlanceTheme.colors.primaryContainer)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSyncing) "Syncing" else "Sync",
                    style = TextStyle(
                        color = if (isSyncing) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (periods.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            // LazyColumn to allow scrolling and showing ALL lectures dynamically
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(periods) { (time, subject) ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .background(TranslucentSurface) // High-contrast dynamic translucent look
                            .cornerRadius(12.dp) // Capsule style card
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time Pill Badge
                        Box(
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = time.substringBefore("-").trim(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = 11.sp, // Larger and clearer time text
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        Text(
                            text = subject,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 13.sp // Larger and clearer subject text
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
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
