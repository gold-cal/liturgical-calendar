package com.liturgical.calendar.helpers

import com.liturgical.calendar.activities.EventActivity
import com.liturgical.calendar.activities.TaskActivity

const val STORED_LOCALLY_ONLY = 0
const val ROW_COUNT = 6
const val COLUMN_COUNT = 7
const val SCHEDULE_CALDAV_REQUEST_CODE = 10000
const val SCHEDULE_WIDGET_REQUEST_CODE = 20000
const val ACTION_AUTO_UPDATE = "com.liturgical.calendar.AUTO_UPDATE"

const val DAY_CODE = "day_code"
const val YEAR_LABEL = "year"
const val EVENT_ID = "event_id"
const val IS_TASK = "is_task"
const val IS_DUPLICATE_INTENT = "is_duplicate_intent"
const val EVENT_OCCURRENCE_TS = "event_occurrence_ts"
const val IS_TASK_COMPLETED = "is_task_completed"
const val NEW_EVENT_START_TS = "new_event_start_ts"
const val WEEK_START_TIMESTAMP = "week_start_timestamp"
const val NEW_EVENT_SET_HOUR_DURATION = "new_event_set_hour_duration"
const val WEEK_START_DATE_TIME = "week_start_date_time"
const val YEAR_TO_OPEN = "year_to_open"
const val CALDAV = "Caldav"
const val VIEW_TO_OPEN = "view_to_open"
const val SHORTCUT_NEW_EVENT = "shortcut_new_event"
const val SHORTCUT_NEW_TASK = "shortcut_new_task"
const val REGULAR_EVENT_TYPE_ID = 1L
const val LITURGICAL_EVENT_TYPE_ID = 3L
const val TIME_ZONE = "time_zone"
const val CURRENT_TIME_ZONE = "current_time_zone"
const val IS_FIRST_RUN = "is_first_run"
const val TODAY_POSITION = "today_position"

const val MONTHLY_VIEW = 1
const val YEARLY_VIEW = 2
const val EVENTS_LIST_VIEW = 3
const val WEEKLY_VIEW = 4
const val DAILY_VIEW = 5
const val LAST_VIEW = 6
const val MONTHLY_DAILY_VIEW = 7

const val REMINDER_OFF = -1
const val REMINDER_DEFAULT_VALUE = "${REMINDER_OFF},${REMINDER_OFF},${REMINDER_OFF}"

const val OTHER_EVENT = 0
const val BIRTHDAY_EVENT = 1
const val ANNIVERSARY_EVENT = 2
const val HOLIDAY_EVENT = 3
const val LITURGICAL_EVENT = 4
const val HOLYDAY_EVENT = 5
const val CONTACT_EVENT = 6

const val ITEM_EVENT = 0
const val ITEM_SECTION_DAY = 1
const val ITEM_SECTION_MONTH = 2

const val DEFAULT_START_TIME_NEXT_FULL_HOUR = -1
const val DEFAULT_START_TIME_CURRENT_TIME = -2

const val TYPE_EVENT = 0
const val TYPE_TASK = 1

const val DAY = 86400
const val WEEK = 604800
const val MONTH = 2592001    // exact value not taken into account, Joda is used for adding months and years
const val YEAR = 31536000

const val SECONDS_IN_MOON_CYCLE = 2551442

const val EVENT_PERIOD_TODAY = -1
const val EVENT_PERIOD_CUSTOM = -2

