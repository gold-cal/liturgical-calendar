package com.liturgical.calendar.interfaces

import androidx.room.*
import com.liturgical.calendar.helpers.HOLY_DAY_EVENT
import com.liturgical.calendar.helpers.LITURGICAL_EVENT
import com.liturgical.calendar.models.EventType

@Dao
interface EventTypesDao {
    @Query("SELECT * FROM event_types ORDER BY title ASC")
    fun getEventTypes(): List<EventType>

    @Query("SELECT * FROM event_types WHERE type != $LITURGICAL_EVENT AND type != $HOLY_DAY_EVENT ORDER BY title ASC")
    fun getAvailableEventTypes(): List<EventType>

    @Query("SELECT * FROM event_types WHERE id = :id")
    fun getEventTypeWithId(id: Long): EventType?

    @Query("SELECT * FROM event_types WHERE title = :title")
    fun getEventTypeWithTitle(title: String): EventType?

    @Query("SELECT id FROM event_types WHERE title = :title COLLATE NOCASE")
    fun getEventTypeIdWithTitle(title: String): Long?

    @Query("SELECT id FROM event_types WHERE title = :title AND caldav_calendar_id = 0 COLLATE NOCASE")
    fun getLocalEventTypeIdWithTitle(title: String): Long?

    @Query("SELECT id FROM event_types WHERE type = :classId")
    fun getEventTypeIdWithClass(classId: Int): Long?

    @Query("SELECT id FROM event_types WHERE type = :classId AND caldav_calendar_id = 0")
    fun getLocalEventTypeIdWithClass(classId: Int): Long?

    @Query("SELECT * FROM event_types WHERE caldav_calendar_id = :calendarId")
    fun getEventTypeWithCalDAVCalendarId(calendarId: Int): EventType?

    @Query("DELETE FROM event_types WHERE caldav_calendar_id IN (:ids)")
    fun deleteEventTypesWithCalendarId(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(eventType: EventType): Long

    @Delete
    fun deleteEventTypes(eventTypes: List<EventType>)

    @Query("DELETE FROM event_types WHERE id = :id")
    fun deleteEventTypeWithId(id: Long)
}
