package com.liturgical.calendar.extensions

import com.liturgical.calendar.helpers.MONTH
import com.liturgical.calendar.helpers.WEEK
import com.liturgical.calendar.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0