// Preferences
const val EVENT_LIST_PERIOD = "event_list_period"
const val WEEK_NUMBERS = "week_numbers"
const val START_WEEKLY_AT = "start_weekly_at"
const val START_WEEK_WITH_CURRENT_DAY = "start_week_with_current_day"
const val SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP = "show_midnight_spanning_events_at_top"
const val ALLOW_CUSTOMIZE_DAY_COUNT = "allow_customise_day_count"
const val VIBRATE = "vibrate"
const val REMINDER_SOUND_URI = "reminder_sound_uri"
const val REMINDER_SOUND_TITLE = "reminder_sound_title"
const val VIEW = "view"
const val LAST_EVENT_REMINDER_MINUTES = "reminder_minutes"
const val LAST_EVENT_REMINDER_MINUTES_2 = "reminder_minutes_2"
const val LAST_EVENT_REMINDER_MINUTES_3 = "reminder_minutes_3"
const val DISPLAY_EVENT_TYPES = "display_event_types"
const val QUICK_FILTER_EVENT_TYPES = "quick_filter_event_types"
const val LIST_WIDGET_VIEW_TO_OPEN = "list_widget_view_to_open"
const val LIST_WIDGET_DAY_POSITION = "list_widget_day_position"
const val CALDAV_SYNC = "caldav_sync"
const val WIDGET_UPDATE = "widget_update"
const val CALDAV_SYNCED_CALENDAR_IDS = "caldav_synced_calendar_ids"
const val LAST_USED_CALDAV_CALENDAR = "last_used_caldav_calendar"
const val LAST_USED_LOCAL_EVENT_TYPE_ID = "last_used_local_event_type_id"
const val LAST_USED_TASK_TYPE_ID = "last_used_task_type_id"
const val DISPLAY_PAST_EVENTS = "display_past_events"
const val DISPLAY_DESCRIPTION = "display_description"
const val REPLACE_DESCRIPTION = "replace_description"
const val SHOW_GRID = "show_grid"
const val LOOP_REMINDERS = "loop_reminders"
const val DIM_PAST_EVENTS = "dim_past_events"
const val DIM_COMPLETED_TASKS = "dim_completed_tasks"
const val LAST_SOUND_URI = "last_sound_uri"
const val LAST_REMINDER_CHANNEL_ID = "last_reminder_channel_ID"
const val REMINDER_AUDIO_STREAM = "reminder_audio_stream"
const val USE_PREVIOUS_EVENT_REMINDERS = "use_previous_event_reminders"
const val DEFAULT_REMINDER_1 = "default_reminder_1"
const val DEFAULT_REMINDER_2 = "default_reminder_2"
const val DEFAULT_REMINDER_3 = "default_reminder_3"
const val PULL_TO_REFRESH = "pull_to_refresh"
const val LAST_VIBRATE_ON_REMINDER = "last_vibrate_on_reminder"
const val DEFAULT_START_TIME = "default_start_time"
const val DEFAULT_DURATION = "default_duration"
const val DEFAULT_EVENT_TYPE_ID = "default_event_type_id"
const val DEFAULT_TASK_TYPE_ID = "default_task_type_id"
const val ALLOW_CHANGING_TIME_ZONES = "allow_changing_time_zones"
const val ADD_BIRTHDAYS_AUTOMATICALLY = "add_birthdays_automatically"
const val ADD_ANNIVERSARIES_AUTOMATICALLY = "add_anniversaries_automatically"
const val ADD_CONTACT_EVENTS_AUTOMATICALLY = "add_contact_events_automatically"
const val BIRTHDAY_REMINDERS = "birthday_reminders"
const val ANNIVERSARY_REMINDERS = "anniversary_reminders"
const val CUSTOM_EVENT_REMINDERS = "custom_event_reminders"
const val LAST_EXPORT_PATH = "last_export_path"
const val EXPORT_PAST_EVENTS = "export_past_events"
const val WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER = "weekly_view_item_height_multiplier"
const val WEEKLY_VIEW_DAYS = "weekly_view_days"
const val HIGHLIGHT_WEEKENDS = "highlight_weekends"
const val SHOW_WHATS_NEW = "show_whats_new"
const val HIGHLIGHT_WEEKENDS_COLOR = "highlight_weekends_color"
const val LAST_USED_EVENT_SPAN = "last_used_event_span"
const val ALLOW_CREATING_TASKS = "allow_creating_tasks"
const val SHOW_BIRTH_ANN_DESCRIPTION = "show_birth_ann_description"
const val SHOW_WIDGET_DESCRIPTION = "show_widget_description"
const val CURRENT_SCROLL_POSITION = "current_scroll_position"
const val VIEW_POSITION = "view_position"

