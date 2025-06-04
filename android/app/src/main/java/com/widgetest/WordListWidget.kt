package com.example.widgetest 

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WordListWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "WordListWidget"
        const val ACTION_AUTO_UPDATE_TIMER = "com.example.widgetest.ACTION_AUTO_UPDATE_TIMER"
        private const val REQUEST_CODE_AUTO_UPDATE = 1001
        private const val UPDATE_INTERVAL_MINUTES = 10 

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            isTimerUpdate: Boolean = false
        ) {
            Log.d(TAG, "updateAppWidget called for ID: $appWidgetId. Timer update: $isTimerUpdate")
            val views = RemoteViews(context.packageName, R.layout.word_list_widget)

            if (!isTimerUpdate) {
                views.setTextViewText(R.id.widget_title_text, "My Day")
                val sdf = SimpleDateFormat("dd MMMM", Locale.getDefault())
                views.setTextViewText(R.id.widget_date_text, sdf.format(Date()))
                views.setImageViewResource(R.id.widget_header_icon, R.drawable.ic_widget_sun)
                views.setImageViewResource(R.id.widget_add_button, R.drawable.ic_widget_add)

                val launchAppIntent = Intent(context, MainActivity::class.java)
                launchAppIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingLaunchAppIntent = PendingIntent.getActivity(context, 0, launchAppIntent, pendingIntentFlags)
                views.setOnClickPendingIntent(R.id.widget_header, pendingLaunchAppIntent)
                views.setOnClickPendingIntent(R.id.widget_add_button, pendingLaunchAppIntent)

                val itemClickIntent = Intent(context, MainActivity::class.java)
                itemClickIntent.action = "com.example.widgetest.ITEM_CLICK"
                itemClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                itemClickIntent.data = Uri.parse(itemClickIntent.toUri(Intent.URI_INTENT_SCHEME))
                val itemClickPendingIntent = PendingIntent.getActivity(
                    context, 1, itemClickIntent, pendingIntentFlags
                )
                views.setPendingIntentTemplate(R.id.widget_task_listview, itemClickPendingIntent)
            }

            val serviceIntent = Intent(context, WidgetTaskService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_listview, serviceIntent)
            views.setEmptyView(R.id.widget_task_listview, R.id.widget_empty_view_text)

            if (isTimerUpdate) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_listview)
                Log.d(TAG, "Timer update: Notified data set changed for ListView for widget ID $appWidgetId")
            } else {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_listview) 
                Log.d(TAG, "Full update: Updated widget and notified data set changed for widget ID $appWidgetId")
            }
        }

        private fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WordListWidget::class.java).apply {
                action = ACTION_AUTO_UPDATE_TIMER
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, REQUEST_CODE_AUTO_UPDATE, intent, flags)
        }


        fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. Widget auto-update will not work. User needs to grant 'Alarms & reminders' permission via app settings.")
                return
            }

            val pendingIntent = getPendingIntent(context)
            val now = Calendar.getInstance()
            val triggerAtMillis = now.apply {
                add(Calendar.MINUTE, UPDATE_INTERVAL_MINUTES) 
                set(Calendar.SECOND, 5) 
                set(Calendar.MILLISECOND, 0)

            }.timeInMillis


            try {
                Log.d(TAG, "Attempting to schedule next ${UPDATE_INTERVAL_MINUTES}min update at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerAtMillis))}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Log.i(TAG, "Successfully scheduled next ${UPDATE_INTERVAL_MINUTES}min update for widget.")
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException while scheduling exact alarm. Ensure 'Alarms & Reminders' permission is granted.", se)
            } catch (e: Exception) {
                Log.e(TAG, "Generic error scheduling alarm: ${e.message}", e)
            }
        }

        fun cancelUpdates(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pendingIntent = getPendingIntent(context)
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled widget auto-updates.")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling updates: ${e.message}", e)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called. IDs: ${appWidgetIds.joinToString()}. Scheduling/Rescheduling updates.")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, isTimerUpdate = false)
        }
        if (appWidgetIds.isNotEmpty()) {
            scheduleNextUpdate(context)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled - First widget instance added. Scheduling initial update.")
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "onDisabled - Last widget instance removed. Cancelling updates.")
        cancelUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d(TAG, "onReceive received action: $action")

        if (ACTION_AUTO_UPDATE_TIMER == action) {
            Log.i(TAG, "Timer fired for widget update.")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, WordListWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(cn)

            if (appWidgetIds.isEmpty()) {
                Log.w(TAG, "Timer fired, but no active widget instances. Cancelling further updates.")
                cancelUpdates(context)
                return
            }

            Log.d(TAG, "Timer update: Refreshing ${appWidgetIds.size} widget(s).")
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, isTimerUpdate = true)
            }
            scheduleNextUpdate(context) 
        }
    }
}