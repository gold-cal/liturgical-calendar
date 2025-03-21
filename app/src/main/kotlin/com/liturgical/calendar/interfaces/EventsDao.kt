package com.liturgical.calendar.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.models.Event

@Dao
interface EventsDao {
    @Query("SELECT * FROM events")
    fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE event_type IN (:eventTypeIds) AND type = $TYPE_EVENT")
    fun getAllEventsWithTypes(eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE id = :id AND type = $TYPE_EVENT")
    fun getEventWithId(id: Long): Event?

    @Query("SELECT * FROM events WHERE id = :id AND type = $TYPE_TASK")
    fun getTaskWithId(id: Long): Event?

    @Query("SELECT * FROM events WHERE id = :id AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getEventOrTaskWithId(id: Long): Event?

    @Query("SELECT * FROM events WHERE import_id = :importId AND type = $TYPE_EVENT")
    fun getEventWithImportId(importId: String): Event?

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND end_ts >= :fromTS AND repeat_interval = 0 AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getOneTimeEventsOrTasksFromTo(toTS: Long, fromTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND start_ts >= :fromTS AND event_type IN (:eventTypeIds) AND type = $TYPE_TASK")
    fun getTasksFromTo(fromTS: Long, toTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE start_ts < :olderThen AND repeat_interval = 0 AND source != \'$SOURCE_LITURGICAL_CALENDAR\'")
    fun getOldEvents(olderThen: Long): List<Event>

    @Query("SELECT * FROM events WHERE id = :id AND start_ts <= :toTS AND end_ts >= :fromTS AND repeat_interval = 0 AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getOneTimeEventFromToWithId(id: Long, toTS: Long, fromTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND end_ts >= :fromTS AND start_ts != 0 AND repeat_interval = 0 AND event_type IN (:eventTypeIds) AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getOneTimeEventsFromToWithTypes(toTS: Long, fromTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE end_ts > :toTS AND repeat_interval = 0 AND event_type IN (:eventTypeIds) AND type = $TYPE_EVENT")
    fun getOneTimeFutureEventsWithTypes(toTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND repeat_interval != 0 AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getRepeatableEventsOrTasksWithTypes(toTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE id = :id AND start_ts <= :toTS AND repeat_interval != 0 AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getRepeatableEventsOrTasksWithId(id: Long, toTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND start_ts != 0 AND repeat_interval != 0 AND event_type IN (:eventTypeIds) AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getRepeatableEventsOrTasksWithTypes(toTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE repeat_interval != 0 AND (repeat_limit == 0 OR repeat_limit > :currTS) AND event_type IN (:eventTypeIds) AND type = $TYPE_EVENT")
    fun getRepeatableFutureEventsWithTypes(currTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE id IN (:ids) AND import_id != \"\" AND type = $TYPE_EVENT")
    fun getEventsByIdsWithImportIds(ids: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE title LIKE :searchQuery OR location LIKE :searchQuery OR description LIKE :searchQuery AND type = $TYPE_EVENT")
    fun getEventsForSearch(searchQuery: String): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_LITURGICAL_CALENDAR\'")
    fun getLiturgicalEvents(): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_BIRTHDAY\' AND type = $TYPE_EVENT")
    fun getBirthdays(): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_CUSTOM\' AND type = $TYPE_EVENT")
    fun getCustom(): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_ANNIVERSARY\' AND type = $TYPE_EVENT")
    fun getAnniversaries(): List<Event>

    @Query("SELECT * FROM events WHERE import_id != \"\" AND type = $TYPE_EVENT")
    fun getEventsWithImportIds(): List<Event>

    @Query("SELECT * FROM events WHERE source = :source AND type = $TYPE_EVENT")
    fun getEventsFromCalDAVCalendar(source: String): List<Event>

    @Query("SELECT * FROM events WHERE id IN (:ids) AND type = $TYPE_EVENT")
    fun getEventsWithIds(ids: List<Long>): List<Event>

    //val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0) AND $COL_START_TS != 0"
    @Query("SELECT * FROM events WHERE reminder_1_minutes != -1 AND (start_ts > :currentTS OR repeat_interval != 0) AND start_ts != 0 AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun getEventsOrTasksAtReboot(currentTS: Long): List<Event>

    @Query("SELECT id FROM events")
    fun getEventIds(): List<Long>

    @Query("SELECT id FROM events WHERE import_id = :importId AND type = $TYPE_EVENT")
    fun getEventIdWithImportId(importId: String): Long?

    @Query("SELECT id FROM events WHERE import_id LIKE :importId AND type = $TYPE_EVENT")
    fun getEventIdWithLastImportId(importId: String): Long?

    @Query("SELECT id FROM events WHERE event_type = :eventTypeId AND type = $TYPE_EVENT")
    fun getEventIdsByEventType(eventTypeId: Long): List<Long>

    @Query("SELECT id FROM events WHERE event_type IN (:eventTypeIds) AND type = $TYPE_EVENT")
    fun getEventIdsByEventType(eventTypeIds: List<Long>): List<Long>

    @Query("SELECT id FROM events WHERE parent_id IN (:parentIds) AND type = $TYPE_EVENT")
    fun getEventIdsWithParentIds(parentIds: List<Long>): List<Long>

    @Query("SELECT id FROM events WHERE source = :source AND import_id != \"\" AND type = $TYPE_EVENT")
    fun getCalDAVCalendarEvents(source: String): List<Long>

    @Query("UPDATE events SET event_type = $REGULAR_EVENT_TYPE_ID WHERE event_type = :eventTypeId AND type = $TYPE_EVENT")
    fun resetEventsWithType(eventTypeId: Long)

    @Query("UPDATE events SET import_id = :importId, source = :source WHERE id = :id AND type = $TYPE_EVENT")
    fun updateEventImportIdAndSource(importId: String, source: String, id: Long)

    @Query("UPDATE events SET repeat_limit = :repeatLimit WHERE id = :id AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun updateEventRepetitionLimit(repeatLimit: Long, id: Long)

    @Query("UPDATE events SET repetition_exceptions = :repetitionExceptions WHERE id = :id AND (type = $TYPE_EVENT OR type = $TYPE_TASK)")
    fun updateEventRepetitionExceptions(repetitionExceptions: String, id: Long)

    @Deprecated("Use Context.updateTaskCompletion() instead unless you know what you are doing.")
    @Query("UPDATE events SET flags = :newFlags WHERE id = :id")
    fun updateTaskCompletion(id: Long, newFlags: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(event: Event): Long

    @Query("DELETE FROM events WHERE id IN (:ids)")
    fun deleteEvents(ids: List<Long>)

    @Query("DELETE FROM events WHERE source = :source AND import_id = :importId")
    fun deleteBirthdayAnniversary(source: String, importId: String): Int
}
