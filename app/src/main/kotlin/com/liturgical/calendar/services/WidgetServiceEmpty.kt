package com.liturgical.calendar.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.liturgical.calendar.adapters.EventListWidgetAdapterEmpty

class WidgetServiceEmpty : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapterEmpty(applicationContext)
}
