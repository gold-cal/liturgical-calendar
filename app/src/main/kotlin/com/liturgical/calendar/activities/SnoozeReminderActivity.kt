package com.liturgical.calendar.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsDB
import com.liturgical.calendar.extensions.rescheduleReminder
import com.liturgical.calendar.helpers.EVENT_ID
import com.secure.commons.extensions.showPickSecondsDialogHelper
import com.secure.commons.helpers.ensureBackgroundThread

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showPickSecondsDialogHelper(config.snoozeTime, true, cancelCallback = { dialogCancelled() }) {
            ensureBackgroundThread {
                val eventId = intent.getLongExtra(EVENT_ID, 0L)
                val event = eventsDB.getEventOrTaskWithId(eventId)
                config.snoozeTime = it / 60
                rescheduleReminder(event, it / 60)
                runOnUiThread {
                    finishActivity()
                }
            }
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
