package com.liturgical.calendar.helpers

import com.liturgical.calendar.extensions.isXMonthlyRepetition
import com.liturgical.calendar.extensions.isXWeeklyRepetition
import com.liturgical.calendar.extensions.isXYearlyRepetition
import com.liturgical.calendar.extensions.seconds
import com.liturgical.calendar.models.Event
import com.liturgical.calendar.models.EventExtendedRule
import com.liturgical.calendar.models.EventRepetition
import com.secure.commons.extensions.areDigitsOnly
import com.secure.commons.helpers.*
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import kotlin.math.floor
import kotlin.math.pow

class Parser {
    // EXRRULE:EXT=EXP;EXCEPTION=D,39-E,PD-SMW,2-R,23,30
    /** EXCEPTION Format:
     * D = Date of exception in ddmm or dd format
     *     If E, then it equals the number of days or weeks of event
     * E = Dependent on Easter
     * PD = Plus Days
     * PW = PLUS Weeks
     * MD = Minus Days
     * MW = Minus Weeks
     * R = Range of exception
     * SPD or SMW : S = shift date
     *              D = Days  P = Plus
     *              W = Weeks M = Minus
     */
    private fun parseExceptionRule(stringValue: String): Int {
        val parts = stringValue.split("-").filter { it.isNotEmpty() }
        var exceptionRule = 1

        for (part in parts) {
            val keyValue = part.split(",")
            if (keyValue.size <= 1) {continue}
            var buf = 0
            val key = keyValue[0]
            var value = keyValue[1]
            exceptionRule = when (key) {
                "D" -> { buf = if (value.toInt() > 255) continue else value.toInt() shl 1
                        exceptionRule or buf }
                "E" -> when (value) {
                    PD -> exceptionRule or EX_RULE_EPD
                    PW -> exceptionRule or EX_RULE_EPW
                    MD -> exceptionRule or EX_RULE_EMD
                    MW -> exceptionRule or EX_RULE_EMW
                    else -> exceptionRule
                }
                SPD -> { buf = if (value.toInt() > 31) continue else value.toInt() shl 17
                    exceptionRule or EX_RULE_SPD or buf
                }
                SMD -> { buf = if (value.toInt() > 31) continue else value.toInt() shl 17
                    exceptionRule or EX_RULE_SMD or buf
                }
                SPW -> { buf = if (value.toInt() > 31) continue else value.toInt() shl 17
                    exceptionRule or EX_RULE_SPW or buf
                }
                SMW -> { buf = if (value.toInt() > 31) continue else value.toInt() shl 17
                    exceptionRule or EX_RULE_SMW or buf
                }
                "R" -> { if (keyValue.size < 3) continue
                    buf = if (value.toInt() > 255) continue else value.toInt() shl 9
                    value = keyValue[2]
                    buf = if (value.toInt() > 255) continue else buf or (value.toInt() shl 1)
                    exceptionRule or EX_RULE_R or buf
                }
                else -> exceptionRule
            }
        }

        return exceptionRule
    }

