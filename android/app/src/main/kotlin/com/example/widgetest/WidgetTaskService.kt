package com.example.widgetest // Make sure this matches your package name

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetTaskService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetTaskRemoteViewsFactory(this.applicationContext)
    }
}