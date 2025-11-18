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
    // TODO: Need to re-evaluate the Range exception
    // Easter Dependent (single day exception): EXRRULE:EXT=EXP;EXCEPTION=D,39-E,PD-SMW,2-R,23,30
    // Easter Dependent (multiple day exception): EXRRULE:EXT=EXP;EXCEPTION=R,23,30-E,PD-SMW,2
    // Week Day Exception: EXRRULE: EXT=EXP;EXCEPTION=WD,SU-SPD,1
    //    - The week day cannot be greater than 31
    // Day of month Exception: EXRRULE: EXT=EXP;EXCEPTION=DM,24,GT-SMW,1
    // Day of month Dependent: EXRRULE: EXT=DEP;EXCEPTION=DD,1,6-SPW,2
    /* EXCEPTION Format:
     * The "-" separates the groups of data
     * D = Date of exception in ddmm or dd format
     *     If E, then it equals the number of days or weeks of event
     * DD = Day Dependent: The next yearly occurrence is always calculated from this value
     *     Format: DD, month, day
     *     Month cannot be greater than 12, and day cannot be greater than 31
     * DM = Day of Month (Int) The date cannot be larger than 31, and the month
     *     is the month of the current event.
     * E = Dependent on Easter:
     *     PD = Plus Days
     *     PW = Plus Weeks
     *     MD = Minus Days
     *     MW = Minus Weeks
     * WD = Day of Week (mon, tue, ...)
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
            if (keyValue.size <= 1) continue
            var buf: Int
            val key = keyValue[0]
            var value = keyValue[1]
            exceptionRule = when (key) {
                "D" -> { buf = if (value.toInt() > 255) continue else value.toInt().shl(RANGE_TO)
                        exceptionRule or buf }
                "E" -> when (value) {
                    PD -> exceptionRule or EX_RULE_EPD
                    PW -> exceptionRule or EX_RULE_EPW
                    MD -> exceptionRule or EX_RULE_EMD
                    MW -> exceptionRule or EX_RULE_EMW
                    else -> exceptionRule
                }
                "DD" -> { if (keyValue.size < 3) continue
                    buf = if (value.toInt() > 12) continue else value.toInt().shl(RANGE_FROM)
                    val extra = keyValue[2].toInt().shl(RANGE_TO)
                    exceptionRule or buf or extra
                }
                "DM" -> { buf = if (value.toInt() > 31) continue else value.toInt().shl(RANGE_TO)
                    var extra = 0
                    if (keyValue.size > 2) {
                        when (keyValue[2]) {
                            "GT" -> extra = EX_RULE_GT
                            "LT" -> extra = EX_RULE_LT
                        }
                    }
                    exceptionRule or buf or EX_RULE_DM or extra
                }
                "WD" -> when (value) {
                    MO -> exceptionRule or EX_RULE_MO or EX_RULE_W
                    TU -> exceptionRule or EX_RULE_TU or EX_RULE_W
                    WE -> exceptionRule or EX_RULE_WE or EX_RULE_W
                    TH -> exceptionRule or EX_RULE_TH or EX_RULE_W
                    FR -> exceptionRule or EX_RULE_FR or EX_RULE_W
                    SA -> exceptionRule or EX_RULE_SA or EX_RULE_W
                    else -> exceptionRule or EX_RULE_W
                }
                SPD -> { buf = if (value.toInt() > 31) continue else value.toInt().shl(SHIFT)
                    exceptionRule or EX_RULE_SPD or buf
                }
                SMD -> { buf = if (value.toInt() > 31) continue else value.toInt().shl(SHIFT)
                    exceptionRule or EX_RULE_SMD or buf
                }
                SPW -> { buf = if (value.toInt() > 31) continue else value.toInt().shl(SHIFT)
                    exceptionRule or EX_RULE_SPW or buf
                }
                SMW -> { buf = if (value.toInt() > 31) continue else value.toInt().shl(SHIFT)
                    exceptionRule or EX_RULE_SMW or buf
                }
                "R" -> { if (keyValue.size < 3) continue
                    buf = if (value.toInt() > 255) continue else value.toInt().shl(RANGE_FROM)
                    value = keyValue[2]
                    buf = if (value.toInt() > 255) continue else buf or (value.toInt().shl(RANGE_TO))
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
                EXT -> newRepeatRule = when (value) {
                    HOLY_NAME_JESUS -> REPEAT_HNOJ
                    EXP -> when (repeatRule) {
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> REPEAT_ORDER_WEEKDAY_USE_LAST_W_EXCEPTION
                        REPEAT_ORDER_WEEKDAY -> REPEAT_ORDER_WEEKDAY_WITH_EXCEPTION
                        REPEAT_SAME_DAY -> REPEAT_SAME_DAY_WITH_EXCEPTION
                        else -> repeatRule
                    }
                    DEP -> REPEAT_WEEKDAY_DEPENDENT
                    else -> repeatRule
                }
                FLAG -> newFlags = when (value) {
                    EXP -> newFlags or FLAG_EXCEPTION
                    FLAG_OFA -> newFlags or FLAG_FISH_OFA
                    FLAG_OA_TF -> newFlags or FLAG_FISH_OA_TF
                    FLAG_TFA -> newFlags or FLAG_FISH_TFA
                    FLAG_TA -> newFlags or FLAG_FISH_TA
                    FLAG_TFPA -> newFlags or FLAG_FISH_TFPA
                    else -> flags
                }
                EXCEPTION -> newExtendedRule = parseExceptionRule(value)
                FM_PLUS_DAYS -> newRepeatRule = RULE_ADD_DAYS + value.toInt()
                FM_PLUS_WEEKS -> newRepeatRule = RULE_ADD_WEEKS + value.toInt()
                FM_MINUS_DAYS -> newRepeatRule = RULE_MINUS_DAYS + value.toInt()
                FM_MINUS_WEEKS -> newRepeatRule = RULE_MINUS_WEEKS + value.toInt()
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
        repeatRule > RULE_MINUS_WEEKS -> FM_MINUS_WEEKS
        repeatRule > RULE_MINUS_DAYS -> FM_MINUS_DAYS
        repeatRule > RULE_ADD_WEEKS -> FM_PLUS_WEEKS
        else -> FM_PLUS_DAYS
    }

    private fun getExNumber(repeatRule: Int) = when {
        repeatRule > RULE_MINUS_WEEKS -> (RULE_MINUS_WEEKS xor repeatRule)
        repeatRule > RULE_MINUS_DAYS -> (RULE_MINUS_DAYS xor repeatRule)
        repeatRule > RULE_ADD_WEEKS -> (RULE_ADD_WEEKS xor repeatRule)
        else -> (RULE_ADD_DAYS xor repeatRule)
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
