package com.example.widgetest 

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetTaskService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetTaskRemoteViewsFactory(this.applicationContext)
    }
}