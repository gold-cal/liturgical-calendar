package com.liturgical.calendar.services

import android.app.IntentService
import android.content.Intent
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsDB
import com.liturgical.calendar.extensions.rescheduleReminder
import com.liturgical.calendar.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventOrTaskWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}
