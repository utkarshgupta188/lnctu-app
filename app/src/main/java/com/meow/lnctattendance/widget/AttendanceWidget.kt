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
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meow.lnctattendance.R
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

// Breakpoints:
// SMALL  = 2×1 cells → just % + refresh
// LARGE  = 3×2 cells → full layout with icon + stats
private val SIZE_SMALL = DpSize(140.dp, 60.dp)
private val SIZE_LARGE = DpSize(200.dp, 110.dp)

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
    return "Updated ${sdf.format(Date(ms))}"
}

private fun pctColor(pct: Double) = when {
    pct >= 75 -> Color(0xFF4CAF50)
    pct >= 65 -> Color(0xFFFFC107)
    else      -> Color(0xFFE53935)
}

// ── Widget ─────────────────────────────────────────────────────────────────────

class AttendanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendanceWidget()
}

class AttendanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val currentState = androidx.glance.appwidget.state.getAppWidgetState(
            context, PreferencesGlanceStateDefinition, id
        )
        val lastFetchMs = currentState[KEY_LAST_FETCH] ?: 0L
        val now = System.currentTimeMillis()
        val shouldFetch = isInternetAvailable(context) &&
                (now - lastFetchMs) >= TWO_HOURS_MS

        if (shouldFetch) {
            Log.d(TAG, "Auto-refreshing (${(now - lastFetchMs) / 60000}min since last fetch)")
            performFetch(context, id)
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
                AttendanceWidgetSmall(percentage, status)
            }
        }
    }
}

// ── Fetch logic ────────────────────────────────────────────────────────────────

internal suspend fun performFetch(context: Context, id: GlanceId) {
    val authState = PreferencesManager(context).authState.firstOrNull()
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
        updateAppWidgetState(context, id) { state ->
            state[KEY_PRESENT]    = data.present
            state[KEY_ABSENT]     = data.absent
            state[KEY_TOTAL]      = data.totalClasses
            state[KEY_PERCENTAGE] = data.percentage
            state[KEY_LAST_FETCH] = System.currentTimeMillis()
            state[KEY_STATUS]     = ""
        }
        Log.d(TAG, "Fetched: ${data.percentage}%")
    } catch (e: Exception) {
        Log.e(TAG, "Fetch failed: ${e.message}", e)
        updateAppWidgetState(context, id) { state ->
            state[KEY_STATUS] = "Update failed"
        }
    }
    AttendanceWidget().update(context, id)
}

// ── Small layout: 2×1 — just percentage + refresh ─────────────────────────────

@Composable
fun AttendanceWidgetSmall(percentage: Double, status: String) {
    val pctColor = pctColor(percentage)
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(Color(0xFF1A1A27))
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        Image(
            provider = ImageProvider(R.mipmap.ic_launcher),
            contentDescription = "LNCT",
            modifier = GlanceModifier.size(28.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        // Percentage
        Text(
            text = "${"%.1f".format(percentage)}%",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(pctColor),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        // Refresh
        Text(
            text = "↻",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier
                .clickable(actionRunCallback<RefreshAttendanceAction>())
                .padding(4.dp)
        )
    }
}

// ── Large layout: 3×2 — icon + % + stats ──────────────────────────────────────

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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header: icon + title + refresh ────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = "LNCT",
                modifier = GlanceModifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Attendance",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "↻",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFF6C63FF)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshAttendanceAction>())
                    .padding(start = 6.dp, top = 2.dp, end = 0.dp, bottom = 2.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // ── Percentage + stats side by side ───────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big %
            Text(
                text = "${"%.1f".format(percentage)}%",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(pctColor),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Stats column
            Column(
                modifier = GlanceModifier
                    .background(Color(0xFF22223A))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatRow("P", present.toString(), Color(0xFF4CAF50))
                Spacer(modifier = GlanceModifier.height(2.dp))
                StatRow("A", absent.toString(),  Color(0xFFE53935))
                Spacer(modifier = GlanceModifier.height(2.dp))
                StatRow("T", total.toString(),   Color(0xFF6C63FF))
            }
        }

        Spacer(modifier = GlanceModifier.height(5.dp))

        // ── Timestamp ─────────────────────────────────────
        Text(
            text = status,
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFF666688)),
                fontSize = 10.sp
            ),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFF888888)),
                fontSize = 10.sp
            ),
            modifier = GlanceModifier.width(12.dp)
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = value,
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(color),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// ── Refresh Action ─────────────────────────────────────────────────────────────

class RefreshAttendanceAction : ActionCallback {
    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { state ->
            state[KEY_STATUS] = "Updating…"
        }
        AttendanceWidget().update(context, glanceId)
        performFetch(context, glanceId)
    }
}