// TLC values
const val TLC_REFRESH = "tlc_refresh"
const val LAST_CALCULATED_FULL_MOON = "last_calculated_full_moon"
const val IS_REFRESH = "is_refresh"

// repeat_rule for monthly repetition
const val REPEAT_LAST_DAY = 3                           // i.e. every last day of the month

// repeat_rule for monthly and yearly repetition
const val REPEAT_SAME_DAY = 1                           // i.e. 25th every month, or 3rd june (if yearly repetition)
const val REPEAT_ORDER_WEEKDAY_USE_LAST = 2             // i.e. every last sunday. 4th if a month has 4 sundays, 5th if 5 (or last sunday in june, if yearly)
const val REPEAT_ORDER_WEEKDAY = 4                      // i.e. every 4th sunday, even if a month has 4 sundays only (will stay 4th even at months with 5)

// repeat_rule for yearly repetition
const val REPEAT_AFTER_DAY = 5
const val REPEAT_BEFORE_DAY = 6
const val REPEAT_AFTER_FM = 7
// const val REPEAT_IF_DAY = 8

// special event and task flags
const val FLAG_ALL_DAY = 1
const val FLAG_IS_IN_PAST = 2
const val FLAG_MISSING_YEAR = 4
const val FLAG_TASK_COMPLETED = 8

// constants related to ICS file exporting / importing
const val BEGIN_CALENDAR = "BEGIN:VCALENDAR"
const val END_CALENDAR = "END:VCALENDAR"
const val CALENDAR_PRODID = "PRODID:-//Liturgical Calendar//NONSGML Event Calendar//EN"
const val CALENDAR_VERSION = "VERSION:2.0"
const val BEGIN_EVENT = "BEGIN:VEVENT"
const val END_EVENT = "END:VEVENT"
const val BEGIN_ALARM = "BEGIN:VALARM"
const val END_ALARM = "END:VALARM"
const val DTSTART = "DTSTART"
const val DTEND = "DTEND"
const val LAST_MODIFIED = "LAST-MODIFIED"
const val DTSTAMP = "DTSTAMP:"
const val DURATION = "DURATION:"
const val SUMMARY = "SUMMARY"
const val DESCRIPTION = "DESCRIPTION:"
const val UID = "UID:"
const val ACTION = "ACTION:"
const val TRANSP = "TRANSP:"
const val ATTENDEE = "ATTENDEE:"
const val MAILTO = "mailto:"
const val TRIGGER = "TRIGGER"
const val RRULE = "RRULE:"
const val CATEGORIES = "CATEGORIES:"
const val STATUS = "STATUS:"
const val EXDATE = "EXDATE"
const val BYDAY = "BYDAY"
const val BYMONTHDAY = "BYMONTHDAY"
const val BYMONTH = "BYMONTH"
const val LOCATION = "LOCATION"
const val RECURRENCE_ID = "RECURRENCE-ID"
const val SEQUENCE = "SEQUENCE"

// this tag isn't a standard ICS tag, but there's no official way of adding a category color in an ics file
const val CATEGORY_COLOR = "X-SMT-CATEGORY-COLOR:"
const val CATEGORY_COLOR_LEGACY = "CATEGORY_COLOR:"
const val MISSING_YEAR = "X-SMT-MISSING-YEAR:"
const val EXRRULE = "EXRRULE:"
//const val NID = "NID:" // New Id for calendar event

// RRule properties
const val DISPLAY = "DISPLAY"
const val EMAIL = "EMAIL"
const val FREQ = "FREQ"
const val UNTIL = "UNTIL"
const val COUNT = "COUNT"
const val INTERVAL = "INTERVAL"
const val CONFIRMED = "CONFIRMED"
const val VALUE = "VALUE"
const val DATE = "DATE"

// Repeat Rules added to Flags
const val REPEAT_XTH_DAY_OF_WEEK_AFTER = 16
const val REPEAT_X_DAYS_AFTER = 32

