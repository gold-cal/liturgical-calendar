package com.liturgical.calendar.helpers

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract.*
import android.util.SparseIntArray
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.liturgical.calendar.R
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.models.*
import com.liturgical.calendar.objects.States.isUpdatingCalDAV
import com.secure.commons.extensions.*
import com.secure.commons.helpers.PERMISSION_READ_CALENDAR
import com.secure.commons.helpers.PERMISSION_WRITE_CALENDAR
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.math.max

@SuppressLint("MissingPermission")
class CalDAVHelper(val context: Context) {
    private val eventsHelper = context.eventsHelper

    fun refreshCalendars(showToasts: Boolean, scheduleNextSync: Boolean, callback: () -> Unit) {
        if (isUpdatingCalDAV) {
            return
        }

        isUpdatingCalDAV = true
        try {
            val calDAVCalendars = getCalDAVCalendars(context.config.caldavSyncedCalendarIds, showToasts)
            for (calendar in calDAVCalendars) {
                val localEventType = eventsHelper.getEventTypeWithCalDAVCalendarId(calendar.id) ?: continue
                if (calendar.displayName != localEventType.title || calendar.color != localEventType.color) {
                    localEventType.apply {
                        title = calendar.displayName
                        caldavDisplayName = calendar.displayName
                        caldavEmail = calendar.accountName
                        color = calendar.color
                        eventsHelper.insertOrUpdateEventTypeSync(this)
                    }
                }

                fetchCalDAVCalendarEvents(calendar.id, localEventType.id!!, showToasts)
            }

            if (scheduleNextSync) {
                context.scheduleCalDAVSync(true)
            }

            callback()
        } finally {
            isUpdatingCalDAV = false
        }
    }

    @SuppressLint("MissingPermission")
    fun getCalDAVCalendars(ids: String, showToasts: Boolean): ArrayList<CalDAVCalendar> {
        val calendars = ArrayList<CalDAVCalendar>()
        if (!context.hasPermission(PERMISSION_WRITE_CALENDAR) || !context.hasPermission(PERMISSION_READ_CALENDAR)) {
            return calendars
        }

        val uri = Calendars.CONTENT_URI
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.OWNER_ACCOUNT,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_ACCESS_LEVEL
        )

        val selection = if (ids.trim().isNotEmpty()) "${Calendars._ID} IN ($ids)" else null
        context.queryCursor(uri, projection, selection, showErrors = showToasts) { cursor ->
            val id = cursor.getIntValue(Calendars._ID)
            val displayName = cursor.getStringValue(Calendars.CALENDAR_DISPLAY_NAME)
            val accountName = cursor.getStringValue(Calendars.ACCOUNT_NAME)
            val accountType = cursor.getStringValue(Calendars.ACCOUNT_TYPE)
            val ownerName = cursor.getStringValue(Calendars.OWNER_ACCOUNT) ?: ""
            val color = cursor.getIntValue(Calendars.CALENDAR_COLOR)
            val accessLevel = cursor.getIntValue(Calendars.CALENDAR_ACCESS_LEVEL)
            val calendar = CalDAVCalendar(id, displayName, accountName, accountType, ownerName, color, accessLevel)
            calendars.add(calendar)
        }

