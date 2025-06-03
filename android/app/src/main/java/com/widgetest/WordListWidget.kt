package com.example.widgetest

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import org.json.JSONArray
import android.content.ComponentName

class WordListWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WordListWidget::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Use the default SharedPreferences (same as Flutter)
        val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        
        // Get tasks JSON string from Flutter's storage
        val tasksJson = prefs.getString("flutter.tasks", "[]") ?: "[]"
        
        Log.d("WidgetUpdate", "Tasks JSON: $tasksJson")
        
        // Parse tasks
        val tasks = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(tasksJson)
            for (i in 0 until jsonArray.length()) {
                val task = jsonArray.getJSONObject(i)
                tasks.add(task.getString("title"))
            }
        } catch (e: Exception) {
            Log.e("WidgetUpdate", "Error parsing tasks: ${e.message}")
            tasks.add("Error loading tasks")
        }
        
        // Create the widget view
        val views = RemoteViews(context.packageName, R.layout.word_list_widget)
        
        // Format the tasks with bullet points
        val formattedTasks = if (tasks.isEmpty()) {
            "No tasks yet!"
        } else {
            tasks.joinToString("\n") { "• $it" }
        }
        
        Log.d("WidgetUpdate", "Formatted tasks: $formattedTasks")
        
        // Update the widget
        views.setTextViewText(R.id.widget_title, "My Tasks")
        views.setTextViewText(R.id.words_text, formattedTasks)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}