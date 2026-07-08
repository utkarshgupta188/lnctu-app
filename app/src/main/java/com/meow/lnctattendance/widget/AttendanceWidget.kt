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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.GlanceTheme
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
import androidx.glance.material3.ColorProviders
import com.meow.lnctattendance.ui.theme.*
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

    override val sizeMode = SizeMode.Exact

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

        val prefsManager = PreferencesManager(context)
        val appDarkMode = prefsManager.darkMode.firstOrNull()
        val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val useDarkMode = appDarkMode ?: isSystemDark

        provideContent {
            val scheme = if (useDarkMode) DarkWidgetColorScheme else LightWidgetColorScheme
            val forcedColors = ColorProviders(light = scheme, dark = scheme)
            GlanceTheme(colors = forcedColors) {
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
            .background(GlanceTheme.colors.background)
            .cornerRadius(28.dp) // Large M3 organic corners
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${"%.1f".format(percentage)}%",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(pctColor),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "↻ Refresh",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier
                .clickable(actionRunCallback<RefreshAttendanceAction>())
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(20.dp) // Pill shape button
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
        val size = LocalSize.current
        if (size.height > 80.dp && status.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = status,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
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
            .background(GlanceTheme.colors.background)
            .cornerRadius(28.dp) // Premium organic rounded corners
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "LNCT Attendance",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (status.isNotEmpty()) {
                    Text(
                        text = status,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
            // Styled refresh icon as a small pill button
            Box(
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshAttendanceAction>())
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sync",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Large stats middle row - fills available vertical space
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Percentage container
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(pctColor),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = if (percentage >= 75) "ON TRACK" else "ATTEND CLASS",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(pctColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Stats vertical capsules
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerticalStatPill("P", present.toString(), Color(0xFF4CAF50))
                Spacer(modifier = GlanceModifier.width(8.dp))
                VerticalStatPill("A", absent.toString(), Color(0xFFE53935))
                Spacer(modifier = GlanceModifier.width(8.dp))
                VerticalStatPill("T", total.toString(), GlanceTheme.colors.primary)
            }
        }
    }
}

@Composable
fun VerticalStatPill(label: String, value: String, color: Color) {
    VerticalStatPill(label, value, androidx.glance.unit.ColorProvider(color))
}

@Composable
fun VerticalStatPill(label: String, value: String, colorProvider: androidx.glance.unit.ColorProvider) {
    Column(
        modifier = GlanceModifier
            .width(48.dp) // Wider capsules to fit larger text
            .height(78.dp) // Taller capsules
            .background(TranslucentSurface) // High-contrast dynamic translucent look
            .cornerRadius(24.dp)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = colorProvider,
                fontSize = 22.sp, // Bold, massive, highly clear numbers
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp, // Larger and clearer labels
                fontWeight = FontWeight.Bold
            )
        )
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