        return calendars
    }

    fun updateCalDAVCalendar(eventType: EventType) {
        val uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, eventType.caldavCalendarId.toLong())
        val values = ContentValues().apply {
            val colorKey = getCalDAVColorKey(eventType)
            if (colorKey != null) {
                put(Calendars.CALENDAR_COLOR_KEY, getCalDAVColorKey(eventType))
            } else {
                put(Calendars.CALENDAR_COLOR, eventType.color)
                put(Calendars.CALENDAR_COLOR_KEY, "")
            }
            put(Calendars.CALENDAR_DISPLAY_NAME, eventType.title)
        }

        try {
            context.contentResolver.update(uri, values, null, null)
            context.eventTypesDB.insertOrUpdate(eventType)
        } catch (e: IllegalArgumentException) {
        } catch (e: SecurityException) {
            context.showErrorToast(e)
        }
    }

    private fun getCalDAVColorKey(eventType: EventType): String? {
        val colors = getAvailableCalDAVCalendarColors(eventType)
        val colorKey = colors.indexOf(eventType.color)
        return if (colorKey > 0) {
            colorKey.toString()
        } else {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun getAvailableCalDAVCalendarColors(eventType: EventType): ArrayList<Int> {
        val colors = SparseIntArray()
        val uri = Colors.CONTENT_URI
        val projection = arrayOf(Colors.COLOR, Colors.COLOR_KEY)
        val selection = "${Colors.COLOR_TYPE} = ? AND ${Colors.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(Colors.TYPE_CALENDAR.toString(), eventType.caldavEmail)

        context.queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val colorKey = cursor.getIntValue(Colors.COLOR_KEY)
            val color = cursor.getIntValue(Colors.COLOR)
            colors.put(colorKey, color)
        }

        var sortedColors = ArrayList<Int>(colors.size())
        (0 until colors.size()).mapTo(sortedColors) { colors[it] }
        if (sortedColors.isNotEmpty()) {
            sortedColors = sortedColors.distinct() as ArrayList<Int>
        }

        return sortedColors
    }

    @SuppressLint("MissingPermission")
    private fun fetchCalDAVCalendarEvents(calendarId: Int, eventTypeId: Long, showToasts: Boolean) {
        val importIdsMap = HashMap<String, Event>()
        val fetchedEventIds = ArrayList<String>()

        var errorFetchingLocalEvents = false
        val existingEvents = try {
            context.eventsDB.getEventsFromCalDAVCalendar("$CALDAV-$calendarId")
        } catch (e: Exception) {
            errorFetchingLocalEvents = true
            ArrayList()
        }

        existingEvents.forEach {
            importIdsMap[it.importId] = it
        }

        val uri = Events.CONTENT_URI
        val projection = arrayOf(
            Events._ID,
            Events.TITLE,
            Events.DESCRIPTION,
            Events.DTSTART,
            Events.DTEND,
            Events.DURATION,
            Events.EXDATE,
            Events.ALL_DAY,
            Events.RRULE,
            Events.ORIGINAL_ID,
            Events.ORIGINAL_INSTANCE_TIME,
            Events.EVENT_LOCATION,
            Events.EVENT_TIMEZONE,
            Events.CALENDAR_TIME_ZONE,
            Events.DELETED,
            Events.AVAILABILITY
        )

        val selection = "${Events.CALENDAR_ID} = $calendarId"
        context.queryCursorInlined(uri, projection, selection, showErrors = showToasts) { cursor ->
            val deleted = cursor.getIntValue(Events.DELETED)
            if (deleted == 1) {
                return@queryCursorInlined
            }

            val id = cursor.getLongValue(Events._ID)
            val title = cursor.getStringValue(Events.TITLE) ?: ""

            if (errorFetchingLocalEvents) {
                context.toast(context.getString(R.string.fetching_event_failed, "\"$title\""), Toast.LENGTH_LONG)
                return
            }

            val description = cursor.getStringValue(Events.DESCRIPTION) ?: ""
            val startTS = cursor.getLongValue(Events.DTSTART) / 1000L
            var endTS = cursor.getLongValue(Events.DTEND) / 1000L
            val allDay = cursor.getIntValue(Events.ALL_DAY)
            val rrule = cursor.getStringValue(Events.RRULE) ?: ""
            val location = cursor.getStringValue(Events.EVENT_LOCATION) ?: ""
            val originalId = cursor.getStringValue(Events.ORIGINAL_ID)
            val originalInstanceTime = cursor.getLongValue(Events.ORIGINAL_INSTANCE_TIME)
            val reminders = getCalDAVEventReminders(id)
            val attendees = Gson().toJson(getCalDAVEventAttendees(id))
            val availability = cursor.getIntValue(Events.AVAILABILITY)

            if (endTS == 0L) {
                val duration = cursor.getStringValue(Events.DURATION) ?: ""
                endTS = startTS + Parser().parseDurationSeconds(duration)
            }

            val reminder1 = reminders.getOrNull(0)
            val reminder2 = reminders.getOrNull(1)
            val reminder3 = reminders.getOrNull(2)
            val importId = getCalDAVEventImportId(calendarId, id)
            val eventTimeZone = cursor.getStringValue(Events.EVENT_TIMEZONE)
                ?: cursor.getStringValue(Events.CALENDAR_TIME_ZONE) ?: DateTimeZone.getDefault().id

            val source = "$CALDAV-$calendarId"
            val repeatRule = Parser().parseRepeatInterval(rrule, startTS)
            val event = Event(
                null, startTS, endTS, title, location, description, reminder1?.minutes ?: REMINDER_OFF,
                reminder2?.minutes ?: REMINDER_OFF, reminder3?.minutes ?: REMINDER_OFF, reminder1?.type
                    ?: REMINDER_NOTIFICATION, reminder2?.type ?: REMINDER_NOTIFICATION, reminder3?.type
                    ?: REMINDER_NOTIFICATION, repeatRule.repeatInterval, repeatRule.repeatRule,
                repeatRule.repeatLimit, ArrayList(), attendees, importId, eventTimeZone, allDay, eventTypeId, source = source, availability = availability
            )

            if (event.getIsAllDay()) {
                event.startTS = Formatter.getShiftedImportTimestamp(event.startTS)
                event.endTS = Formatter.getShiftedImportTimestamp(event.endTS)
                if (event.endTS > event.startTS) {
                    event.endTS -= DAY
                }
            }

            fetchedEventIds.add(importId)

            // if the event is an exception from another events repeat rule, find the original parent event
            if (originalInstanceTime != 0L) {
                val parentImportId = "$source-$originalId"
                val parentEvent = context.eventsDB.getEventWithImportId(parentImportId)
                val originalDayCode = Formatter.getDayCodeFromTS(originalInstanceTime / 1000L)
                if (parentEvent != null && !parentEvent.repetitionExceptions.contains(originalDayCode)) {
                    val storedEventId = context.eventsDB.getEventIdWithImportId(importId)
                    if (storedEventId != null) {
                        event.id = storedEventId
                    }
                    event.parentId = parentEvent.id!!
                    parentEvent.addRepetitionException(originalDayCode)
                    eventsHelper.insertEvent(parentEvent, addToCalDAV = false, showToasts = false)

                    event.parentId = parentEvent.id!!
                    event.addRepetitionException(originalDayCode)
                    eventsHelper.insertEvent(event, addToCalDAV = false, showToasts = false)
                    return@queryCursorInlined
                }
            }

            // some calendars add repeatable event exceptions with using the "exdate" field, not by creating a child event that is an exception
            // exdate can be stored as "20190216T230000Z", but also as "Europe/Madrid;20201208T000000Z"
            val exdate = cursor.getStringValue(Events.EXDATE) ?: ""
            if (exdate.length > 8) {
                val lines = exdate.split("\n")
                for (line in lines) {
                    val dates = line.split(",", ";")
                    dates.filter { it.isNotEmpty() && it[0].isDigit() }.forEach {
                        if (it.endsWith("Z")) {
                            // convert for example "20190216T230000Z" to "20190217000000" in Slovakia in a weird way
                            val formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'")
                            val offset = DateTimeZone.getDefault().getOffset(System.currentTimeMillis())
                            val dt = formatter.parseDateTime(it).plusMillis(offset)
                            val dayCode = Formatter.getDayCodeFromDateTime(dt)
                            event.addRepetitionException(dayCode)
                        } else {
                            val potentialTS = it.substring(0, 8)
                            if (potentialTS.areDigitsOnly()) {
                                event.addRepetitionException(potentialTS)
                            }
                        }
                    }
                }
            }

            if (importIdsMap.containsKey(event.importId)) {
                val existingEvent = importIdsMap[importId]
                val originalEventId = existingEvent!!.id

                existingEvent.apply {
                    this.id = null
                    color = 0
                    lastUpdated = 0L
                    repetitionExceptions = ArrayList()
                }

                if (existingEvent.hashCode() != event.hashCode() && title.isNotEmpty()) {
                    event.id = originalEventId
                    eventsHelper.updateEvent(event, updateAtCalDAV = false, showToasts = false)
                }
            } else {
                if (title.isNotEmpty()) {
                    importIdsMap[event.importId] = event
                    eventsHelper.insertEvent(event, addToCalDAV = false, showToasts = false)
                }
            }
        }

        val eventIdsToDelete = ArrayList<Long>()
        importIdsMap.keys.filter { !fetchedEventIds.contains(it) }.forEach {
            val caldavEventId = it
            existingEvents.forEach { event ->
                if (event.importId == caldavEventId) {
                    eventIdsToDelete.add(event.id!!)
                }
            }
        }

        eventsHelper.deleteEvents(eventIdsToDelete.toMutableList(), false)
    }

    @SuppressLint("MissingPermission")
    fun insertCalDAVEvent(event: Event) {
        val uri = Events.CONTENT_URI
        val values = fillEventContentValues(event)
        val newUri = context.contentResolver.insert(uri, values)

        val calendarId = event.getCalDAVCalendarId()
        val eventRemoteID = java.lang.Long.parseLong(newUri!!.lastPathSegment!!)
        event.importId = getCalDAVEventImportId(calendarId, eventRemoteID)

        setupCalDAVEventReminders(event)
        setupCalDAVEventAttendees(event)
        setupCalDAVEventImportId(event)
        refreshCalDAVCalendar(event)
    }

    fun updateCalDAVEvent(event: Event) {
        val uri = Events.CONTENT_URI
        val values = fillEventContentValues(event)
        val eventRemoteID = event.getCalDAVEventId()
        event.importId = getCalDAVEventImportId(event.getCalDAVCalendarId(), eventRemoteID)

        val newUri = ContentUris.withAppendedId(uri, eventRemoteID)
        context.contentResolver.update(newUri, values, null, null)

        setupCalDAVEventReminders(event)
        setupCalDAVEventAttendees(event)
        setupCalDAVEventImportId(event)
        refreshCalDAVCalendar(event)
    }

    private fun setupCalDAVEventReminders(event: Event) {
        clearEventReminders(event)
        event.getReminders().forEach {
            val contentValues = ContentValues().apply {
                put(Reminders.MINUTES, it.minutes)
                put(Reminders.METHOD, if (it.type == REMINDER_EMAIL) Reminders.METHOD_EMAIL else Reminders.METHOD_ALERT)
                put(Reminders.EVENT_ID, event.getCalDAVEventId())
            }

            try {
                context.contentResolver.insert(Reminders.CONTENT_URI, contentValues)
            } catch (e: Exception) {
                context.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun setupCalDAVEventAttendees(event: Event) {
        clearEventAttendees(event)
        val attendees = Gson().fromJson<ArrayList<Attendee>>(event.attendees, object : TypeToken<List<Attendee>>() {}.type) ?: ArrayList()
        attendees.forEach {
            val contentValues = ContentValues().apply {
                put(Attendees.ATTENDEE_NAME, it.name)
                put(Attendees.ATTENDEE_EMAIL, it.email)
                put(Attendees.ATTENDEE_STATUS, it.status)
                put(Attendees.ATTENDEE_RELATIONSHIP, it.relationship)
                put(Attendees.EVENT_ID, event.getCalDAVEventId())
            }

            try {
                context.contentResolver.insert(Attendees.CONTENT_URI, contentValues)
            } catch (e: Exception) {
                context.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun setupCalDAVEventImportId(event: Event) {
        context.eventsDB.updateEventImportIdAndSource(event.importId, "$CALDAV-${event.getCalDAVCalendarId()}", event.id!!)
    }

    private fun fillEventContentValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(Events.CALENDAR_ID, event.getCalDAVCalendarId())
            put(Events.TITLE, event.title)
            put(Events.DESCRIPTION, event.description)
            put(Events.DTSTART, event.startTS * 1000L)
            put(Events.ALL_DAY, if (event.getIsAllDay()) 1 else 0)
            put(Events.EVENT_TIMEZONE, event.getTimeZoneString())
            put(Events.EVENT_LOCATION, event.location)
            put(Events.STATUS, Events.STATUS_CONFIRMED)
            put(Events.AVAILABILITY, event.availability)

            val repeatRule = Parser().getRepeatCode(event)
            if (repeatRule.isEmpty()) {
                putNull(Events.RRULE)
            } else {
                put(Events.RRULE, repeatRule)
            }

            if (event.getIsAllDay() && event.endTS >= event.startTS)
                event.endTS += DAY

            if (event.repeatInterval > 0) {
                put(Events.DURATION, getDurationCode(event))
                putNull(Events.DTEND)
            } else {
                put(Events.DTEND, event.endTS * 1000L)
                putNull(Events.DURATION)
            }
        }
    }

    private fun clearEventReminders(event: Event) {
        val selection = "${Reminders.EVENT_ID} = ?"
        val selectionArgs = arrayOf(event.getCalDAVEventId().toString())
        context.contentResolver.delete(Reminders.CONTENT_URI, selection, selectionArgs)
    }

    private fun clearEventAttendees(event: Event) {
        val selection = "${Attendees.EVENT_ID} = ?"
        val selectionArgs = arrayOf(event.getCalDAVEventId().toString())
        context.contentResolver.delete(Attendees.CONTENT_URI, selection, selectionArgs)
    }

    private fun getDurationCode(event: Event): String {
        return if (event.getIsAllDay()) {
            val dur = max(1, (event.endTS - event.startTS) / DAY)
            "P${dur}D"
        } else {
            Parser().getDurationCode((event.endTS - event.startTS) / 60L)
        }
    }

    fun deleteCalDAVCalendarEvents(calendarId: Long) {
        val eventIds = context.eventsDB.getCalDAVCalendarEvents("$CALDAV-$calendarId").toMutableList()
        eventsHelper.deleteEvents(eventIds, false)
    }

    fun deleteCalDAVEvent(event: Event) {
        val uri = Events.CONTENT_URI
        val contentUri = ContentUris.withAppendedId(uri, event.getCalDAVEventId())
        try {
            context.contentResolver.delete(contentUri, null, null)
        } catch (ignored: Exception) {
        }
        refreshCalDAVCalendar(event)
    }

    fun insertEventRepeatException(event: Event, occurrenceTS: Long) {
        val uri = Events.CONTENT_URI
        val values = fillEventRepeatExceptionValues(event, occurrenceTS)
        try {
            context.contentResolver.insert(uri, values)
            refreshCalDAVCalendar(event)
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }

    private fun fillEventRepeatExceptionValues(event: Event, occurrenceTS: Long): ContentValues {
        return ContentValues().apply {
            put(Events.CALENDAR_ID, event.getCalDAVCalendarId())
            put(Events.DTSTART, occurrenceTS)
            put(Events.DTEND, occurrenceTS + (event.endTS - event.startTS))
            put(Events.ORIGINAL_ID, event.getCalDAVEventId())
            put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id.toString())
            put(Events.ORIGINAL_INSTANCE_TIME, occurrenceTS * 1000L)
            put(Events.EXDATE, Formatter.getDayCodeFromTS(occurrenceTS))
        }
    }

    private fun getCalDAVEventReminders(eventId: Long): List<Reminder> {
        val reminders = ArrayList<Reminder>()
        val uri = Reminders.CONTENT_URI
        val projection = arrayOf(
            Reminders.MINUTES,
            Reminders.METHOD
        )
        val selection = "${Reminders.EVENT_ID} = $eventId"

        context.queryCursor(uri, projection, selection) { cursor ->
            val minutes = cursor.getIntValue(Reminders.MINUTES)
            val method = cursor.getIntValue(Reminders.METHOD)
            if (method == Reminders.METHOD_ALERT || method == Reminders.METHOD_EMAIL) {
                val type = if (method == Reminders.METHOD_EMAIL) REMINDER_EMAIL else REMINDER_NOTIFICATION
                val reminder = Reminder(minutes, type)
                reminders.add(reminder)
            }
        }

        return reminders.sortedBy { it.minutes }
    }

    private fun getCalDAVEventAttendees(eventId: Long): List<Attendee> {
        val attendees = ArrayList<Attendee>()
        val uri = Attendees.CONTENT_URI
        val projection = arrayOf(
            Attendees.ATTENDEE_NAME,
            Attendees.ATTENDEE_EMAIL,
            Attendees.ATTENDEE_STATUS,
            Attendees.ATTENDEE_RELATIONSHIP
        )
        val selection = "${Attendees.EVENT_ID} = $eventId"
        context.queryCursor(uri, projection, selection) { cursor ->
            val name = cursor.getStringValue(Attendees.ATTENDEE_NAME) ?: ""
            val email = cursor.getStringValue(Attendees.ATTENDEE_EMAIL) ?: ""
            val status = cursor.getIntValue(Attendees.ATTENDEE_STATUS)
            val relationship = cursor.getIntValue(Attendees.ATTENDEE_RELATIONSHIP)
            val attendee = Attendee(0, name, email, status, "", false, relationship)
            attendees.add(attendee)
        }

        return attendees
    }

    private fun getCalDAVEventImportId(calendarId: Int, eventId: Long) = "$CALDAV-$calendarId-$eventId"

    private fun refreshCalDAVCalendar(event: Event) = context.refreshCalDAVCalendars(event.getCalDAVCalendarId().toString(), false)
}
