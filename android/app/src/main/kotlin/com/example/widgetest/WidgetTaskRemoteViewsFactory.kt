package com.example.widgetest // Make sure this matches your package name

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.json.JSONArray
import org.json.JSONException

class WidgetTaskRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: MutableList<WidgetTask> = mutableListOf()

    override fun onCreate() {
        // Connect to data source, load initial data
        loadTasksFromPreferences()
    }

    override fun onDataSetChanged() {
        // This is called by notifyAppWidgetViewDataChanged()
        val identityToken = Binder.clearCallingIdentity() // Temporarily clear calling identity
        try {
            loadTasksFromPreferences()
        } finally {
            Binder.restoreCallingIdentity(identityToken) // Restore calling identity
        }
    }

    override fun onDestroy() {
        tasks.clear()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= tasks.size) {
            return null
        }
        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item_layout)

        views.setTextViewText(R.id.task_item_title_text, task.title)

        val checkboxResId = if (task.isCompleted) R.drawable.ic_widget_radio_button_checked
                            else R.drawable.ic_widget_radio_button_unchecked
        views.setImageViewResource(R.id.task_item_checkbox_image, checkboxResId)

        // Handle "Today" and reminder icon visibility based on your task's logic
        if (task.isConsideredToday()) { // Example: shows if createdAt is today
            views.setViewVisibility(R.id.task_item_details_container, android.view.View.VISIBLE)
            views.setTextViewText(R.id.task_item_due_date_text, "Today")
            // Show/hide reminder icon based on a task property (e.g., if a reminder is set)
            if (task.hasReminder()) { // You'll need to implement this logic in WidgetTask
                 views.setViewVisibility(R.id.task_item_reminder_image, android.view.View.VISIBLE)
            } else {
                 views.setViewVisibility(R.id.task_item_reminder_image, android.view.View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.task_item_details_container, android.view.View.GONE)
        }

        // Set up fill-in intent for item clicks (e.g., to open the app to this task)
        val fillInIntent = Intent()
        // You can add extras to identify the task, e.g., task.id
        fillInIntent.putExtra("task_id", task.id)
        views.setOnClickFillInIntent(R.id.task_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        // Optional: Return a RemoteViews for a loading indicator
        return null // Or a custom layout
    }

    override fun getViewTypeCount(): Int = 1 // Only one type of item view

    override fun getItemId(position: Int): Long = tasks[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadTasksFromPreferences() {
        tasks.clear()
        val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        // Flutter's shared_preferences plugin prefixes keys with "flutter."
        val tasksJsonString = prefs.getString("flutter.tasks", null)
        Log.d("WidgetFactory", "Tasks JSON from Prefs: $tasksJsonString")


        if (tasksJsonString != null) {
            try {
                val jsonArray = JSONArray(tasksJsonString)
                for (i in 0 until jsonArray.length()) {
                    tasks.add(WidgetTask.fromJson(jsonArray.getJSONObject(i)))
                }
                // Optional: Sort tasks (e.g., incomplete first, then by date)
                // tasks.sortWith(compareBy<WidgetTask> { it.isCompleted }.thenByDescending { it.createdAt })
            } catch (e: JSONException) {
                Log.e("WidgetFactory", "Error parsing tasks JSON: ${e.message}")
                tasks.clear()
            }
        }
        Log.d("WidgetFactory", "Loaded ${tasks.size} tasks.")
    }
}