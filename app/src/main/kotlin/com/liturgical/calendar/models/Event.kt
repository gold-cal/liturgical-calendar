package com.liturgical.calendar.models

import androidx.collection.LongSparseArray
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liturgical.calendar.extensions.getBits
import com.liturgical.calendar.extensions.seconds
import com.liturgical.calendar.helpers.*
import com.secure.commons.extensions.addBitIf
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.io.Serializable

@Entity(tableName = "events", indices = [(Index(value = ["id"], unique = true))])
data class Event(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "start_ts") var startTS: Long = 0L,
    @ColumnInfo(name = "end_ts") var endTS: Long = 0L,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "location") var location: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "reminder_1_minutes") var reminder1Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_2_minutes") var reminder2Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_3_minutes") var reminder3Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_1_type") var reminder1Type: Int = REMINDER_NOTIFICATION,
    @ColumnInfo(name = "reminder_2_type") var reminder2Type: Int = REMINDER_NOTIFICATION,
    @ColumnInfo(name = "reminder_3_type") var reminder3Type: Int = REMINDER_NOTIFICATION,
    @ColumnInfo(name = "repeat_interval") var repeatInterval: Int = 0,
    @ColumnInfo(name = "repeat_rule") var repeatRule: Int = 0,
    @ColumnInfo(name = "repeat_limit") var repeatLimit: Long = 0L,
    @ColumnInfo(name = "repetition_exceptions") var repetitionExceptions: List<String> = emptyList(),
    @ColumnInfo(name = "attendees") var attendees: String = "",
    @ColumnInfo(name = "import_id") var importId: String = "",
    @ColumnInfo(name = "time_zone") var timeZone: String = "",
    @ColumnInfo(name = "flags") var flags: Int = 0,
    @ColumnInfo(name = "event_type") var eventType: Long = REGULAR_EVENT_TYPE_ID,
    @ColumnInfo(name = "parent_id") var parentId: Long = 0,
    @ColumnInfo(name = "last_updated") var lastUpdated: Long = 0L,
    @ColumnInfo(name = "source") var source: String = SOURCE_DEFAULT_CALENDAR,
    @ColumnInfo(name = "availability") var availability: Int = 0,
    @ColumnInfo(name = "color") var color: Int = 0,
    @ColumnInfo(name = "type") var type: Int = TYPE_EVENT,
    @ColumnInfo(name = "extended_rule") var extendedRule: Int = 0
) : Serializable {

    /*companion object {
        private const val SERIAL_VERSION_UID = -32456795132345616L
    }*/

    fun addIntervalTime(original: Event) {
        val oldStart = Formatter.getDateTimeFromTS(startTS)
        val newStart = when (repeatInterval) {
            DAY -> oldStart.plusDays(1)
            else -> {
                when {
                    repeatInterval % YEAR == 0 -> when (repeatRule) {
                        REPEAT_SAME_DAY -> addYearsWithSameDay(oldStart)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(oldStart, original, true)
                        REPEAT_SAME_DAY_WITH_EXCEPTION -> handleYearlyException(oldStart, original)
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(oldStart, original, false)
                        REPEAT_ORDER_WEEKDAY_WITH_EXCEPTION -> handleYearlyException(oldStart, original)
                        REPEAT_AFTER_FM -> calculateEaster(oldStart)
                        REPEAT_HNOJ -> calculateHNOJ(oldStart)
                        REPEAT_ORDER_WEEKDAY_USE_LAST_W_EXCEPTION -> handleYearlyException(oldStart, original)
                        REPEAT_WEEKDAY_DEPENDENT -> calculateEventShift(oldStart, original)
                        else -> addYearWithRepeatRule(oldStart)
                    }
                    repeatInterval % MONTH == 0 -> when (repeatRule) {
                        REPEAT_SAME_DAY -> addMonthsWithSameDay(oldStart, original)
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(oldStart, original, false)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(oldStart, original, true)
                        else -> oldStart.plusMonths(repeatInterval / MONTH).dayOfMonth().withMaximumValue()
                    }
                    repeatInterval % WEEK == 0 -> {
                        // step through weekly repetition by days too, as events can trigger multiple times a week
                        oldStart.plusDays(1)
                    }
                    else -> oldStart.plusSeconds(repeatInterval)
                }
            }
        }

        val newStartTS = newStart.seconds()
        val newEndTS = newStartTS + (endTS - startTS)
        startTS = newStartTS
        endTS = newEndTS
    }
    
    /* Shifting the feasts after Epiphany to after Pentecost
    * NOTES:
    * Septuagesima is 9 weeks before Easter
    *   - If it is less than 3 weeks from Jan. 6, then the 3rd sunday after Epiphany is in Pentecost
    *   - Jan 6 -> Septuagesima < 3 weeks: 3, 4, 5, 6 sundays are in pentecost
    *   - Jan 6 -> Septuagesima < 4 weeks: 4, 5, 6 sundays are in pentecost
    *   - Jan 6 -> Septuagesima < 5 weeks: 5, 6 sundays are in pentecost
    *   - Jan 6 -> Septuagesima < 6 weeks: 6 sunday is in pentecost
    *   - Jan 6 -> Septuagesima < 7 weeks: no sundays in epiphany in pentecost
    *  Septuagesima is 9 weeks before Easter
    *  NOTE: When Epiphany lands on Thu, Fri, or Sat, there are enough sundays for all
    *        the Sundays in Epiphany, otherwise one of the sundays does not get said.
    *
    *         If the number of Sundays       |
    *             after Pentecost            |  The Mass is that of the:
    *     Is:   24    25    26    27    28   |
    * On the:                          24th  |  3rd after Epiphany
    * On the:                    24th  25th  |  4th after Epiphany
    * On the:              24th  25th  26th  |  5th after Epiphany
    * On the:        24th  25th  26th  27th  |  6th after Epiphany
    * On the:  24th  25th  26th  27th  28th  |  last after Pentecost
    */

    private fun shiftEpiphany(currStart: DateTime, weeks: Int, day: Int, epiphanyWeekDay: Int): DateTime {
        var newDateTime = currStart.minusYears(1)
        var checkDay = 0
        if (weeks < 3) {
            checkDay = day
        } else if (weeks < 4) { // weeks = 3, so 4th, 5th, and 6th are in pentecost
            checkDay = day - 1
        } else if (weeks < 5) { // weeks = 4, so 5th and 6th are in pentecost
            checkDay = day - 2
        } else if (weeks < 6) {
            checkDay = day - 3
        }
        if (epiphanyWeekDay != 4 && epiphanyWeekDay != 5 && epiphanyWeekDay != 6) // Thu, Fri, Sat
            checkDay--

        newDateTime = when (checkDay) {
            3 -> { description += " ($title)"
                title = "Twenty-Fourth Sunday after Pentecost" // 0x41F
                addYearWithRepeatRule(newDateTime, 0x41F) }
            4 -> { description += " ($title)"
                title = "Twenty-Fifth Sunday after Pentecost" // 0x420
                addYearWithRepeatRule(newDateTime, 0x420) }
            5 -> { description += " ($title)"
                title = "Twenty-Sixth Sunday after Pentecost" // 0x421
                addYearWithRepeatRule(newDateTime, 0x421) }
            6 -> { description += " ($title)"
                title = "Twenty-Seventh Sunday after Pentecost" // 0x422
                addYearWithRepeatRule(newDateTime, 0x422) }
            else -> {
                // remove exception flag so it checks that it is landing on the
                // correct date the flowing year
                flags = flags xor FLAG_EXCEPTION
                shiftDateDependentEvent(currStart)
            }
        }
        return newDateTime
    }

    /* How the Extended rule is used in this function (Refer to Constants.kt for more info)
     * 32                23          17              9               1
     * 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1
     *        W           | d shift |     month     |     date      |
     * If W is set, then the days are dependent on the day of week they land on otherwise
     * if W is not set, they stay on their normal date.
     */
    private fun shiftDateDependentEvent(currStart: DateTime): DateTime {
        val day = extendedRule.getBits(XOR_RANGE_TO).ushr(RANGE_TO)
        val month = extendedRule.getBits(XOR_RANGE_FROM).ushr(RANGE_FROM)
        //val origDay = Formatter.getDateTimeFromTS(original.startTS)
        var dependentDate = DateTime(currStart.year + 1, month, day, 5, 0)
        val daysToSunday = dependentDate.dayOfWeek
        if (daysToSunday != 7) dependentDate = dependentDate.minusDays(daysToSunday)

        /*if ((extendedRule and EX_RULE_W) != 0) {
            newDateTime = addXthDayInterval(currStart, original, false)
        } else {
            newDateTime = addYearsWithSameDay(newDateTime)
        }*/

        return handleShiftBit(dependentDate)
    }

    private fun calculateEventShift(currStart: DateTime, original: Event): DateTime {
        var newDateTime = shiftDateDependentEvent(currStart)

        if ((flags and FLAG_EXCEPTION) == FLAG_EXCEPTION) {
            val recalculate = false
            while (!recalculate) {
                // check and make sure the title is set back to original
                if (title != original.title) title = original.title
                if (description != original.description) description = original.description
                // calculate the number of weeks to Septuagesima
                var janSix = DateTime(newDateTime.year, 1, 6, 5, 0)
                val epiphanyWeekDay = janSix.dayOfWeek
                val septuagesimaRule = RULE_MINUS_WEEKS + 9
                val septuagesima = addYearWithRepeatRule(newDateTime.minusYears(1), septuagesimaRule)
                var days = 7 - janSix.dayOfWeek
                if (days != 0) janSix = janSix.plusDays(days)
                var weeks = 0
                var isFinished = false
                while (!isFinished) {
                    weeks++
                    janSix = janSix.plusWeeks(1)
                    if (janSix.monthOfYear == septuagesima.monthOfYear) {
                        if (janSix.dayOfMonth == septuagesima.dayOfMonth)
                            isFinished = true
                    }
                }

                if (weeks > 5) return newDateTime

                val key = title.split(" ")
                days = when (key[0]) {
                    "Third" -> 3
                    "Fourth" -> 4
                    "Fifth" -> 5
                    "Sixth" -> 6
                    else -> 7
                }

                // if the number of weeks after epiphany is less than the sunday after epiphany (days)
                // then the sunday is in pentecost or not said
                if (weeks >= days) return newDateTime

                newDateTime = shiftEpiphany(newDateTime, weeks, days, epiphanyWeekDay)
                // if the FLAG_EXCEPTION is not set, recalculate date
                if ((flags and FLAG_EXCEPTION) != FLAG_EXCEPTION) {
                    // reset flag
                    flags = flags or FLAG_EXCEPTION
                } else return newDateTime
            }
        }
        return newDateTime
    }

    private fun shiftDate(currStart: DateTime, shiftBit: Int, shiftValue: Int): DateTime {
        val newDateTime = when (shiftBit) {
            EX_RULE_SPD -> currStart.plusDays(shiftValue)
            EX_RULE_SMD -> currStart.minusDays(shiftValue)
            EX_RULE_SPW -> currStart.plusWeeks(shiftValue)
            EX_RULE_SMW -> currStart.minusWeeks(shiftValue)
            else -> currStart
        }
        return newDateTime
    }

    private fun handleShiftBit(currStart: DateTime, reverseRule: Boolean = false): DateTime {
        val shift = extendedRule.getBits(XOR_SHIFT).ushr(SHIFT)
        var shiftBit = EX_RULE_SPD
        var isFinished = false
        var newDateTime = currStart
        while (!isFinished) {
            if (reverseRule) {
                shiftBit = if ((extendedRule and EX_RULE_SPD) != 0) EX_RULE_SMD
                else if ((extendedRule and EX_RULE_SMD) != 0) EX_RULE_SPD
                else if ((extendedRule and EX_RULE_SPW) != 0) EX_RULE_SMW
                else if ((extendedRule and EX_RULE_SMW) != 0) EX_RULE_SPW
                else return currStart
                isFinished = true
                newDateTime = shiftDate(currStart, shiftBit, shift)
            } else {
                // if the bit is not set check the next condition
                if ((extendedRule and shiftBit) == 0) {
                    shiftBit = shiftBit.shl(1)
                    // if the bit is greater then SMW, no bit is set, so return
                    if (shiftBit > EX_RULE_SMW) return currStart
                } else {
                    isFinished = true
                    newDateTime = shiftDate(currStart, shiftBit, shift)
                }
            }
        }
        return newDateTime
    }

    private fun handleYearlyException(currStart: DateTime, original: Event): DateTime {
        var newDateTime = addYearsWithSameDay(currStart)
        var normalDateTime = currStart
        val origDay = Formatter.getDateTimeFromTS(original.startTS)
        // check and make sure the date is the same as the original event
        if ((flags and FLAG_EXCEPTION) == FLAG_EXCEPTION) {
            if (repeatRule == REPEAT_SAME_DAY_WITH_EXCEPTION) { //currStart.monthOfYear != origDay.monthOfYear || currStart.dayOfMonth != origDay.dayOfMonth)
                val yearShift = newDateTime.year - origDay.year
                newDateTime = origDay.plusYears(yearShift)
            } else {
                normalDateTime = handleShiftBit(currStart, true)
            }
            // remove exception flag
            flags = flags xor FLAG_EXCEPTION
        }

        if (repeatRule != REPEAT_SAME_DAY_WITH_EXCEPTION) {
            newDateTime = when (repeatRule) {
                REPEAT_ORDER_WEEKDAY_WITH_EXCEPTION -> addXthDayInterval(normalDateTime, original, false)
                REPEAT_ORDER_WEEKDAY_USE_LAST_W_EXCEPTION -> addXthDayInterval(normalDateTime, original, true)
                else -> newDateTime
            }
        }
        var isException = false
        var date = extendedRule.getBits(XOR_RANGE_TO).ushr(RANGE_TO)
        // Check if it is a range
        if ((extendedRule and EX_RULE_R) != 0) {
            var from = extendedRule.getBits(XOR_RANGE_FROM).ushr(RANGE_FROM)
            /** TODO: Need to add range check*/
        } else {
            // check if exception day lands on the same day as newDateTime
            // these are in a specific order
            if ((extendedRule and EX_RULE_W) != 0) {
                if (date == 0) date = 7
                if (newDateTime.dayOfWeek == date) isException = true
            } else if ((extendedRule and EX_RULE_DM) != 0) {
                if ((extendedRule and EX_RULE_GT) != 0) {
                    if (newDateTime.dayOfMonth > date) isException = true
                } else if ((extendedRule and EX_RULE_LT) != 0) {
                    if (newDateTime.dayOfMonth < date) isException = true
                } else if (newDateTime.dayOfMonth == date) isException = true
            } else if ((extendedRule and EX_RULE_EPD) != 0) {
                val ePD = addYearWithRepeatRule(currStart, (date or RULE_ADD_DAYS))
                if (newDateTime == ePD) isException = true
            } else if ((extendedRule and EX_RULE_EMD) != 0) {
                val eMD = addYearWithRepeatRule(currStart, (date or RULE_MINUS_DAYS))
                if (newDateTime == eMD) isException = true
            } else if ((extendedRule and EX_RULE_EPW) != 0) {
                val ePW = addYearWithRepeatRule(currStart, (date or RULE_ADD_WEEKS))
                if (newDateTime == ePW) isException = true
            } else if ((extendedRule and EX_RULE_EMW) != 0) {
                val eMW = addYearWithRepeatRule(currStart, (date or RULE_MINUS_WEEKS))
                if (newDateTime == eMW) isException = true
            }
        }
        if (isException) {
            newDateTime = handleShiftBit(newDateTime)
            flags = flags or FLAG_EXCEPTION
        }
        return newDateTime
    }

    // if an event should happen on 29th Feb. with Same Day yearly repetition, show it only on leap years
    private fun addYearsWithSameDay(currStart: DateTime): DateTime {
        var newDateTime = getNextYearlyOccurrence(currStart) //currStart.plusYears(repeatInterval / YEAR)

        // Date may slide within the same month
        if (newDateTime.dayOfMonth != currStart.dayOfMonth) {
            while (newDateTime.dayOfMonth().maximumValue < currStart.dayOfMonth) {
                newDateTime = getNextYearlyOccurrence(currStart) //newDateTime.plusYears(repeatInterval / YEAR)
            }
            newDateTime = newDateTime.withDayOfMonth(currStart.dayOfMonth)
        }
        return newDateTime
    }

    // if the first sunday of jan lands on the 1,6 or 7, then it is on the 2
    // otherwise it is on the first sunday
    private fun calculateHNOJ(currStart: DateTime): DateTime {
        val year = currStart.year + 1
        var newDateTime = DateTime(year, 1, 1, 0, 0)
        val day = newDateTime.dayOfWeek
        newDateTime = when (day) {
            3 -> newDateTime.plusDays(4)
            4 -> newDateTime.plusDays(3)
            5 -> newDateTime.plusDays(2)
            else -> newDateTime.plusDays(1)
        }
        return newDateTime
    }

    // the repeat rule contains information on how to calculate the date repetition
    private fun addYearWithRepeatRule(currStart: DateTime, customRepeatRule: Int = 0): DateTime {
        val newRepeatRule = if (customRepeatRule == 0) repeatRule else customRepeatRule
        var newDateTime = calculateEaster(currStart)

        // check and make sure there are more than 23 weeks in pentecost
        if (title == "Twenty-Third Sunday after Pentecost") {
            // Pentecost is 7 weeks after Easter add 23 weeks, and see if it land after the 19th of november
            // 7 + 23 = 30
            val weeksInPentecost = newDateTime.plusWeeks(30)
            if (weeksInPentecost.monthOfYear == 11 && weeksInPentecost.dayOfMonth > 18) {
                // move it to the following year
                newDateTime = calculateEaster(newDateTime)
            }
        }

        var rule = newRepeatRule
        // have to keep these in this order largest to smallest
        if (newRepeatRule > RULE_MINUS_WEEKS) {
            rule = rule xor RULE_MINUS_WEEKS
            newDateTime = newDateTime.minusWeeks(rule)
        } else if (newRepeatRule > RULE_MINUS_DAYS) {
            rule = rule xor RULE_MINUS_DAYS
            newDateTime = newDateTime.minusDays(rule)
        } else if (newRepeatRule > RULE_ADD_WEEKS) {
            rule = rule xor RULE_ADD_WEEKS
            newDateTime = newDateTime.plusWeeks(rule)
        } else if (newRepeatRule > RULE_ADD_DAYS) {
            rule = rule xor RULE_ADD_DAYS
            newDateTime = newDateTime.plusDays(rule)
        }

        return newDateTime
    }

    private fun getDayInt(day: String): Int {
        return when (day) {
            "Mon" -> 1
            "Tue" -> 2
            "Wed" -> 3
            "Thu" -> 4
            "Fri" -> 5
            "Sat" -> 6
            else -> {
                0
            }
        }
    }

    private fun isLeapYear(year: Int) : Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun getNextYearlyOccurrence(currStart: DateTime): DateTime {
        return currStart.plusYears(repeatInterval / YEAR)
    }

    private fun calculateEaster(currStart: DateTime): DateTime{
        // Calculate when Easter is to determine everything else
        // The full moon of 2023 is Apl 5 @ 23:37, Wed = 4 (will use military time)
        val fullMoon = DateTime(2023, 4, 5, 23, 37)
        if (currStart.year < fullMoon.year) return currStart

        var lastFullMoon = fullMoon
        /** Check if we should calculate the next easter cycle **/
        val fullMoonDayTitle = Formatter.getDayOfWeek(fullMoon)
        var fullMoonDay = getDayInt(fullMoonDayTitle)
        var finished = false

        /* Now equinox to equinox is 365 days if not a leap year
        ** To calculate the number of days from the last full moon to the next spring equinox
        ** we need to subtract the days between the first equinox and the first full moon.
        **  Last full moon          Easter         Spring Equinox       Next Full Moon        Easter
        **        |--------------------|------------------|--------------------|-------------------|
        ** 1 leap year = 366 days - 31 = 335 days + March 21 = 356 days
        ** 1 year = 365 days - month of March (31 days) = 334 days + March 21 = 355 days
        ** April 1 -> March 21 = 355 days
        ** Days in 1 moon cycle: 29.53058770576 days = 2551442 sec */
        var nextFullMoonYear = lastFullMoon.year
        var easterDay = DateTime(fullMoon.year, fullMoon.monthOfYear, fullMoon.dayOfMonth, 5, 0)
        var daysToEaster = 7 - fullMoonDay
        // Easter is the first Sunday after the first full moon from the spring equinox (March 21)
        easterDay = easterDay.plusDays(daysToEaster)

        while (!finished) {
            // Need to calculate days from spring equinox to full moon
            val daysFromEquinoxToFullMoon = if (lastFullMoon.monthOfYear == 4) {
                10 + lastFullMoon.dayOfMonth
            } else {
                lastFullMoon.dayOfMonth - 21
            }

            nextFullMoonYear++
            // need to calculate when the next full moon is
            // Number of days from Jan 1 to March 21
            val janToEquinox = if (isLeapYear(nextFullMoonYear)) 81 else 80
            // Total days from last full moon to spring equinox
            val daysFromLastFullMoonToEquinox = 285 + janToEquinox - daysFromEquinoxToFullMoon  // -21 + 306
            // now need to convert to seconds
            val secondsToEquinox = daysFromLastFullMoonToEquinox * 86400 //* 24 * 60 *60
            // Now Calculate the time from the spring equinox to the next full moon
            var secondsToNextFullMoon = 0
            while (secondsToNextFullMoon < secondsToEquinox) {
                secondsToNextFullMoon += SECONDS_IN_MOON_CYCLE
            }
            // Now add this to the last full moon
            lastFullMoon = lastFullMoon.plusSeconds(secondsToNextFullMoon)
            fullMoonDay = getDayInt(Formatter.getDayOfWeek(lastFullMoon))
            easterDay = DateTime(lastFullMoon.year, lastFullMoon.monthOfYear, lastFullMoon.dayOfMonth, 5, 0)
            daysToEaster = 7 - fullMoonDay
            // Easter is the first Sunday after the first full moon from the spring equinox (March 21)
            easterDay = easterDay.plusDays(daysToEaster)
            if (nextFullMoonYear == (currStart.year + 1)) {
                finished = true
            }
        }

        return easterDay
    }

    // if an event should happen on 31st with Same Day monthly repetition, don't show it at all at months with 30 or less days
    private fun addMonthsWithSameDay(currStart: DateTime, original: Event): DateTime {
        var newDateTime = currStart.plusMonths(repeatInterval / MONTH)
        if (newDateTime.dayOfMonth == currStart.dayOfMonth) {
            return newDateTime
        }

        while (newDateTime.dayOfMonth().maximumValue < Formatter.getDateTimeFromTS(original.startTS).dayOfMonth().maximumValue) {
            newDateTime = newDateTime.plusMonths(repeatInterval / MONTH)
            newDateTime = try {
                newDateTime.withDayOfMonth(currStart.dayOfMonth)
            } catch (e: Exception) {
                newDateTime
            }
        }
        return newDateTime
    }



    // handle monthly repetitions like Third Monday
    private fun addXthDayInterval(currStart: DateTime, original: Event, forceLastWeekday: Boolean): DateTime {
        val day = currStart.dayOfWeek
        var order = (currStart.dayOfMonth - 1) / 7
        var properMonth = currStart.withDayOfMonth(7).plusMonths(repeatInterval / MONTH).withDayOfWeek(day)
        var wantedDay: Int

        // check if it should be for example Fourth Monday, or Last Monday
        if (forceLastWeekday && (order == 3 || order == 4)) {
            val originalDateTime = Formatter.getDateTimeFromTS(original.startTS)
            val isLastWeekday = originalDateTime.monthOfYear != originalDateTime.plusDays(7).monthOfYear
            if (isLastWeekday)
                order = -1
        }

        if (order == -1) {
            wantedDay = properMonth.dayOfMonth + ((properMonth.dayOfMonth().maximumValue - properMonth.dayOfMonth) / 7) * 7
        } else {
            wantedDay = properMonth.dayOfMonth + (order - (properMonth.dayOfMonth - 1) / 7) * 7
            while (properMonth.dayOfMonth().maximumValue < wantedDay) {
                properMonth = properMonth.withDayOfMonth(7).plusMonths(repeatInterval / MONTH).withDayOfWeek(day)
                wantedDay = properMonth.dayOfMonth + (order - (properMonth.dayOfMonth - 1) / 7) * 7
            }
        }

        return properMonth.withDayOfMonth(wantedDay)
    }

    /************ Public Functions ************/
    /******************************************/

    fun getIsAllDay() = flags and FLAG_ALL_DAY != 0
    fun hasMissingYear() = flags and FLAG_MISSING_YEAR != 0
    fun isTask() = type == TYPE_TASK
    fun isTaskCompleted() = isTask() && flags and FLAG_TASK_COMPLETED != 0

    fun getReminders() = listOf(
        Reminder(reminder1Minutes, reminder1Type),
        Reminder(reminder2Minutes, reminder2Type),
        Reminder(reminder3Minutes, reminder3Type)
    ).filter { it.minutes != REMINDER_OFF }

    // properly return the start time of all-day events as midnight
    fun getEventStartTS(): Long {
        return if (getIsAllDay()) {
            Formatter.getDateTimeFromTS(startTS).withTime(0, 0, 0, 0).seconds()
        } else {
            startTS
        }
    }

    fun getCalDAVEventId(): Long {
        return try {
            (importId.split("-").lastOrNull() ?: "0").toString().toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    fun getCalDAVCalendarId() = if (source.startsWith(CALDAV)) (source.split("-").lastOrNull() ?: "0").toString().toInt() else 0

    // check if it's the proper week, for events repeating every x weeks
    // get the week number since 1970, not just in the current year
    fun isOnProperWeek(startTimes: LongSparseArray<Long>): Boolean {
        val initialWeekNumber = Formatter.getDateTimeFromTS(startTimes[id!!]!!).withTimeAtStartOfDay().millis / (7 * 24 * 60 * 60 * 1000f)
        val currentWeekNumber = Formatter.getDateTimeFromTS(startTS).withTimeAtStartOfDay().millis / (7 * 24 * 60 * 60 * 1000f)
        return (Math.round(initialWeekNumber) - Math.round(currentWeekNumber)) % (repeatInterval / WEEK) == 0
    }

    fun updateIsPastEvent() {
        val endTSToCheck = if (startTS < getNowSeconds() && getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(endTS))
        } else {
            endTS
        }
        isPastEvent = endTSToCheck < getNowSeconds()
    }

    fun addRepetitionException(dayCode: String) {
        var newRepetitionExceptions = repetitionExceptions.toMutableList()
        newRepetitionExceptions.add(dayCode)
        newRepetitionExceptions = newRepetitionExceptions.distinct().toMutableList() as ArrayList<String>
        repetitionExceptions = newRepetitionExceptions
    }

    var isPastEvent: Boolean
        get() = flags and FLAG_IS_IN_PAST != 0
        set(isPastEvent) {
            flags = flags.addBitIf(isPastEvent, FLAG_IS_IN_PAST)
        }

    fun getTimeZoneString(): String {
        return if (timeZone.isNotEmpty() && getAllTimeZones().map { it.zoneName }.contains(timeZone)) {
            timeZone
        } else {
            DateTimeZone.getDefault().id
        }
    }
}