    // EXRRULE:FM=AFTER or EXRRULE:FM_PD=20
    // EXRRULE:
    fun parseExtendedRule(fullString: String, repeatRule: Int, flags: Int): EventExtendedRule {
        val parts = fullString.split(";").filter { it.isNotEmpty() }
        var newRepeatRule = 0
        var newExtendedRule = 1
        var newFlags = flags
        /*if (!repetition.repeatInterval.isXYearlyRepetition()) {
            return repetition
        } */

        for (part in parts) {
            val keyValue = part.split("=")
            if (keyValue.size <= 1) { continue }
            val key = keyValue[0]
            val value = keyValue[1]
            when (key) {
                FULL_MOON -> newRepeatRule = when (value) {
                    AFTER -> REPEAT_AFTER_FM
                    BEFORE -> REPEAT_BEFORE_FM
                    else -> repeatRule
                }
                EXT -> when (value) {
                    HOLY_NAME_JESUS -> newRepeatRule = REPEAT_HNOJ
                    EXP -> newRepeatRule = REPEAT_SAME_DAY_WITH_EXCEPTION
                    FLAG_OFA -> newFlags = newFlags or FLAG_FISH_OFA
                    FLAG_OA_TF -> newFlags = newFlags or FLAG_FISH_OA_TF
                    FLAG_TFA -> newFlags = newFlags or FLAG_FISH_TFA
                    FLAG_TA -> newFlags = newFlags or FLAG_FISH_TA
                    FLAG_TFPA -> newFlags = newFlags or FLAG_FISH_TFPA
                    else -> newRepeatRule = repeatRule
                }
                EXCEPTION -> newExtendedRule = parseExceptionRule(value)
                FM_PLUS_DAYS -> newRepeatRule = FM_ADD_DAYS_RULE + value.toInt()
                FM_PLUS_WEEKS -> newRepeatRule = FM_ADD_WEEKS_RULE + value.toInt()
                FM_MINUS_DAYS -> newRepeatRule = FM_MINUS_DAYS_RULE + value.toInt()
                FM_MINUS_WEEKS -> newRepeatRule = FM_MINUS_WEEKS_RULE + value.toInt()
                else -> newRepeatRule = repeatRule
            }
        }
        return EventExtendedRule(newRepeatRule, newExtendedRule, newFlags )
    }
    // RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=2
    // from RRULE:FREQ=DAILY;COUNT=5 to Daily, 5x...
    fun parseRepeatInterval(fullString: String, startTS: Long): EventRepetition {
        val parts = fullString.split(";").filter { it.isNotEmpty() }
        var repeatInterval = 0
        var repeatRule = 0
        var repeatLimit = 0L

        for (part in parts) {
            val keyValue = part.split("=")
            if (keyValue.size <= 1) {
                continue
            }

            val key = keyValue[0]
            val value = keyValue[1]
            if (key == FREQ) {
                repeatInterval = getFrequencySeconds(value)
                if (value == WEEKLY) {
                    val start = Formatter.getDateTimeFromTS(startTS)
                    repeatRule = 2.0.pow((start.dayOfWeek - 1).toDouble()).toInt()
                } else if (value == MONTHLY || value == YEARLY) {
                    repeatRule = REPEAT_SAME_DAY
                } else if (value == DAILY && fullString.contains(INTERVAL)) {
                    val interval = fullString.substringAfter("$INTERVAL=").substringBefore(";")
                    // properly handle events repeating by 14 days or so, just add a repeat rule to specify a day of the week
                    if (interval.areDigitsOnly() && interval.toInt() % 7 == 0) {
                        val dateTime = Formatter.getDateTimeFromTS(startTS)
                        repeatRule = 2.0.pow((dateTime.dayOfWeek - 1).toDouble()).toInt()
                    } else if (fullString.contains("BYDAY")) {
                        // some services use weekly repetition for repeating on specific week days, some use daily
                        // make these produce the same result
                        // RRULE:FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR
                        // RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
                        repeatInterval = WEEK_SECONDS
                    }
                }
            } else if (key == COUNT) {
                repeatLimit = -value.toLong()
            } else if (key == UNTIL) {
                repeatLimit = parseDateTimeValue(value)
            } else if (key == INTERVAL) {
                repeatInterval *= value.toInt()
            } else if (key == BYDAY) {
                if (repeatInterval.isXWeeklyRepetition()) {
                    repeatRule = handleRepeatRule(value)
                } else if (repeatInterval.isXMonthlyRepetition() || repeatInterval.isXYearlyRepetition()) {
                    repeatRule = if (value.startsWith("-1")) REPEAT_ORDER_WEEKDAY_USE_LAST else REPEAT_ORDER_WEEKDAY
                }
            } else if (key == BYMONTHDAY) {
                if (value.split(",").any { it.toInt() == -1 }) {
                    repeatRule = REPEAT_LAST_DAY
                }
            }
        }
        return EventRepetition(repeatInterval, repeatRule, repeatLimit, 0)
    }

