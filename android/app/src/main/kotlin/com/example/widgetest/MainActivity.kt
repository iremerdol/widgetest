package com.example.widgetest // Make sure this matches your package name

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "widget_channel"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "updateWidget") {
                try {
                    Log.d("MainActivity", "updateWidget called from Flutter")
                    val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                    val componentName = ComponentName(applicationContext, WordListWidget::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                    if (appWidgetIds.isNotEmpty()) {
                        // This tells the ListView in the widget to refresh its data.
                        // It will call onDataSetChanged() in your RemoteViewsFactory.
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_listview)

                        // Optionally, to update other parts of the widget (like date in header) if they changed.
                        // You could also call the full onUpdate loop:
                         for (appWidgetId in appWidgetIds) {
                             WordListWidget.updateAppWidget(applicationContext, appWidgetManager, appWidgetId)
                         }
                        Log.d("MainActivity", "Widget update signaled for ${appWidgetIds.size} widgets.")
                        result.success(null)
                    } else {
                        Log.d("MainActivity", "No widget instances found to update.")
                        result.success(null) // Still success, just nothing to update
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating widget: ${e.message}", e)
                    result.error("WIDGET_UPDATE_FAILED", "Failed to update widget: ${e.message}", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.example.widgetest.ITEM_CLICK") {
            val taskId = intent.getStringExtra("task_id")
            Log.d("MainActivity", "Widget item clicked, Task ID: $taskId")
            // Here you can navigate to a specific part of your Flutter app
            // or show a dialog, etc., based on the taskId.
            // For now, just logging.
        }
    }
}