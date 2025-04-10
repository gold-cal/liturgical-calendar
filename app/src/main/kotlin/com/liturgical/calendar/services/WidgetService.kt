package com.liturgical.calendar.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.liturgical.calendar.adapters.EventListWidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapter(applicationContext, intent)
}
