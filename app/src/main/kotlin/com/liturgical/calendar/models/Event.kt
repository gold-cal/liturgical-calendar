package com.liturgical.calendar.models

import androidx.collection.LongSparseArray
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liturgical.calendar.extensions.config
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
    @ColumnInfo(name = "repetition_exceptions") var repetitionExceptions: ArrayList<String> = ArrayList(),
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
    @ColumnInfo(name = "type") var type: Int = TYPE_EVENT
) : Serializable {

    companion object {
        private const val serialVersionUID = -32456795132345616L
    }

    fun addIntervalTime(original: Event) {
        val oldStart = Formatter.getDateTimeFromTS(startTS)
        val newStart = when (repeatInterval) {
            DAY -> oldStart.plusDays(1)
            else -> {
                when {
                    repeatInterval % YEAR == 0 -> when (repeatRule) {
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(oldStart, original, false)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(oldStart, original, true)
                        REPEAT_AFTER_FM -> calculateFullMoon(oldStart)
                        else -> addYearsWithSameDay(oldStart)
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

    // if an event should happen on 29th Feb. with Same Day yearly repetition, show it only on leap years
    private fun addYearsWithSameDay(currStart: DateTime): DateTime {
        var newDateTime = currStart.plusYears(repeatInterval / YEAR)

        // Date may slide within the same month
        if (newDateTime.dayOfMonth != currStart.dayOfMonth) {
            while (newDateTime.dayOfMonth().maximumValue < currStart.dayOfMonth) {
                newDateTime = newDateTime.plusYears(repeatInterval / YEAR)
            }
            newDateTime = newDateTime.withDayOfMonth(currStart.dayOfMonth)
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

    private fun calculateFullMoon(currStart: DateTime): DateTime{
        // Calculate when Easter is to determine everything else
        // The full moon of 2023 is Apl 5 @ 23:37, Wed = 4 (will use military time)
        val fullMoon = DateTime(2023, 4, 5, 23, 37)
        //val today = DateTime.now()
        /*if (config.isFirstCalc) {
            config.lastCalculatedFullMoon = fullMoon.seconds()
        }*/
        var lastFullMoon = fullMoon
        /** Check if we should calculate the next easter cycle **/
        //if (lastFullMoon.year == (today.year + 2)) return
        val fullMoonDayTitle = Formatter.getDayOfWeek(fullMoon)
        var fullMoonDay = getDayInt(fullMoonDayTitle)
        /*if (fullMoonDay == 8) {
            toast("Failed to get day of week", Toast.LENGTH_LONG)
            return
        }*/
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
        var easterDay = DateTime(fullMoon.year, fullMoon.monthOfYear, fullMoon.dayOfMonth, 0, 0)
        var daysToEaster = 7 - fullMoonDay
        // Easter is the first Sunday after the first full moon from the spring equinox (March 21)
        easterDay = easterDay.plusDays(daysToEaster)

        while (!finished) { //(today.year)) {

            //addLiturgicalEvent(true, "Easter", "", easterDay.seconds())
            // TODO: add other liturgical events

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
            easterDay = DateTime(lastFullMoon.year, lastFullMoon.monthOfYear, lastFullMoon.dayOfMonth, 0, 0)
            daysToEaster = 7 - fullMoonDay
            // Easter is the first Sunday after the first full moon from the spring equinox (March 21)
            easterDay = easterDay.plusDays(daysToEaster)
            if (nextFullMoonYear == (currStart.year + 1)) {
                finished = true
            }
        }
        // save the last calculated full moon
        //config.lastCalculatedFullMoon = lastFullMoon.seconds()
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

    fun addRepetitionException(daycode: String) {
        var newRepetitionExceptions = repetitionExceptions
        newRepetitionExceptions.add(daycode)
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
