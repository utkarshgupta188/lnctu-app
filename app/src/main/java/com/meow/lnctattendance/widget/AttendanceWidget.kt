package com.meow.lnctattendance.widget

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
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
import androidx.glance.appwidget.state.updateAppWidgetState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meow.lnctattendance.data.ApiService
import com.meow.lnctattendance.prefs.AuthState
import com.meow.lnctattendance.prefs.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AttendanceWidget"
private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L

private val KEY_PRESENT    = intPreferencesKey("present")
private val KEY_ABSENT     = intPreferencesKey("absent")
private val KEY_TOTAL      = intPreferencesKey("total")
private val KEY_PERCENTAGE = doublePreferencesKey("percentage")
private val KEY_STATUS     = stringPreferencesKey("status")
private val KEY_LAST_FETCH = longPreferencesKey("last_fetch_ms")

private val SIZE_SMALL  = DpSize(57.dp,  57.dp)
private val SIZE_MEDIUM = DpSize(130.dp, 57.dp)
private val SIZE_LARGE  = DpSize(200.dp, 110.dp)

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun formatTimestamp(ms: Long): String {
    if (ms == 0L) return "Never updated"
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return "Updated at ${sdf.format(Date(ms))}"
}

// ── Widget ─────────────────────────────────────────────────────────────────────

class AttendanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendanceWidget()
}

class AttendanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read current state to check last fetch time
        val currentState = androidx.glance.appwidget.state.getAppWidgetState(
            context, PreferencesGlanceStateDefinition, id
        )
        val lastFetchMs = currentState[KEY_LAST_FETCH] ?: 0L
        val now = System.currentTimeMillis()
        val shouldFetch = isInternetAvailable(context) &&
                          (now - lastFetchMs) >= TWO_HOURS_MS

        if (shouldFetch) {
            Log.d(TAG, "Auto-refreshing attendance (last fetch ${(now - lastFetchMs) / 60000}min ago)")
            performFetch(context, id)
        } else {
            Log.d(TAG, "Skipping auto-refresh — internet=${isInternetAvailable(context)}, " +
                       "age=${(now - lastFetchMs) / 60000}min")
        }

        provideContent {
            val prefs      = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val present    = prefs[KEY_PRESENT]    ?: 0
            val absent     = prefs[KEY_ABSENT]     ?: 0
            val total      = prefs[KEY_TOTAL]      ?: 0
            val percentage = prefs[KEY_PERCENTAGE] ?: 0.0
            val lastFetch  = prefs[KEY_LAST_FETCH] ?: 0L
            val status     = if (lastFetch > 0L) formatTimestamp(lastFetch)
                             else (prefs[KEY_STATUS] ?: "Tap ↻ to load")

            val size = LocalSize.current
            if (size.width >= SIZE_LARGE.width && size.height >= SIZE_LARGE.height) {
                AttendanceWidgetLarge(present, absent, total, percentage, status)
            } else {
                AttendanceWidgetCompact(percentage, status)
            }
        }
    }
}

// ── Shared fetch logic ─────────────────────────────────────────────────────────

internal suspend fun performFetch(context: Context, id: GlanceId) {
    val prefsManager = PreferencesManager(context)
    val authState = prefsManager.authState.firstOrNull()

    if (authState !is AuthState.Authenticated) {
        updateAppWidgetState(context, id) { state ->
            state[KEY_STATUS] = "Login in app first"
        }
        AttendanceWidget().update(context, id)
        return
    }

    try {
        val data = ApiService.fetchAttendance(
            authState.login.username,
            authState.login.password
        )
        val now = System.currentTimeMillis()
        updateAppWidgetState(context, id) { state ->
            state[KEY_PRESENT]    = data.present
            state[KEY_ABSENT]     = data.absent
            state[KEY_TOTAL]      = data.totalClasses
            state[KEY_PERCENTAGE] = data.percentage
            state[KEY_LAST_FETCH] = now
            // Clear old text status — timestamp will be used instead
            state[KEY_STATUS]     = ""
        }
        Log.d(TAG, "Fetch success: ${data.percentage}%")
    } catch (e: Exception) {
        Log.e(TAG, "Fetch failed: ${e.message}", e)
        updateAppWidgetState(context, id) { state ->
            state[KEY_STATUS] = "Update failed"
        }
    }
    AttendanceWidget().update(context, id)
}

// ── Compact layout ─────────────────────────────────────────────────────────────

@Composable
fun AttendanceWidgetCompact(percentage: Double, status: String) {
    val pctColor = pctColor(percentage)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xFF1A1A27))
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${"%.1f".format(percentage)}%",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(pctColor),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "↻",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier
                .clickable(actionRunCallback<RefreshAttendanceAction>())
                .background(Color(0xFF22223A))
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        val size = LocalSize.current
        if (size.height > 80.dp && status.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = status,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.Gray),
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }
    }
}

// ── Large layout ───────────────────────────────────────────────────────────────

@Composable
fun AttendanceWidgetLarge(
    present: Int, absent: Int, total: Int,
    percentage: Double, status: String
) {
    val pctColor = pctColor(percentage)

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
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LNCT Attendance",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "↻",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshAttendanceAction>())
                    .padding(start = 8.dp, top = 4.dp, end = 0.dp, bottom = 4.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Big percentage
        Text(
            text = "${"%.1f".format(percentage)}%",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(pctColor),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(14.dp))

        // Stats row
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF22223A))
                .cornerRadius(12.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactStat("Present", present.toString(), Color(0xFF4CAF50), GlanceModifier.defaultWeight())
            CompactStat("Absent",  absent.toString(),  Color(0xFFE53935), GlanceModifier.defaultWeight())
            CompactStat("Total",   total.toString(),   Color(0xFF6C63FF), GlanceModifier.defaultWeight())
        }

        if (status.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = status,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.Gray),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun CompactStat(label: String, value: String, color: Color, modifier: GlanceModifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = TextStyle(
            color = androidx.glance.unit.ColorProvider(color),
            fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Text(text = label, style = TextStyle(
            color = androidx.glance.unit.ColorProvider(Color.Gray),
            fontSize = 11.sp))
    }
}

private fun pctColor(pct: Double) = when {
    pct >= 75 -> Color(0xFF4CAF50)
    pct >= 65 -> Color(0xFFFFC107)
    else      -> Color(0xFFE53935)
}

// ── Refresh Action ─────────────────────────────────────────────────────────────

class RefreshAttendanceAction : ActionCallback {
    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        // Show "Updating…" immediately
        updateAppWidgetState(context, glanceId) { state ->
            state[KEY_STATUS] = "Updating…"
        }
        AttendanceWidget().update(context, glanceId)

        // Then fetch fresh data
        performFetch(context, glanceId)
    }
}
