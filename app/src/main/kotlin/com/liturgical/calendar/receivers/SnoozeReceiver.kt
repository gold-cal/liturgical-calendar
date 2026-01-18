package com.liturgical.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsDB
import com.liturgical.calendar.extensions.rescheduleReminder
import com.liturgical.calendar.helpers.ACTION_SNOOZE
import com.liturgical.calendar.helpers.EVENT_ID
import com.secure.commons.helpers.ensureBackgroundThread

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action  == ACTION_SNOOZE) {
            ensureBackgroundThread {
                val eventId = intent.getLongExtra(EVENT_ID, 0L)
                val event = context.eventsDB.getEventOrTaskWithId(eventId)
                context.rescheduleReminder(event, context.config.snoozeTime)
            }
        }
    }
}