    private fun getFrequencySeconds(interval: String) = when (interval) {
        DAILY -> DAY
        WEEKLY -> WEEK
        MONTHLY -> MONTH
        YEARLY -> YEAR
        else -> 0
    }

    private fun handleRepeatRule(value: String): Int {
        var newRepeatRule = 0
        if (value.contains(MO))
            newRepeatRule = newRepeatRule or MONDAY_BIT
        if (value.contains(TU))
            newRepeatRule = newRepeatRule or TUESDAY_BIT
        if (value.contains(WE))
            newRepeatRule = newRepeatRule or WEDNESDAY_BIT
        if (value.contains(TH))
            newRepeatRule = newRepeatRule or THURSDAY_BIT
        if (value.contains(FR))
            newRepeatRule = newRepeatRule or FRIDAY_BIT
        if (value.contains(SA))
            newRepeatRule = newRepeatRule or SATURDAY_BIT
        if (value.contains(SU))
            newRepeatRule = newRepeatRule or SUNDAY_BIT
        return newRepeatRule
    }

    fun parseDateTimeValue(value: String): Long {
        val edited = value.replace("T", "").replace("Z", "").replace("-", "")
        return if (edited.length == 14) {
            parseLongFormat(edited, value.endsWith("Z"))
        } else {
            val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd")
            dateTimeFormat.parseDateTime(edited).withHourOfDay(5).seconds()
        }
    }

    private fun parseLongFormat(digitString: String, useUTC: Boolean): Long {
        val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")
        val dateTimeZone = if (useUTC) DateTimeZone.UTC else DateTimeZone.getDefault()
        return dateTimeFormat.parseDateTime(digitString).withZoneRetainFields(dateTimeZone).seconds()
    }

    fun getExRepeatCode(event: Event): String {
        val repeatInterval = event.repeatInterval
        if (repeatInterval < YEAR) return ""

        val repeatRule = getExCode(event.repeatRule)
        val repeatNumber = getExNumber(event.repeatRule)
        return "$repeatRule=$repeatNumber"
    }

    private fun getExCode(repeatRule: Int) = when {
        repeatRule > FM_MINUS_WEEKS_RULE -> FM_MINUS_WEEKS
        repeatRule > FM_MINUS_DAYS_RULE -> FM_MINUS_DAYS
        repeatRule > FM_ADD_WEEKS_RULE -> FM_PLUS_WEEKS
        else -> FM_PLUS_DAYS
    }

    private fun getExNumber(repeatRule: Int) = when {
        repeatRule > FM_MINUS_WEEKS_RULE -> (FM_MINUS_WEEKS_RULE xor repeatRule)
        repeatRule > FM_MINUS_DAYS_RULE -> (FM_MINUS_DAYS_RULE xor repeatRule)
        repeatRule > FM_ADD_WEEKS_RULE -> (FM_ADD_WEEKS_RULE xor repeatRule)
        else -> (FM_ADD_DAYS_RULE xor repeatRule)
    }

    // from Daily, 5x... to RRULE:FREQ=DAILY;COUNT=5
    fun getRepeatCode(event: Event): String {
        val repeatInterval = event.repeatInterval
        if (repeatInterval == 0)
            return ""

        val freq = getFreq(repeatInterval)
        val interval = getInterval(repeatInterval)
        val repeatLimit = getRepeatLimitString(event)
        val byMonth = getByMonth(event)
        val byDay = getByDay(event)
        return "$FREQ=$freq;$INTERVAL=$interval$repeatLimit$byMonth$byDay"
    }

    private fun getFreq(interval: Int) = when {
        interval % YEAR == 0 -> YEARLY
        interval % MONTH == 0 -> MONTHLY
        interval % WEEK == 0 -> WEEKLY
        else -> DAILY
    }

