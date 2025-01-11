package com.liturgical.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.scheduleListWidgetRefresh
import com.secure.commons.helpers.ensureBackgroundThread

class AppUpdateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ensureBackgroundThread {
                context.config.isRefresh = true
                context.scheduleListWidgetRefresh(context.config.widgetUpdate)
            }
        }
    }
}
