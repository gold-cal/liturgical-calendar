package com.liturgical.calendar.helpers

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import com.liturgical.calendar.R
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.scheduleCalDAVSync
import com.secure.commons.extensions.getDefaultAlarmTitle
import com.secure.commons.helpers.BaseConfig
import com.secure.commons.helpers.YEAR_SECONDS
import java.util.*
import kotlin.collections.ArrayList

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }
    
    var isFirstRun: Boolean
        get() = prefs.getBoolean(IS_FIRST_RUN, true)
        set(isFirstRun) = prefs.edit().putBoolean(IS_FIRST_RUN, isFirstRun).apply()

    var lastCalculatedFullMoon: Long
        get() = prefs.getLong(LAST_CALCULATED_FULL_MOON, 0L)
        set(lastCalculatedFullMoon) = prefs.edit().putLong(LAST_CALCULATED_FULL_MOON, lastCalculatedFullMoon).apply()

    var isFirstCalc: Boolean
        get() = prefs.getBoolean(IS_FIRST_CALC, true)
        set(isFirstCalc) = prefs.edit().putBoolean(IS_FIRST_CALC, isFirstCalc).apply()

    var tlcRefresh: Long
        get() = prefs.getLong(TLC_REFRESH, 0L)
        set(tlcRefresh) = prefs.edit().putLong(TLC_REFRESH, tlcRefresh).apply()

    var showWeekNumbers: Boolean
        get() = prefs.getBoolean(WEEK_NUMBERS, false)
        set(showWeekNumbers) = prefs.edit().putBoolean(WEEK_NUMBERS, showWeekNumbers).apply()

    var startWeeklyAt: Int
        get() = prefs.getInt(START_WEEKLY_AT, 7)
        set(startWeeklyAt) = prefs.edit().putInt(START_WEEKLY_AT, startWeeklyAt).apply()

    var startWeekWithCurrentDay: Boolean
        get() = prefs.getBoolean(START_WEEK_WITH_CURRENT_DAY, false)
        set(startWeekWithCurrentDay) = prefs.edit().putBoolean(START_WEEK_WITH_CURRENT_DAY, startWeekWithCurrentDay).apply()

    var showMidnightSpanningEventsAtTop: Boolean
        get() = prefs.getBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, true)
        set(midnightSpanning) = prefs.edit().putBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, midnightSpanning).apply()

    var allowCustomizeDayCount: Boolean
        get() = prefs.getBoolean(ALLOW_CUSTOMIZE_DAY_COUNT, true)
        set(allow) = prefs.edit().putBoolean(ALLOW_CUSTOMIZE_DAY_COUNT, allow).apply()

    var vibrateOnReminder: Boolean
        get() = prefs.getBoolean(VIBRATE, false)
        set(vibrate) = prefs.edit().putBoolean(VIBRATE, vibrate).apply()

    var reminderSoundUri: String
        get() = prefs.getString(REMINDER_SOUND_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())!!
        set(reminderSoundUri) = prefs.edit().putString(REMINDER_SOUND_URI, reminderSoundUri).apply()

    var reminderSoundTitle: String
        get() = prefs.getString(REMINDER_SOUND_TITLE, context.getDefaultAlarmTitle(RingtoneManager.TYPE_NOTIFICATION))!!
        set(reminderSoundTitle) = prefs.edit().putString(REMINDER_SOUND_TITLE, reminderSoundTitle).apply()

    var lastSoundUri: String
        get() = prefs.getString(LAST_SOUND_URI, "")!!
        set(lastSoundUri) = prefs.edit().putString(LAST_SOUND_URI, lastSoundUri).apply()

    var lastReminderChannel: Long
        get() = prefs.getLong(LAST_REMINDER_CHANNEL_ID, 0L)
        set(lastReminderChannel) = prefs.edit().putLong(LAST_REMINDER_CHANNEL_ID, lastReminderChannel).apply()

    var storedView: Int
        get() = prefs.getInt(VIEW, MONTHLY_VIEW)
        set(view) = prefs.edit().putInt(VIEW, view).apply()

    var lastEventReminderMinutes1: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES, REMINDER_OFF) /** Was originally 10 */
        set(lastEventReminderMinutes) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES, lastEventReminderMinutes).apply()

    var lastEventReminderMinutes2: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_2, REMINDER_OFF)
        set(lastEventReminderMinutes2) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_2, lastEventReminderMinutes2).apply()

    var lastEventReminderMinutes3: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_3, REMINDER_OFF)
        set(lastEventReminderMinutes3) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_3, lastEventReminderMinutes3).apply()

    var displayPastEvents: Int
        get() = prefs.getInt(DISPLAY_PAST_EVENTS, 0)
        set(displayPastEvents) = prefs.edit().putInt(DISPLAY_PAST_EVENTS, displayPastEvents).apply()

    var displayEventTypes: Set<String>
        get() = prefs.getStringSet(DISPLAY_EVENT_TYPES, HashSet<String>())!!
        set(displayEventTypes) = prefs.edit().remove(DISPLAY_EVENT_TYPES).putStringSet(DISPLAY_EVENT_TYPES, displayEventTypes).apply()

    var quickFilterEventTypes: Set<String>
        get() = prefs.getStringSet(QUICK_FILTER_EVENT_TYPES, HashSet<String>())!!
        set(quickFilterEventTypes) = prefs.edit().remove(QUICK_FILTER_EVENT_TYPES).putStringSet(QUICK_FILTER_EVENT_TYPES, quickFilterEventTypes).apply()

    fun addQuickFilterEventType(type: String) {
        val currQuickFilterEventTypes = HashSet<String>(quickFilterEventTypes)
        currQuickFilterEventTypes.add(type)
        quickFilterEventTypes = currQuickFilterEventTypes
    }

    var listWidgetViewToOpen: Int
        get() = prefs.getInt(LIST_WIDGET_VIEW_TO_OPEN, DAILY_VIEW)
        set(viewToOpenFromListWidget) = prefs.edit().putInt(LIST_WIDGET_VIEW_TO_OPEN, viewToOpenFromListWidget).apply()

    var listWidgetDayPosition: Int
        get() = prefs.getInt(LIST_WIDGET_DAY_POSITION, 0)
        set(listWidgetDayPosition) = prefs.edit().putInt(LIST_WIDGET_DAY_POSITION, listWidgetDayPosition).apply()

    var caldavSync: Boolean
        get() = prefs.getBoolean(CALDAV_SYNC, false)
        set(caldavSync) {
            context.scheduleCalDAVSync(caldavSync)
            prefs.edit().putBoolean(CALDAV_SYNC, caldavSync).apply()
        }

    var widgetUpdate: Boolean
        get() = prefs.getBoolean(WIDGET_UPDATE, false)
        set(widgetUpdate) = prefs.edit().putBoolean(WIDGET_UPDATE, widgetUpdate).apply()

    var caldavSyncedCalendarIds: String
        get() = prefs.getString(CALDAV_SYNCED_CALENDAR_IDS, "")!!
        set(calendarIDs) = prefs.edit().putString(CALDAV_SYNCED_CALENDAR_IDS, calendarIDs).apply()

    var lastUsedCaldavCalendarId: Int
        get() = prefs.getInt(LAST_USED_CALDAV_CALENDAR, getSyncedCalendarIdsAsList().first().toInt())
        set(calendarId) = prefs.edit().putInt(LAST_USED_CALDAV_CALENDAR, calendarId).apply()

    var lastUsedLocalEventTypeId: Long
        get() = prefs.getLong(LAST_USED_LOCAL_EVENT_TYPE_ID, REGULAR_EVENT_TYPE_ID)
        set(lastUsedLocalEventTypeId) = prefs.edit().putLong(LAST_USED_LOCAL_EVENT_TYPE_ID, lastUsedLocalEventTypeId).apply()

    var lastUsedTaskTypeId: Long
        get() = prefs.getLong(LAST_USED_TASK_TYPE_ID, REGULAR_EVENT_TYPE_ID)
        set(lastUsedTaskTypeId) = prefs.edit().putLong(LAST_USED_TASK_TYPE_ID, lastUsedTaskTypeId).apply()

    var reminderAudioStream: Int
        get() = prefs.getInt(REMINDER_AUDIO_STREAM, AudioManager.STREAM_NOTIFICATION)
        set(reminderAudioStream) = prefs.edit().putInt(REMINDER_AUDIO_STREAM, reminderAudioStream).apply()

    var replaceDescription: Boolean
        get() = prefs.getBoolean(REPLACE_DESCRIPTION, false)
        set(replaceDescription) = prefs.edit().putBoolean(REPLACE_DESCRIPTION, replaceDescription).apply()

    var displayDescription: Boolean
        get() = prefs.getBoolean(DISPLAY_DESCRIPTION, true)
        set(displayDescription) = prefs.edit().putBoolean(DISPLAY_DESCRIPTION, displayDescription).apply()

    var showGrid: Boolean
        get() = prefs.getBoolean(SHOW_GRID, false)
        set(showGrid) = prefs.edit().putBoolean(SHOW_GRID, showGrid).apply()

    var loopReminders: Boolean
        get() = prefs.getBoolean(LOOP_REMINDERS, false)
        set(loopReminders) = prefs.edit().putBoolean(LOOP_REMINDERS, loopReminders).apply()

    var dimPastEvents: Boolean
        get() = prefs.getBoolean(DIM_PAST_EVENTS, true)
        set(dimPastEvents) = prefs.edit().putBoolean(DIM_PAST_EVENTS, dimPastEvents).apply()

    var dimCompletedTasks: Boolean
        get() = prefs.getBoolean(DIM_COMPLETED_TASKS, true)
        set(dimCompletedTasks) = prefs.edit().putBoolean(DIM_COMPLETED_TASKS, dimCompletedTasks).apply()

    fun getSyncedCalendarIdsAsList() =
        caldavSyncedCalendarIds.split(",").filter { it.trim().isNotEmpty() }.map { Integer.parseInt(it) }.toMutableList() as ArrayList<Int>

    fun getDisplayEventTypesAsList() = displayEventTypes.map { it.toLong() }.toMutableList() as ArrayList<Long>

    fun addDisplayEventType(type: String) {
        addDisplayEventTypes(HashSet<String>(Arrays.asList(type)))
    }

    private fun addDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet<String>(displayEventTypes)
        currDisplayEventTypes.addAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    fun removeDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet<String>(displayEventTypes)
        currDisplayEventTypes.removeAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    var usePreviousEventReminders: Boolean
        get() = prefs.getBoolean(USE_PREVIOUS_EVENT_REMINDERS, true)
        set(usePreviousEventReminders) = prefs.edit().putBoolean(USE_PREVIOUS_EVENT_REMINDERS, usePreviousEventReminders).apply()

    var defaultReminder1: Int
        get() = prefs.getInt(DEFAULT_REMINDER_1, REMINDER_OFF) /** was originally 10*/
        set(defaultReminder1) = prefs.edit().putInt(DEFAULT_REMINDER_1, defaultReminder1).apply()

    var defaultReminder2: Int
        get() = prefs.getInt(DEFAULT_REMINDER_2, REMINDER_OFF)
        set(defaultReminder2) = prefs.edit().putInt(DEFAULT_REMINDER_2, defaultReminder2).apply()

    var defaultReminder3: Int
        get() = prefs.getInt(DEFAULT_REMINDER_3, REMINDER_OFF)
        set(defaultReminder3) = prefs.edit().putInt(DEFAULT_REMINDER_3, defaultReminder3).apply()

    var pullToRefresh: Boolean
        get() = prefs.getBoolean(PULL_TO_REFRESH, false)
        set(pullToRefresh) = prefs.edit().putBoolean(PULL_TO_REFRESH, pullToRefresh).apply()

    var lastVibrateOnReminder: Boolean
        get() = prefs.getBoolean(LAST_VIBRATE_ON_REMINDER, context.config.vibrateOnReminder)
        set(lastVibrateOnReminder) = prefs.edit().putBoolean(LAST_VIBRATE_ON_REMINDER, lastVibrateOnReminder).apply()

    var defaultStartTime: Int
        get() = prefs.getInt(DEFAULT_START_TIME, DEFAULT_START_TIME_NEXT_FULL_HOUR)
        set(defaultStartTime) = prefs.edit().putInt(DEFAULT_START_TIME, defaultStartTime).apply()

    var defaultDuration: Int
        get() = prefs.getInt(DEFAULT_DURATION, 0)
        set(defaultDuration) = prefs.edit().putInt(DEFAULT_DURATION, defaultDuration).apply()

    /** Set default event type to Regular event, instead of last used one
    ** can be changed in settings
     * */
    var defaultEventTypeId: Long
        get() = prefs.getLong(DEFAULT_EVENT_TYPE_ID, REGULAR_EVENT_TYPE_ID) // Originally set to -1L
        set(defaultEventTypeId) = prefs.edit().putLong(DEFAULT_EVENT_TYPE_ID, defaultEventTypeId).apply()

    var defaultTaskTypeId: Long
        get() = prefs.getLong(DEFAULT_TASK_TYPE_ID, REGULAR_EVENT_TYPE_ID)
        set(defaultTaskTypeId) = prefs.edit().putLong(DEFAULT_TASK_TYPE_ID, defaultTaskTypeId).apply()

    /*var birthdayEventTypeId: Long
        get() = prefs.getLong(BIRTHDAY_EVENT_TYPE_ID, 0L)
        set(birthdayEventTypeId) = prefs.edit().putLong(BIRTHDAY_EVENT_TYPE_ID,birthdayEventTypeId).apply()

    var anniversaryEventTypeId: Long
        get() = prefs.getLong(BIRTHDAY_EVENT_TYPE_ID, 0L)
        set(anniversaryEventTypeId) = prefs.edit().putLong(BIRTHDAY_EVENT_TYPE_ID,anniversaryEventTypeId).apply() */

    var allowChangingTimeZones: Boolean
        get() = prefs.getBoolean(ALLOW_CHANGING_TIME_ZONES, false)
        set(allowChangingTimeZones) = prefs.edit().putBoolean(ALLOW_CHANGING_TIME_ZONES, allowChangingTimeZones).apply()

    var addBirthdaysAutomatically: Boolean
        get() = prefs.getBoolean(ADD_BIRTHDAYS_AUTOMATICALLY, false)
        set(addBirthdaysAutomatically) = prefs.edit().putBoolean(ADD_BIRTHDAYS_AUTOMATICALLY, addBirthdaysAutomatically).apply()

    var addAnniversariesAutomatically: Boolean
        get() = prefs.getBoolean(ADD_ANNIVERSARIES_AUTOMATICALLY, false)
        set(addAnniversariesAutomatically) = prefs.edit().putBoolean(ADD_ANNIVERSARIES_AUTOMATICALLY, addAnniversariesAutomatically).apply()

    var addCustomEventsAutomatically: Boolean
        get() = prefs.getBoolean(ADD_CONTACT_EVENTS_AUTOMATICALLY, false)
        set(addCustomEventsAutomatically) = prefs.edit().putBoolean(ADD_CONTACT_EVENTS_AUTOMATICALLY, addCustomEventsAutomatically).apply()

    var birthdayReminders: ArrayList<Int>
        get() = prefs.getString(BIRTHDAY_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",").map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(birthdayReminders) = prefs.edit().putString(BIRTHDAY_REMINDERS, birthdayReminders.joinToString(",")).apply()

    var anniversaryReminders: ArrayList<Int>
        get() = prefs.getString(ANNIVERSARY_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",").map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(anniversaryReminders) = prefs.edit().putString(ANNIVERSARY_REMINDERS, anniversaryReminders.joinToString(",")).apply()

    var customEventReminders: ArrayList<Int>
        get() = prefs.getString(CUSTOM_EVENT_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",").map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(customEventReminders) = prefs.edit().putString(CUSTOM_EVENT_REMINDERS, customEventReminders.joinToString(",")).apply()

    var lastExportPath: String
        get() = prefs.getString(LAST_EXPORT_PATH, "")!!
        set(lastExportPath) = prefs.edit().putString(LAST_EXPORT_PATH, lastExportPath).apply()

    var exportPastEvents: Boolean
        get() = prefs.getBoolean(EXPORT_PAST_EVENTS, false)
        set(exportPastEvents) = prefs.edit().putBoolean(EXPORT_PAST_EVENTS, exportPastEvents).apply()

    var weeklyViewItemHeightMultiplier: Float
        get() = prefs.getFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, 1f)
        set(weeklyViewItemHeightMultiplier) = prefs.edit().putFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, weeklyViewItemHeightMultiplier).apply()

    var weeklyViewDays: Int
        get() = prefs.getInt(WEEKLY_VIEW_DAYS, 7)
        set(weeklyViewDays) = prefs.edit().putInt(WEEKLY_VIEW_DAYS, weeklyViewDays).apply()

    var highlightWeekends: Boolean
        get() = prefs.getBoolean(HIGHLIGHT_WEEKENDS, false)
        set(highlightWeekends) = prefs.edit().putBoolean(HIGHLIGHT_WEEKENDS, highlightWeekends).apply()

    var showWhatsNewDialog: Boolean
        get() = prefs.getBoolean(SHOW_WHATS_NEW, true)
        set(showWhatsNewDialog) = prefs.edit().putBoolean(SHOW_WHATS_NEW, showWhatsNewDialog).apply()

    var highlightWeekendsColor: Int
        get() = prefs.getInt(HIGHLIGHT_WEEKENDS_COLOR, context.resources.getColor(R.color.red_text,null))
        set(highlightWeekendsColor) = prefs.edit().putInt(HIGHLIGHT_WEEKENDS_COLOR, highlightWeekendsColor).apply()

    var lastUsedEventSpan: Int
        get() = prefs.getInt(LAST_USED_EVENT_SPAN, YEAR_SECONDS)
        set(lastUsedEventSpan) = prefs.edit().putInt(LAST_USED_EVENT_SPAN, lastUsedEventSpan).apply()

    var allowCreatingTasks: Boolean
        get() = prefs.getBoolean(ALLOW_CREATING_TASKS, true)
        set(allowCreatingTasks) = prefs.edit().putBoolean(ALLOW_CREATING_TASKS, allowCreatingTasks).apply()

    var showBirthdayAnniversaryDescription: Boolean
        get() = prefs.getBoolean(SHOW_BIRTH_ANN_DESCRIPTION, false)
        set(showBirthdayAnniversaryDescription) = prefs.edit().putBoolean(SHOW_BIRTH_ANN_DESCRIPTION, showBirthdayAnniversaryDescription).apply()

    var showWidgetDescription: Boolean
        get() = prefs.getBoolean(SHOW_WIDGET_DESCRIPTION, false)
        set(showWidgetDescription) = prefs.edit().putBoolean(SHOW_WIDGET_DESCRIPTION, showWidgetDescription).apply()

    var currentScrollPosition: Int
        get() = prefs.getInt(CURRENT_SCROLL_POSITION, 0)
        set(currentScrollPosition) = prefs.edit().putInt(CURRENT_SCROLL_POSITION, currentScrollPosition).apply()

    var viewPosition: Int
        get() = prefs.getInt(VIEW_POSITION, 0)
        set(viewPosition) = prefs.edit().putInt(VIEW_POSITION, viewPosition).apply()
}
