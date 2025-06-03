package com.example.widgetest 

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
        loadTasksFromPreferences()
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity() 
        try {
            loadTasksFromPreferences()
        } finally {
            Binder.restoreCallingIdentity(identityToken) 
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

        if (task.isConsideredToday()) { 
            views.setViewVisibility(R.id.task_item_details_container, android.view.View.VISIBLE)
            views.setTextViewText(R.id.task_item_due_date_text, "Today")
            
            if (task.hasReminder()) { 
                 views.setViewVisibility(R.id.task_item_reminder_image, android.view.View.VISIBLE)
            } else {
                 views.setViewVisibility(R.id.task_item_reminder_image, android.view.View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.task_item_details_container, android.view.View.GONE)
        }

        val fillInIntent = Intent()
        fillInIntent.putExtra("task_id", task.id)
        views.setOnClickFillInIntent(R.id.task_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null 
    }

    override fun getViewTypeCount(): Int = 1 

    override fun getItemId(position: Int): Long = tasks[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadTasksFromPreferences() {
        tasks.clear()
        val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val tasksJsonString = prefs.getString("flutter.tasks", null)
        Log.d("WidgetFactory", "Tasks JSON from Prefs: $tasksJsonString")


        if (tasksJsonString != null) {
            try {
                val jsonArray = JSONArray(tasksJsonString)
                for (i in 0 until jsonArray.length()) {
                    tasks.add(WidgetTask.fromJson(jsonArray.getJSONObject(i)))
                }
            } catch (e: JSONException) {
                Log.e("WidgetFactory", "Error parsing tasks JSON: ${e.message}")
                tasks.clear()
            }
        }
        Log.d("WidgetFactory", "Loaded ${tasks.size} tasks.")
    }
}