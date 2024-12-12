package com.liturgical.calendar.interfaces

import com.liturgical.calendar.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
