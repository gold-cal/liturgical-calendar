package com.liturgical.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.recheckCalDAVCalendars
import com.liturgical.calendar.extensions.refreshCalDAVCalendars
import com.liturgical.calendar.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshCalDAVCalendars(context.config.caldavSyncedCalendarIds, false)
        }

        context.recheckCalDAVCalendars(true) {
            context.updateWidgets()
        }
    }
}