// Repeat Rule Bits
// FIRST_SUNDAY == 1 and continue up 2 FIRST_MONDAY, 3,4, ...
const val FM_ADD_DAYS_RULE = 512
const val FM_ADD_WEEKS_RULE = 1024
const val FM_MINUS_DAYS_RULE = 2048
const val FM_MINUS_WEEKS_RULE = 4096

// EXRRULE Properties
const val BEFORE_DATE = "BEFORE-DATE"
const val AFTER_DATE = "AFTER-DATE"
const val FULL_MOON = "FM"
const val FM_PLUS_WEEKS = "FM-PW"
const val FM_PLUS_DAYS = "FM-PD"
const val FM_MINUS_DAYS = "FM-MD"
const val FM_MINUS_WEEKS = "FM-MW"
//const val IF = "IF"
//const val CUSTOM = "CUSTOM" // allow for use of if statements

const val DAILY = "DAILY"
const val WEEKLY = "WEEKLY"
const val MONTHLY = "MONTHLY"
const val YEARLY = "YEARLY"

const val MO = "MO"
const val TU = "TU"
const val WE = "WE"
const val TH = "TH"
const val FR = "FR"
const val SA = "SA"
const val SU = "SU"

const val OPAQUE = "OPAQUE"
const val TRANSPARENT = "TRANSPARENT"

const val SOURCE_DEFAULT_CALENDAR = "default-calendar"
const val SOURCE_IMPORTED_ICS = "imported-ics"
const val SOURCE_CONTACT_BIRTHDAY = "contact-birthday"
const val SOURCE_CONTACT_CUSTOM = "contact-custom"
const val SOURCE_CONTACT_ANNIVERSARY = "contact-anniversary"
const val SOURCE_LITURGICAL_CALENDAR = "liturgical-calendar"

const val DELETE_SELECTED_OCCURRENCE = 0
const val DELETE_FUTURE_OCCURRENCES = 1
const val DELETE_ALL_OCCURRENCES = 2

const val REMINDER_NOTIFICATION = 0
const val REMINDER_EMAIL = 1

// Event Items
const val EVENT = "EVENT"
const val TASK = "TASK"
const val START_TS = "START_TS"
const val END_TS = "END_TS"
const val ORIGINAL_START_TS = "ORIGINAL_START_TS"
const val ORIGINAL_END_TS = "ORIGINAL_END_TS"
const val REMINDER_1_MINUTES = "REMINDER_1_MINUTES"
const val REMINDER_2_MINUTES = "REMINDER_2_MINUTES"
const val REMINDER_3_MINUTES = "REMINDER_3_MINUTES"
const val REMINDER_1_TYPE = "REMINDER_1_TYPE"
const val REMINDER_2_TYPE = "REMINDER_2_TYPE"
const val REMINDER_3_TYPE = "REMINDER_3_TYPE"
const val REPEAT_INTERVAL = "REPEAT_INTERVAL"
const val REPEAT_LIMIT = "REPEAT_LIMIT"
const val REPEAT_RULE = "REPEAT_RULE"
const val ATTENDEES = "ATTENDEES"
const val AVAILABILITY = "AVAILABILITY"
const val EVENT_TYPE_ID = "EVENT_TYPE_ID"
const val EVENT_CALENDAR_ID = "EVENT_CALENDAR_ID"
const val IS_NEW_EVENT = "IS_NEW_EVENT"

// actions
const val ACTION_MARK_COMPLETED = "ACTION_MARK_COMPLETED"
//const val ACTION_REFRESH_EVENTS = "ACTION_REFRESH_EVENTS"

fun getNowSeconds() = System.currentTimeMillis() / 1000L

fun isWeekend(i: Int, isSundayFirst: Boolean): Boolean {
    return if (isSundayFirst) {
        i == 0 || i == 6 || i == 7 || i == 13
    } else {
        i == 5 || i == 6 || i == 12 || i == 13
    }
}

fun getActivityToOpen(isTask: Boolean) = if (isTask) {
    TaskActivity::class.java
} else {
    EventActivity::class.java
}
