package com.example.widgetest 

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WordListWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.word_list_widget)

            views.setTextViewText(R.id.widget_title_text, "My Day")
            val sdf = SimpleDateFormat("dd MMMM", Locale.getDefault())
            views.setTextViewText(R.id.widget_date_text, sdf.format(Date()))
            views.setImageViewResource(R.id.widget_header_icon, R.drawable.ic_widget_sun) // Sun icon
            views.setImageViewResource(R.id.widget_add_button, R.drawable.ic_widget_add) // Plus icon

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


            val serviceIntent = Intent(context, WidgetTaskService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_listview, serviceIntent)

            views.setEmptyView(R.id.widget_task_listview, R.id.widget_empty_view_text)

            val itemClickIntent = Intent(context, MainActivity::class.java)
            itemClickIntent.action = "com.example.widgetest.ITEM_CLICK"
            itemClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            itemClickIntent.data = Uri.parse(itemClickIntent.toUri(Intent.URI_INTENT_SCHEME))

            val itemClickPendingIntent = PendingIntent.getActivity(
                context,
                1, 
                itemClickIntent,
                pendingIntentFlags
            )
            views.setPendingIntentTemplate(R.id.widget_task_listview, itemClickPendingIntent)


            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}