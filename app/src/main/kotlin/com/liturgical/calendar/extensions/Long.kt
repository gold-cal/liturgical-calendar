package com.liturgical.calendar.extensions

import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.models.Event
import kotlin.math.pow

fun Long.isTsOnProperDay(event: Event): Boolean {
    val dateTime = Formatter.getDateTimeFromTS(this)
    val power = 2.0.pow((dateTime.dayOfWeek - 1).toDouble()).toInt()
    return event.repeatRule and power != 0
}
