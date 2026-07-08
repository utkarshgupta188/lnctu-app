package com.meow.lnctattendance.background

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.meow.lnctattendance.MainActivity
import com.meow.lnctattendance.api.ApiService
import com.meow.lnctattendance.prefs.AuthState
import com.meow.lnctattendance.prefs.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class AttendanceAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleDailyCheck(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkAttendanceAndNotify(context)
            } catch (e: Exception) {
                Log.e("AttendanceAlarm", "Daily check failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun checkAttendanceAndNotify(context: Context) {
        val prefsManager = PreferencesManager(context)
        val authState = prefsManager.authState.firstOrNull()

        if (authState is AuthState.Authenticated) {
            val data = ApiService.fetchAttendance(
                authState.login.username,
                authState.login.password
            )
            // Cache it too
            com.meow.lnctattendance.database.OfflineCacheHelper(context).saveAttendance(
                data.present, data.absent, data.totalClasses, data.percentage, System.currentTimeMillis()
            )

            if (data.percentage < 75.0) {
                showNotification(
                    context,
                    "Attendance Alert!",
                    "Your attendance is currently ${"%.1f".format(data.percentage)}% (below 75%). Please attend classes!"
                )
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "attendance_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES_O) {
            val channel = NotificationChannel(
                channelId,
                "Attendance Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(101, notification)
    }

    companion object {
        fun scheduleDailyCheck(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AttendanceAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 100, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 9) // 9:00 AM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                Log.d("AttendanceAlarm", "Scheduled daily check at 9:00 AM")
            } catch (e: Exception) {
                Log.e("AttendanceAlarm", "Failed to schedule alarm", e)
            }
        }
    }
}
