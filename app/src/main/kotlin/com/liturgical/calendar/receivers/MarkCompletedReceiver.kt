package com.liturgical.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liturgical.calendar.extensions.eventsDB
import com.liturgical.calendar.extensions.updateTaskCompletion
import com.liturgical.calendar.helpers.ACTION_MARK_COMPLETED
import com.liturgical.calendar.helpers.EVENT_ID
import com.secure.commons.helpers.ensureBackgroundThread

class MarkCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_COMPLETED) {
            ensureBackgroundThread {
                val taskId = intent.getLongExtra(EVENT_ID, 0L)
                val task = context.eventsDB.getTaskWithId(taskId)
                if (task != null) {
                    context.updateTaskCompletion(task, true)
                    //context.cancelPendingIntent(task.id!!)
                    //context.cancelNotification(task.id!!)
                }
            }
        }
    }
}
