package com.liturgical.calendar.helpers

import android.content.Context
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.interfaces.WeeklyCalendar
import com.liturgical.calendar.models.Event
import com.secure.commons.helpers.DAY_SECONDS
import com.secure.commons.helpers.WEEK_SECONDS
import java.util.*

class WeeklyCalendarImpl(val callback: WeeklyCalendar, val context: Context) {
    var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Long) {
        val endTS = weekStartTS + 2 * WEEK_SECONDS
        context.eventsHelper.getEvents(weekStartTS - DAY_SECONDS, endTS) {
            mEvents = it
            callback.updateWeeklyCalendar(it)
        }
    }
}
