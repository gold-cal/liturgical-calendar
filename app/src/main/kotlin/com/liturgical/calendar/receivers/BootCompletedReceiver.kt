package com.liturgical.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liturgical.calendar.extensions.*
import com.secure.commons.helpers.ensureBackgroundThread

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
                intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
                context.apply {
                    scheduleListWidgetRefresh(config.widgetUpdate)
                    scheduleAllEvents()
                    notifyRunningEvents()
                    recheckCalDAVCalendars(true) {}
                }
            }
        }
    }
}