    private fun getInterval(interval: Int) = when {
        interval % YEAR == 0 -> interval / YEAR
        interval % MONTH == 0 -> interval / MONTH
        interval % WEEK == 0 -> interval / WEEK
        else -> interval / DAY
    }

    private fun getRepeatLimitString(event: Event) = when {
        event.repeatLimit == 0L -> ""
        event.repeatLimit < 0 -> ";$COUNT=${-event.repeatLimit}"
        else -> ";$UNTIL=${Formatter.getDayCodeFromTS(event.repeatLimit)}"
    }

    private fun getByMonth(event: Event) = when {
        event.repeatInterval.isXYearlyRepetition() -> {
            val start = Formatter.getDateTimeFromTS(event.startTS)
            ";$BYMONTH=${start.monthOfYear}"
        }
        else -> ""
    }

    private fun getByDay(event: Event) = when {
        event.repeatInterval.isXWeeklyRepetition() -> {
            val days = getByDayString(event.repeatRule)
            ";$BYDAY=$days"
        }
        event.repeatInterval.isXMonthlyRepetition() || event.repeatInterval.isXYearlyRepetition() -> when (event.repeatRule) {
            REPEAT_LAST_DAY -> ";$BYMONTHDAY=-1"
            REPEAT_ORDER_WEEKDAY_USE_LAST, REPEAT_ORDER_WEEKDAY -> {
                val start = Formatter.getDateTimeFromTS(event.startTS)
                val dayOfMonth = start.dayOfMonth
                val isLastWeekday = start.monthOfYear != start.plusDays(7).monthOfYear
                val order = if (isLastWeekday) "-1" else ((dayOfMonth - 1) / 7 + 1).toString()
                val day = getDayLetters(start.dayOfWeek)
                ";$BYDAY=$order$day"
            }
            else -> ""
        }
        else -> ""
    }

    private fun getByDayString(rule: Int): String {
        var result = ""
        if (rule and MONDAY_BIT != 0)
            result += "$MO,"
        if (rule and TUESDAY_BIT != 0)
            result += "$TU,"
        if (rule and WEDNESDAY_BIT != 0)
            result += "$WE,"
        if (rule and THURSDAY_BIT != 0)
            result += "$TH,"
        if (rule and FRIDAY_BIT != 0)
            result += "$FR,"
        if (rule and SATURDAY_BIT != 0)
            result += "$SA,"
        if (rule and SUNDAY_BIT != 0)
            result += "$SU,"
        return result.trimEnd(',')
    }

    private fun getDayLetters(dayOfWeek: Int) = when (dayOfWeek) {
        1 -> MO
        2 -> TU
        3 -> WE
        4 -> TH
        5 -> FR
        6 -> SA
        else -> SU
    }

    // from P0DT1H5M0S to 3900 (seconds)
    fun parseDurationSeconds(duration: String): Int {
        val weeks = getDurationValue(duration, "W")
        val days = getDurationValue(duration, "D")
        val hours = getDurationValue(duration, "H")
        val minutes = getDurationValue(duration, "M")
        val seconds = getDurationValue(duration, "S")

        val minSecs = 60
        val hourSecs = minSecs * 60
        val daySecs = hourSecs * 24
        val weekSecs = daySecs * 7

        return seconds + (minutes * minSecs) + (hours * hourSecs) + (days * daySecs) + (weeks * weekSecs)
    }

    private fun getDurationValue(duration: String, char: String) = Regex("[0-9]+(?=$char)").find(duration)?.value?.toInt() ?: 0

    // from 65 to P0DT1H5M0S
    fun getDurationCode(minutes: Long): String {
        var days = 0
        var hours = 0
        var remainder = minutes
        if (remainder >= DAY_MINUTES) {
            days = floor((remainder / DAY_MINUTES).toDouble()).toInt()
            remainder -= days * DAY_MINUTES
        }

        if (remainder >= 60) {
            hours = floor((remainder / 60).toDouble()).toInt()
            remainder -= hours * 60
        }

        return "P${days}DT${hours}H${remainder}M0S"
    }
}
