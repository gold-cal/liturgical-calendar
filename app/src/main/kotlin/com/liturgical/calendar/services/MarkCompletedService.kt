package com.liturgical.calendar.services

import android.app.IntentService
import android.content.Intent
import com.liturgical.calendar.extensions.cancelNotification
import com.liturgical.calendar.extensions.cancelPendingIntent
import com.liturgical.calendar.extensions.eventsDB
import com.liturgical.calendar.extensions.updateTaskCompletion
import com.liturgical.calendar.helpers.ACTION_MARK_COMPLETED
import com.liturgical.calendar.helpers.EVENT_ID

class MarkCompletedService : IntentService("MarkCompleted") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && intent.action == ACTION_MARK_COMPLETED) {
            val taskId = intent.getLongExtra(EVENT_ID, 0L)
            val task = eventsDB.getTaskWithId(taskId)
            if (task != null) {
                updateTaskCompletion(task, true)
                cancelPendingIntent(task.id!!)
                cancelNotification(task.id!!)
            }
        }
    }
}
