package com.liturgical.calendar.helpers

import android.provider.CalendarContract.Events
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.extensions.eventsDB
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.extensions.isXYearlyRepetition
import com.liturgical.calendar.helpers.IcsImporter.ImportResult.IMPORT_FAIL
import com.liturgical.calendar.helpers.IcsImporter.ImportResult.IMPORT_NOTHING_NEW
import com.liturgical.calendar.helpers.IcsImporter.ImportResult.IMPORT_OK
import com.liturgical.calendar.helpers.IcsImporter.ImportResult.IMPORT_PARTIAL
import com.liturgical.calendar.models.Event
import com.liturgical.calendar.models.EventRepetition
import com.liturgical.calendar.models.EventType
import com.liturgical.calendar.models.Reminder
import com.secure.commons.extensions.areDigitsOnly
import com.secure.commons.extensions.showErrorToast
import com.secure.commons.helpers.HOUR_SECONDS
import org.joda.time.DateTimeZone
import java.io.File

class IcsImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL, IMPORT_NOTHING_NEW
    }

    private var curStart = -1L
    private var curEnd = -1L
    private var curTitle = ""
    private var curLocation = ""
    private var curDescription = ""
    private var curImportId = ""
    private var curRecurrenceDayCode = ""
    private var curRrule = ""
    private var curExRrule = ""
    private var curFlags = 0
    private var curReminderMinutes = ArrayList<Int>()
    private var curReminderActions = ArrayList<Int>()
    private var curRepeatExceptions = ArrayList<String>()
    private var curRepeatInterval = 0
    private var curRepeatLimit = 0L
    private var curRepeatRule = 0
    private var curEventTypeId = REGULAR_EVENT_TYPE_ID
    private var curLastModified = 0L
    private var curCategoryColor = -2
    private var curExtendedRule = 0
    private var curAvailability = Events.AVAILABILITY_BUSY
    private var isNotificationDescription = false
    private var isProperReminderAction = false
    private var isSequence = false
    private var isParsingEvent = false
    private var curReminderTriggerMinutes = REMINDER_OFF
    private var curReminderTriggerAction = REMINDER_NOTIFICATION
    private val eventsHelper = activity.eventsHelper

    private var eventsImported = 0
    private var eventsFailed = 0
    private var eventsAlreadyExist = 0

    fun importEvents(
        liturgical: Boolean,
        path: String,
        defaultEventTypeId: Long,
        calDAVCalendarId: Int,
        overrideFileEventTypes: Boolean,
        eventReminders: ArrayList<Int>? = null,
    ): ImportResult {
        try {
            val eventTypes = eventsHelper.getEventTypesSync()
            val existingEvents = activity.eventsDB.getEventsWithImportIds().toMutableList() as ArrayList<Event>
            val eventsToInsert = ArrayList<Event>()
            var line = ""

            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                activity.assets.open(path)
            }

            inputStream.bufferedReader().use { it ->
                while (true) {
                    val curLine = it.readLine() ?: break
                    if (curLine.trim().isEmpty()) {
                        continue
                    }

                    if (curLine.startsWith("\t") || curLine.substring(0, 1) == " ") {
                        line += curLine.removePrefix("\t").removePrefix(" ")
                        continue
                    }

                    if (line.trim() == BEGIN_EVENT) {
                        resetValues()
                        curEventTypeId = defaultEventTypeId
                        isParsingEvent = true
                    } else if (line.startsWith(DTSTART)) {
                        if (isParsingEvent) {
                            curStart = getTimestamp(line.substring(DTSTART.length))

                            if (curRrule != "") parseRepeatRule()
                            if (curExRrule != "") parseExtendedRule()
                        }
                    } else if (line.startsWith(DTEND)) {
                        curEnd = getTimestamp(line.substring(DTEND.length))
                    } else if (line.startsWith(DURATION)) {
                        val duration = line.substring(DURATION.length)
                        curEnd = curStart + Parser().parseDurationSeconds(duration)
                    } else if (line.startsWith(SUMMARY) && !isNotificationDescription) {
                        curTitle = line.substring(SUMMARY.length)
                        curTitle = getTitle(curTitle).replace("\\n", "\n").replace("\\,", ",")
                    } else if (line.startsWith(DESCRIPTION) && !isNotificationDescription) {
                        curDescription = line.substring(DESCRIPTION.length).replace("\\n", "\n").replace("\\,", ",")
                        if (curDescription.trim().isEmpty()) {
                            curDescription = ""
                        }
                    } else if (line.startsWith(UID)) {
                        curImportId = line.substring(UID.length).trim()
                    } else if (line.startsWith(RRULE)) {
                        curRrule = line.substring(RRULE.length)
                        // some RRULEs need to know the events start datetime. If it's yet unknown, postpone RRULE parsing
                        if (curStart != -1L) {
                            parseRepeatRule()
                        }
                    } else if (line.startsWith(EXRRULE)) {
                        curExRrule = line.substring(EXRRULE.length)
                        // only parse if RRULE has been parsed
                        if (curRepeatInterval != 0 && curRepeatInterval.isXYearlyRepetition()) {
                            parseExtendedRule()
                        }
                    } else if (line.startsWith(ACTION)) {
                        val action = line.substring(ACTION.length).trim()
                        isProperReminderAction = action == DISPLAY || action == EMAIL
                        if (isProperReminderAction) {
                            curReminderTriggerAction = if (action == DISPLAY) REMINDER_NOTIFICATION else REMINDER_EMAIL
                        }
                    } else if (line.startsWith(TRIGGER)) {
                        val value = line.substringAfterLast(":")
                        curReminderTriggerMinutes = Parser().parseDurationSeconds(value) / 60
                        if (!value.startsWith("-")) {
                            curReminderTriggerMinutes *= -1
                        }
                    } else if (line.startsWith(CATEGORY_COLOR_LEGACY)) {
                        val color = line.substring(CATEGORY_COLOR_LEGACY.length)
                        if (color.trimStart('-').areDigitsOnly()) {
                            curCategoryColor = Integer.parseInt(color)
                        }
                    } else if (line.startsWith(CATEGORY_COLOR)) {
                        val color = line.substring(CATEGORY_COLOR.length)
                        if (color.trimStart('-').areDigitsOnly()) {
                            curCategoryColor = Integer.parseInt(color)
                        }
                    } else if (line.startsWith(MISSING_YEAR)) {
                        if (line.substring(MISSING_YEAR.length) == "1") {
                            curFlags = curFlags or FLAG_MISSING_YEAR
                        }
                    } else if (line.startsWith(CATEGORIES) && !overrideFileEventTypes) {
                        val categories = line.substring(CATEGORIES.length)
                        tryAddCategories(categories, defaultEventTypeId)
                    } else if (line.startsWith(LAST_MODIFIED)) {
                        curLastModified = getTimestamp(line.substring(LAST_MODIFIED.length)) * 1000L
                    } else if (line.startsWith(EXDATE)) {
                        var value = line.substring(EXDATE.length)
                        if (value.endsWith('}')) {
                            value = value.substring(0, value.length - 1)
                        }

                        if (value.contains(",")) {
                            value.split(",").forEach { exdate ->
                                curRepeatExceptions.add(Formatter.getDayCodeFromTS(getTimestamp(exdate)))
                            }
                        } else {
                            curRepeatExceptions.add(Formatter.getDayCodeFromTS(getTimestamp(value)))
                        }
                    } else if (line.startsWith(LOCATION)) {
                        curLocation = getLocation(line.substring(LOCATION.length).replace("\\,", ","))
                        if (curLocation.trim().isEmpty()) {
                            curLocation = ""
                        }
                    } else if (line.startsWith(RECURRENCE_ID)) {
                        val timestamp = getTimestamp(line.substring(RECURRENCE_ID.length))
                        curRecurrenceDayCode = Formatter.getDayCodeFromTS(timestamp)
                    } else if (line.startsWith(SEQUENCE)) {
                        isSequence = true
                    } else if (line.startsWith(TRANSP)) {
                        line.substring(TRANSP.length).let { curAvailability = if (it == TRANSPARENT) Events.AVAILABILITY_FREE else Events.AVAILABILITY_BUSY }
                    } else if (line.trim() == BEGIN_ALARM) {
                        isNotificationDescription = true
                    } else if (line.trim() == END_ALARM) {
                        if (isProperReminderAction && curReminderTriggerMinutes != REMINDER_OFF) {
                            curReminderMinutes.add(curReminderTriggerMinutes)
                            curReminderActions.add(curReminderTriggerAction)
                        }
                        isNotificationDescription = false
                    } else if (line.trim() == END_EVENT) {
                        isParsingEvent = false
                        if (curStart != -1L && curEnd == -1L) {
                            curEnd = curStart
                        }

                        if (curTitle.isEmpty() || curStart == -1L) {
                            line = curLine
                            continue
                        }

                        // repeating event exceptions can have the same import id as their parents, so pick the latest event to update
                        val eventToUpdate =
                            existingEvents.filter { curImportId.isNotEmpty() && curImportId == it.importId }.sortedByDescending { it.lastUpdated }.firstOrNull()
                        if (eventToUpdate != null && eventToUpdate.lastUpdated >= curLastModified) {
                            eventsAlreadyExist++
                            line = curLine
                            continue
                        }

                        var reminders = eventReminders?.map { reminderMinutes -> Reminder(reminderMinutes, REMINDER_NOTIFICATION) } ?: arrayListOf(
                            Reminder(curReminderMinutes.getOrElse(0) { REMINDER_OFF }, curReminderActions.getOrElse(0) { REMINDER_NOTIFICATION }),
                            Reminder(curReminderMinutes.getOrElse(1) { REMINDER_OFF }, curReminderActions.getOrElse(1) { REMINDER_NOTIFICATION }),
                            Reminder(curReminderMinutes.getOrElse(2) { REMINDER_OFF }, curReminderActions.getOrElse(2) { REMINDER_NOTIFICATION })
                        )

                        reminders = reminders.sortedBy { it.minutes }.sortedBy { it.minutes == REMINDER_OFF }.toMutableList() as ArrayList<Reminder>

                        val eventType = eventTypes.firstOrNull { it.id == curEventTypeId }
                        val source = if (liturgical) SOURCE_LITURGICAL_CALENDAR
                                else if (calDAVCalendarId == 0 || eventType?.isSyncedEventType() == false) SOURCE_IMPORTED_ICS
                                else "$CALDAV-$calDAVCalendarId"
                        val event = Event(
                            null,
                            curStart,
                            curEnd,
                            curTitle,
                            curLocation,
                            curDescription,
                            reminders[0].minutes,
                            reminders[1].minutes,
                            reminders[2].minutes,
                            reminders[0].type,
                            reminders[1].type,
                            reminders[2].type,
                            curRepeatInterval,
                            curRepeatRule,
                            curRepeatLimit,
                            curRepeatExceptions,
                            "",
                            curImportId,
                            DateTimeZone.getDefault().id,
                            curFlags,
                            curEventTypeId,
                            0,
                            curLastModified,
                            source,
                            curAvailability,
                            extendedRule = curExtendedRule
                        )

                        if (event.getIsAllDay() && curEnd > curStart) {
                            event.endTS -= DAY

                            // fix some glitches related to daylight saving shifts
                            if (event.startTS - event.endTS == HOUR_SECONDS.toLong()) {
                                event.endTS += HOUR_SECONDS
                            } else if (event.startTS - event.endTS == -HOUR_SECONDS.toLong()) {
                                event.endTS -= HOUR_SECONDS
                            }
                        }

                        if (event.importId.isEmpty()) {
                            event.importId = event.hashCode().toString()
                            if (existingEvents.map { it.importId }.contains(event.importId)) {
                                eventsAlreadyExist++
                                line = curLine
                                continue
                            }
                        }

                        if (eventToUpdate == null) {
                            // if an event belongs to a sequence insert it immediately, to avoid some glitches with linked events
                            if (isSequence) {
                                if (curRecurrenceDayCode.isEmpty()) {
                                    eventsHelper.insertEvent(event, true, false)
                                } else {
                                    // if an event contains the RECURRENCE-ID field, it is an exception to a recurring event, so update its parent too
                                    val parentEvent = activity.eventsDB.getEventWithImportId(event.importId)
                                    if (parentEvent != null && !parentEvent.repetitionExceptions.contains(curRecurrenceDayCode)) {
                                        parentEvent.addRepetitionException(curRecurrenceDayCode)
                                        eventsHelper.insertEvent(parentEvent, true, false)

                                        event.parentId = parentEvent.id!!
                                        eventsToInsert.add(event)
                                    }
                                }
                            } else {
                                eventsToInsert.add(event)
                            }
                        } else {
                            event.id = eventToUpdate.id
                            eventsHelper.updateEvent(event, true, false)
                        }
                        eventsImported++
                        resetValues()
                    }
                    line = curLine
                }
            }

            eventsHelper.insertEvents(eventsToInsert, true)
        } catch (e: Exception) {
            activity.showErrorToast(e)
            eventsFailed++
        }

        return when {
            eventsImported == 0 -> {
                if (eventsAlreadyExist > 0) {
                    IMPORT_NOTHING_NEW
                } else {
                    IMPORT_FAIL
                }
            }
            eventsFailed > 0 -> IMPORT_PARTIAL
            else -> IMPORT_OK
        }
    }

    private fun getTimestamp(fullString: String): Long {
        return try {
            when {
                fullString.startsWith(';') -> {
                    val value = fullString.substring(fullString.lastIndexOf(':') + 1).replace(" ", "")
                    if (value.isEmpty()) {
                        return 0
                    } else if (!value.contains("T")) {
                        curFlags = curFlags or FLAG_ALL_DAY
                    }

                    Parser().parseDateTimeValue(value)
                }
                fullString.startsWith(":") -> Parser().parseDateTimeValue(fullString.substring(1).trim())
                else -> Parser().parseDateTimeValue(fullString)
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
            eventsFailed++
            -1
        }
    }

    private fun getLocation(fullString: String): String {
        return if (fullString.startsWith(":")) {
            fullString.trimStart(':')
        } else {
            fullString.substringAfter(':').trim()
        }
    }

    private fun tryAddCategories(categories: String, defaultEventTypeId: Long) {
        val eventTypeTitle = if (categories.contains(",")) {
            categories.split(",")[0]
        } else {
            categories
        }

        val eventId = eventsHelper.getEventTypeIdWithTitle(eventTypeTitle)
        curEventTypeId = if (eventId == -1L) {
            val newTypeColor = if (curCategoryColor == -2) activity.resources.getColor(R.color.color_primary, null) else curCategoryColor
            var type = OTHER_EVENT
            if (defaultEventTypeId == LITURGICAL_EVENT_TYPE_ID) {
                type = HOLYDAY_EVENT
            }
            val eventType = EventType(null, eventTypeTitle, newTypeColor, type = type)
            eventsHelper.insertOrUpdateEventTypeSync(eventType)
        } else {
            eventId
        }
    }

    private fun getTitle(title: String): String {
        return if (title.startsWith(";") && title.contains(":")) {
            title.substring(title.lastIndexOf(':') + 1)
        } else {
            title.substring(1, title.length.coerceAtMost(180))
        }
    }

    private fun parseRepeatRule() {
        val repeatRule = Parser().parseRepeatInterval(curRrule, curStart)
        curRepeatRule = repeatRule.repeatRule
        curRepeatInterval = repeatRule.repeatInterval
        curRepeatLimit = repeatRule.repeatLimit
    }

    private fun parseExtendedRule() {
        if (!curRepeatInterval.isXYearlyRepetition()) return
        val repetition = Parser().parseExtendedRule(curExRrule, curRepeatRule, curFlags)
        curRepeatRule = repetition.repeatRule
        curExtendedRule = repetition.extendedRule
        curFlags = repetition.flags
    }

    private fun resetValues() {
        curStart = -1L
        curEnd = -1L
        curTitle = ""
        curLocation = ""
        curDescription = ""
        curImportId = ""
        curRecurrenceDayCode = ""
        curRrule = ""
        curFlags = 0
        curReminderMinutes = ArrayList()
        curReminderActions = ArrayList()
        curRepeatExceptions = ArrayList()
        curRepeatInterval = 0
        curRepeatLimit = 0L
        curRepeatRule = 0
        curEventTypeId = REGULAR_EVENT_TYPE_ID
        curLastModified = 0L
        curCategoryColor = -2
        isNotificationDescription = false
        isProperReminderAction = false
        isSequence = false
        isParsingEvent = false
        curReminderTriggerMinutes = REMINDER_OFF
        curReminderTriggerAction = REMINDER_NOTIFICATION
    }
}
