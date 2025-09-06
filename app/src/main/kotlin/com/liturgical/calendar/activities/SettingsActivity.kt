package com.liturgical.calendar.activities

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.ActivitySettingsBinding
import com.liturgical.calendar.databinding.ItemSettingsCheckboxBinding
import com.liturgical.calendar.databinding.ItemSettingsColorBinding
import com.liturgical.calendar.databinding.ItemSettingsDoubleTextviewBinding
import com.liturgical.calendar.databinding.ItemSettingsHolderBinding
import com.liturgical.calendar.databinding.ItemSettingsLabelBinding
import com.liturgical.calendar.databinding.ItemSettingsSingleTextviewBinding
import com.liturgical.calendar.dialogs.SelectCalendarsDialog
import com.liturgical.calendar.dialogs.SelectEventTypeDialog
import com.liturgical.calendar.dialogs.SelectQuickFilterEventTypesDialog
import com.liturgical.calendar.dialogs.YearPickerDialog
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.models.EventType
import com.secure.commons.dialogs.*
import com.secure.commons.extensions.*
import com.secure.commons.helpers.*
import com.secure.commons.models.AlarmSound
import com.secure.commons.models.RadioItem
import org.joda.time.DateTime
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val GET_RINGTONE_URI = 1
    private val PICK_IMPORT_SOURCE_INTENT = 2
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    //private var mStoredAccentColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        //mStoredAccentColor = getProperAccentColor()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)
        setupSettingItems()
    }

    private fun setupSettingItems() {
        binding.settingsHolder.removeAllViews()
        //-------- COLOR CUSTOMIZATION -------------
        var label = createSettingsLabel(R.string.color_customization)
        binding.settingsHolder.addView(label)
        setupColorCustomization()
        //-------- GENERAL -------------------------
        label = createSettingsLabel(R.string.general_settings)
        binding.settingsHolder.addView(label)
        setupGeneralSettings()
        //-------- REMINDERS -----------------------
        label = createSettingsLabel(R.string.reminders)
        binding.settingsHolder.addView(label)
        setupRemindersSection()
        //-------- CALDAV --------------------------
        label = createSettingsLabel(R.string.caldav)
        binding.settingsHolder.addView(label)
        setupCaldavSection()
        //-------- EVENTS --------------------------
        label = createSettingsLabel(R.string.events)
        binding.settingsHolder.addView(label)
        setupEventsSection()
        //-------- NEW EVENTS ----------------------
        label = createSettingsLabel(R.string.new_event)
        binding.settingsHolder.addView(label)
        setupNewEventsSection()
        //-------- TASKS ---------------------------
        label = createSettingsLabel(R.string.tasks)
        binding.settingsHolder.addView(label)
        setupTaskSection()
        //-------- EVENT LISTS ---------------------
        label = createSettingsLabel(R.string.event_lists)
        binding.settingsHolder.addView(label)
        setupEventsListsSection()
        //-------- WEEKLY VIEW ---------------------
        label = createSettingsLabel(R.string.weekly_view)
        binding.settingsHolder.addView(label)
        setupWeeklyViewSection()
        //-------- MONTHLY VIEW ---------------------
        label = createSettingsLabel(R.string.monthly_view)
        binding.settingsHolder.addView(label)
        setupMonthlyViewSection()
        //-------- WIDGETS -------------------------
        label = createSettingsLabel(R.string.widgets)
        binding.settingsHolder.addView(label)
        setupWidgetsSection()
        //-------- BACKUPS -------------------------
        label = createSettingsLabel(R.string.migrating)
        binding.settingsHolder.addView(label)
        setupBackupSection()
    }

    /*override fun onPause() {
        super.onPause()
        mStoredAccentColor = getProperAccentColor()
    }*/

    override fun onStop() {
        super.onStop()
        val reminders = sortedSetOf(config.defaultReminder1, config.defaultReminder2, config.defaultReminder3).filter { it != REMINDER_OFF }
        config.defaultReminder1 = reminders.getOrElse(0) { REMINDER_OFF }
        config.defaultReminder2 = reminders.getOrElse(1) { REMINDER_OFF }
        config.defaultReminder3 = reminders.getOrElse(2) { REMINDER_OFF }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == GET_RINGTONE_URI && resultCode == RESULT_OK && resultData != null) {
            val newAlarmSound = storeNewYourAlarmSound(resultData)
            updateReminderSound(newAlarmSound)
        } else if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        }
    }

    private fun createHolder() = ItemSettingsHolderBinding.inflate(layoutInflater, null, false)

    private fun createSettingsLabel(resId: Int): TextView {
        val labelBinding = ItemSettingsLabelBinding.inflate(layoutInflater, null, false)
        labelBinding.settingsLabel.setText(resId)
        labelBinding.settingsLabel.setTextColor(getProperAccentColor())
        // background.applyColorFilter(getProperBackgroundColor().getContrastColor())
        return labelBinding.root
    }
    /** @param label: Resource id value */
    private fun createSettingsCheckbox(label: Int): ItemSettingsCheckboxBinding {
        val checkbox = ItemSettingsCheckboxBinding.inflate(layoutInflater, null, false)
        checkbox.settingsCheckbox.setText(label)
        return checkbox
    }
    /** @param label: Resource id value */
    private fun createSettingsSingleView(label: Int): ItemSettingsSingleTextviewBinding {
        val view = ItemSettingsSingleTextviewBinding.inflate(layoutInflater, null, false)
        view.settingsItemLabel.setText(label)
        return view
    }
    /** @param label: Resource id value
     *  @param value: String value */
    private fun createSettingsDoubleView(label: Int, value: String): ItemSettingsDoubleTextviewBinding {
        val view = ItemSettingsDoubleTextviewBinding.inflate(layoutInflater, null, false)
        view.settingsLabel.setText(label)
        view.settingsValue.text = value
        return view
    }

    private fun createSettingsColor(label: Int, color: Int): ItemSettingsColorBinding {
        val view = ItemSettingsColorBinding.inflate(layoutInflater, null, false)
        view.settingsColorTextLabel.setText(label)
        view.settingsColor.setFillWithStroke(color, getProperBackgroundColor())
        return view
    }

    private fun setupColorCustomization() {
        val holderBinding = ItemSettingsHolderBinding.inflate(layoutInflater, null, false)
        // customize colors
        var item = createSettingsSingleView(R.string.customize_colors)
        item.settingsItemHolder.setOnClickListener { startCustomizationActivity() }
        holderBinding.settingsWrapper.addView(item.root)
        // customize widget colors
        item = createSettingsSingleView(R.string.customize_widget_colors)
        item.settingsItemHolder.setOnClickListener {
            Intent(this, WidgetListConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
        holderBinding.settingsWrapper.addView(item.root)
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupGeneralSettings() {
        val holderBinding = ItemSettingsHolderBinding.inflate(layoutInflater, null, false)
        var checkbox: ItemSettingsCheckboxBinding
        val doubleView: ItemSettingsDoubleTextviewBinding
        // Use English Language
        if ((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus()) {
            checkbox = createSettingsCheckbox(R.string.use_english_language)
            checkbox.apply {
                settingsCheckbox.isChecked = config.useEnglish
                settingsCheckboxHolder.setOnClickListener {
                    settingsCheckbox.toggle()
                    config.useEnglish = settingsCheckbox.isChecked
                    exitProcess(0)
                }
            }
            holderBinding.root.addView(checkbox.root)
            // Language
            doubleView = createSettingsDoubleView(R.string.language, Locale.getDefault().displayLanguage)
            doubleView.settingsDoubleViewHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
            holderBinding.root.addView(doubleView.root)
        }
        // Manage Event Types
        var singleView = createSettingsSingleView(R.string.manage_event_types)
        singleView.settingsItemHolder.setOnClickListener {
            startActivity(Intent(this, ManageEventTypesActivity::class.java))
        }
        holderBinding.root.addView(singleView.root)
        // Manage Quick Filter Event Types
        singleView = createSettingsSingleView(R.string.manage_quick_filter_event_types)
        singleView.settingsItemHolder.setOnClickListener {
            SelectQuickFilterEventTypesDialog(this)
        }
        holderBinding.root.addView(singleView.root)
        // Use 24 hour Time
        checkbox = createSettingsCheckbox(R.string.use_24_hour_time_format)
        checkbox.apply {
            settingsCheckbox.isChecked = config.use24HourFormat
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.use24HourFormat = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Start week on Sunday
        checkbox = createSettingsCheckbox(R.string.sunday_first)
        checkbox.apply {
            settingsCheckbox.isChecked = config.isSundayFirst
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.isSundayFirst = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Highlight weekends color
        val colorBinding = createSettingsColor(R.string.highlight_weekends_color, config.highlightWeekendsColor)
        colorBinding.settingsColorHolder.setOnClickListener {
            ColorPickerDialog(this, config.highlightWeekendsColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    config.highlightWeekendsColor = color
                    colorBinding.settingsColor.setFillWithStroke(color, getProperBackgroundColor())
                }
            }
        }
        // Highlight Weekends on some views
        checkbox = createSettingsCheckbox(R.string.highlight_weekends)
        checkbox.apply {
            settingsCheckbox.isChecked = config.highlightWeekends
            colorBinding.settingsColorHolder.beVisibleIf(config.highlightWeekends)
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.highlightWeekends = settingsCheckbox.isChecked
                colorBinding.settingsColorHolder.beVisibleIf(settingsCheckbox.isChecked)
            }
        }
        holderBinding.root.addView(checkbox.root)
        holderBinding.root.addView(colorBinding.root)
        // Allow Using debug (for current instance)
        checkbox = createSettingsCheckbox(R.string.allow_dbg)
        checkbox.apply {
            settingsCheckbox.isChecked = config.allowAppDbg
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.allowAppDbg = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Add all to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupRemindersSection() {
        val holderBinding = createHolder()
        // Customize notifications
        if (isOreoPlus()) {
            val notification = createSettingsSingleView(R.string.customize_notifications)
            notification.settingsItemHolder.setOnClickListener {
                launchCustomizeNotificationsIntent()
            }
            holderBinding.root.addView(notification.root)
        }
        // Reminder Sound
        else {
            val sound = createSettingsDoubleView(R.string.reminder_sound, config.reminderSoundTitle)
            sound.settingsDoubleViewHolder.setOnClickListener {
                SelectAlarmSoundDialog(this, config.reminderSoundUri, config.reminderAudioStream, GET_RINGTONE_URI, RingtoneManager.TYPE_NOTIFICATION, false,
                    onAlarmPicked = {
                        if (it != null) {
                            updateReminderSound(it)
                        }
                    }, onAlarmSoundDeleted = {
                        if (it.uri == config.reminderSoundUri) {
                            val defaultAlarm = getDefaultAlarmSound(RingtoneManager.TYPE_NOTIFICATION)
                            updateReminderSound(defaultAlarm)
                        }
                    })
            }
            holderBinding.root.addView(sound.root)
        }
        // Audio stream used by reminders
        val stream = createSettingsDoubleView(R.string.reminder_stream, getAudioStreamText())
        stream.settingsDoubleViewHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_stream)),
                RadioItem(AudioManager.STREAM_SYSTEM, getString(R.string.system_stream)),
                RadioItem(AudioManager.STREAM_NOTIFICATION, getString(R.string.notification_stream)),
                RadioItem(AudioManager.STREAM_RING, getString(R.string.ring_stream))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.reminderAudioStream) {
                config.reminderAudioStream = it as Int
                stream.settingsValue.text = getAudioStreamText()
            }
        }
        holderBinding.root.addView(stream.root)
        // Vibrate on reminder notification
        var checkbox = createSettingsCheckbox(R.string.vibrate)
        checkbox.apply {
            settingsCheckbox.isChecked = config.vibrateOnReminder
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.vibrateOnReminder = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Loop reminders until dismissed
        checkbox = createSettingsCheckbox(R.string.loop_reminders)
        checkbox.apply {
            settingsCheckbox.isChecked = config.loopReminders
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.loopReminders = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Snooze time
        val snooze = createSettingsDoubleView(R.string.snooze_time, formatMinutesToTimeString(config.snoozeTime))
        snooze.settingsDoubleViewHolder.beVisibleIf(config.useSameSnooze)
        snooze.settingsDoubleViewHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.snoozeTime, true) {
                config.snoozeTime = it / 60
                snooze.settingsValue.text = formatMinutesToTimeString(config.snoozeTime)
            }
        }
        // Always use same snooze time
        checkbox = createSettingsCheckbox(R.string.use_same_snooze)
        checkbox.apply {
            settingsCheckbox.isChecked = config.useSameSnooze
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.useSameSnooze = settingsCheckbox.isChecked
                snooze.settingsDoubleViewHolder.beVisibleIf(config.useSameSnooze)
            }
        }
        holderBinding.root.addView(checkbox.root)
        holderBinding.root.addView(snooze.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupCaldavSection() {
        val holderBinding = createHolder()
        // Pull to refresh
        val refresh = createSettingsCheckbox(R.string.enable_pull_to_refresh)
        refresh.apply {
            settingsCheckbox.isChecked = config.pullToRefresh
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.pullToRefresh = settingsCheckbox.isChecked
            }
        }
        // Manage Synced calendars
        val manageCalendars = createSettingsSingleView(R.string.manage_synced_calendars)
        manageCalendars.settingsItemHolder.setOnClickListener {
            showCalendarPicker()
        }
        // Caldav
        val caldav = createSettingsCheckbox(R.string.caldav_sync)
        caldav.apply {
            settingsCheckbox.isChecked = config.caldavSync
            refresh.settingsCheckboxHolder.beVisibleIf(config.caldavSync)
            manageCalendars.settingsItemHolder.beVisibleIf(config.caldavSync)
            settingsCheckboxHolder.setOnClickListener {
                if (config.caldavSync) {
                    toggleCaldavSync(false)
                    refresh.settingsCheckboxHolder.beGone()
                    manageCalendars.settingsItemHolder.beGone()
                    settingsCheckbox.isChecked = false
                } else {
                    handlePermission(PERMISSION_WRITE_CALENDAR) { haveCalenderWrite ->
                        if (haveCalenderWrite) {
                            handlePermission(PERMISSION_READ_CALENDAR) {
                                if (it) {
                                    handleNotificationPermission { granted ->
                                        if (granted) {
                                            val result = toggleCaldavSync(true)
                                            refresh.settingsCheckboxHolder.beVisibleIf(result)
                                            manageCalendars.settingsItemHolder.beVisibleIf(result)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        holderBinding.root.addView(caldav.root)
        holderBinding.root.addView(refresh.root)
        holderBinding.root.addView(manageCalendars.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupEventsSection() {
        val holderBinding = createHolder()
        // Dim Past events
        var checkbox = createSettingsCheckbox(R.string.dim_past_events)
        checkbox.apply {
            settingsCheckbox.isChecked = config.dimPastEvents
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.dimPastEvents = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Allow Changing event time zones
        checkbox = createSettingsCheckbox(R.string.allow_changing_time_zones)
        checkbox.apply {
            settingsCheckbox.isChecked = config.allowChangingTimeZones
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.allowChangingTimeZones = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Sync contact Birthdays
        checkbox = createSettingsCheckbox(R.string.sync_contact_birthdays)
        checkbox.apply {
            settingsCheckbox.isChecked = config.addBirthdaysAutomatically
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.addBirthdaysAutomatically = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Sync contact Anniversaries
        checkbox = createSettingsCheckbox(R.string.sync_contact_anniversaries)
        checkbox.apply {
            settingsCheckbox.isChecked = config.addAnniversariesAutomatically
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.addAnniversariesAutomatically = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Delete events older than
        val olderThan = createSettingsDoubleView(R.string.delete_events_older_then, "")
        olderThan.settingsDoubleViewHolder.beVisibleIf(config.deleteOldEvents)
        olderThan.settingsValue.text = String.format("%d Years", config.deleteEventsOlderThen)
        olderThan.settingsDoubleViewHolder.setOnClickListener {
            YearPickerDialog(this) {value ->
                config.deleteEventsOlderThen = if (value < 1) 1 else value
                olderThan.settingsValue.text = String.format("%d Years", value)
            }
        }
        // Delete events older than X years
        checkbox = createSettingsCheckbox(R.string.delete_older_events)
        checkbox.apply {
            settingsCheckbox.isChecked = config.deleteOldEvents
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.deleteOldEvents = settingsCheckbox.isChecked
                olderThan.settingsDoubleViewHolder.beVisibleIf(settingsCheckbox.isChecked)
            }
        }
        holderBinding.root.addView(checkbox.root)
        holderBinding.root.addView(olderThan.root)
        // Delete all events
        val deleteAll = createSettingsSingleView(R.string.delete_all_events)
        deleteAll.settingsItemHolder.setOnClickListener {
            ConfirmationDialog(this, messageId = R.string.delete_all_events_confirmation) {
                eventsHelper.deleteAllEvents()
            }
        }
        holderBinding.root.addView(deleteAll.root)
        // Add events to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupNewEventsSection() {
        val holderBinding = createHolder()
        // Default Start time
        val startTime = createSettingsDoubleView(R.string.default_start_time, "")
        updateDefaultStartTimeText(startTime)
        startTime.settingsDoubleViewHolder.setOnClickListener {
            val currentDefaultTime = when (config.defaultStartTime) {
                DEFAULT_START_TIME_NEXT_FULL_HOUR -> DEFAULT_START_TIME_NEXT_FULL_HOUR
                DEFAULT_START_TIME_CURRENT_TIME -> DEFAULT_START_TIME_CURRENT_TIME
                else -> 0
            }

            val items = ArrayList<RadioItem>()
            items.add(RadioItem(DEFAULT_START_TIME_CURRENT_TIME, getString(R.string.current_time)))
            items.add(RadioItem(DEFAULT_START_TIME_NEXT_FULL_HOUR, getString(R.string.next_full_hour)))
            items.add(RadioItem(0, getString(R.string.other_time)))

            RadioGroupDialog(this@SettingsActivity, items, currentDefaultTime) {
                if (it as Int == DEFAULT_START_TIME_NEXT_FULL_HOUR || it == DEFAULT_START_TIME_CURRENT_TIME) {
                    config.defaultStartTime = it
                    updateDefaultStartTimeText(startTime)
                } else {
                    val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                        config.defaultStartTime = hourOfDay * 60 + minute
                        updateDefaultStartTimeText(startTime)
                    }

                    val currentDateTime = DateTime.now()
                    TimePickerDialog(
                        this,
                        getTimePickerDialogTheme(),
                        timeListener,
                        currentDateTime.hourOfDay,
                        currentDateTime.minuteOfHour,
                        config.use24HourFormat
                    ).show()
                }
            }
        }
        holderBinding.root.addView(startTime.root)
        // Default Duration
        val duration = createSettingsDoubleView(R.string.default_duration, "")
        updateDefaultDurationText(duration)
        duration.settingsDoubleViewHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, config.defaultDuration * 60) {
                val result = it / 60
                config.defaultDuration = result
                updateDefaultDurationText(duration)
            }
        }
        holderBinding.root.addView(duration.root)
        // Default Event type
        val eventType = createSettingsDoubleView(R.string.default_event_type, "")
        updateDefaultEventTypeText(eventType)
        eventType.settingsDoubleViewHolder.setOnClickListener {
            SelectEventTypeDialog(this, config.defaultEventTypeId, true, false, true, true) {
                config.defaultEventTypeId = it.id!!
                updateDefaultEventTypeText(eventType)
            }
        }
        holderBinding.root.addView(eventType.root)
        // Default reminder 1
        val reminder1 = createSettingsDoubleView(R.string.default_reminder_1, getFormattedMinutes(config.defaultReminder1))
        reminder1.settingsDoubleViewHolder.beVisibleIf(!config.usePreviousEventReminders)
        reminder1.settingsDoubleViewHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder1) {
                config.defaultReminder1 = if (it == -1 || it == 0) it else it / 60
                reminder1.settingsValue.text = getFormattedMinutes(config.defaultReminder1)
            }
        }
        // Default reminder 2
        val reminder2 = createSettingsDoubleView(R.string.default_reminder_1, getFormattedMinutes(config.defaultReminder2))
        reminder2.settingsDoubleViewHolder.beVisibleIf(!config.usePreviousEventReminders)
        reminder2.settingsDoubleViewHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder2) {
                config.defaultReminder2 = if (it == -1 || it == 0) it else it / 60
                reminder2.settingsValue.text = getFormattedMinutes(config.defaultReminder2)
            }
        }
        // Default reminder 3
        val reminder3 = createSettingsDoubleView(R.string.default_reminder_3, getFormattedMinutes(config.defaultReminder3))
        reminder3.settingsDoubleViewHolder.beVisibleIf(!config.usePreviousEventReminders)
        reminder3.settingsDoubleViewHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder3) {
                config.defaultReminder3 = if (it == -1 || it == 0) it else it / 60
                reminder3.settingsValue.text = getFormattedMinutes(config.defaultReminder3)
            }
        }
        // Use the last event's reminders as the default
        val checkbox = createSettingsCheckbox(R.string.use_last_event_reminders)
        checkbox.apply {
            settingsCheckbox.isChecked = config.usePreviousEventReminders
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.usePreviousEventReminders = settingsCheckbox.isChecked
                reminder1.settingsDoubleViewHolder.beVisibleIf(!settingsCheckbox.isChecked)
                reminder2.settingsDoubleViewHolder.beVisibleIf(!settingsCheckbox.isChecked)
                reminder3.settingsDoubleViewHolder.beVisibleIf(!settingsCheckbox.isChecked)
            }
        }
        holderBinding.root.addView(checkbox.root)
        holderBinding.root.addView(reminder1.root)
        holderBinding.root.addView(reminder2.root)
        holderBinding.root.addView(reminder3.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupTaskSection() {
        val holderBinding = createHolder()
        // Allow Creating Tasks
        var checkbox = createSettingsCheckbox(R.string.allow_creating_tasks)
        checkbox.apply {
            settingsCheckbox.isChecked = config.allowCreatingTasks
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.allowCreatingTasks = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Dim Completed Tasks
        checkbox = createSettingsCheckbox(R.string.dim_completed_tasks)
        checkbox.apply {
            settingsCheckbox.isChecked = config.dimCompletedTasks
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.dimCompletedTasks = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        //Default Task type
        val defaultTask = createSettingsDoubleView(R.string.default_task_type, "")
        updateDefaultTaskTypeText(defaultTask)
        defaultTask.settingsDoubleViewHolder.setOnClickListener {
            SelectEventTypeDialog(this, config.defaultTaskTypeId, true, false, true, true) {
                config.defaultTaskTypeId = it.id!!
                updateDefaultTaskTypeText(defaultTask)
            }
        }
        holderBinding.root.addView(defaultTask.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupEventsListsSection() {
        val holderBinding = createHolder()
        // Display events from the past
        var displayPastEvents = config.displayPastEvents
        val pastEvents = createSettingsDoubleView(R.string.display_past_events, getDisplayPastEventsText(displayPastEvents))
        pastEvents.settingsDoubleViewHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, displayPastEvents * 60) {
                val result = it / 60
                displayPastEvents = result
                config.displayPastEvents = result
                pastEvents.settingsValue.text = getDisplayPastEventsText(result)
            }
        }
        holderBinding.root.addView(pastEvents.root)
        // Replace event description with location
        val replace = createSettingsCheckbox(R.string.replace_description_with_location)
        replace.apply {
            settingsCheckbox.isChecked = config.replaceDescription
            settingsCheckboxHolder.beVisibleIf(config.displayDescription)
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.replaceDescription = settingsCheckbox.isChecked
            }
        }
        // Display description or location
        var checkbox = createSettingsCheckbox(R.string.display_description_or_location)
        checkbox.apply {
            settingsCheckbox.isChecked = config.displayDescription
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.displayDescription = settingsCheckbox.isChecked
                replace.settingsCheckboxHolder.beVisibleIf(settingsCheckbox.isChecked)
            }
        }
        holderBinding.root.addView(checkbox.root)
        holderBinding.root.addView(replace.root)
        // Show label for Birthdays & Anniversaries
        checkbox = createSettingsCheckbox(R.string.show_birth_ann_description)
        checkbox.apply {
            settingsCheckbox.isChecked = config.showBirthdayAnniversaryDescription
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.showBirthdayAnniversaryDescription = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupWeeklyViewSection() {
        val holderBinding = createHolder()
        // Start Day at
        val day = createSettingsDoubleView(R.string.start_day_at, getHoursString(config.startWeeklyAt))
        day.settingsDoubleViewHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..16).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this, items, config.startWeeklyAt) {
                config.startWeeklyAt = it as Int
                day.settingsValue.text = getHoursString(it)
            }
        }
        holderBinding.root.addView(day.root)
        // Show events spanning across midnight at the top bar
        var checkbox = createSettingsCheckbox(R.string.midnight_spanning)
        checkbox.apply {
            settingsCheckbox.isChecked = config.showMidnightSpanningEventsAtTop
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.showMidnightSpanningEventsAtTop = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Allow customizing day count
        checkbox = createSettingsCheckbox(R.string.allow_customizing_day_count)
        checkbox.apply {
            settingsCheckbox.isChecked = config.allowCustomizeDayCount
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.allowCustomizeDayCount = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Start week with the current day
        checkbox = createSettingsCheckbox(R.string.start_week_with_current_day)
        checkbox.apply {
            settingsCheckbox.isChecked = config.startWeekWithCurrentDay
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.startWeekWithCurrentDay = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupMonthlyViewSection() {
        val holderBinding = createHolder()
        // Show Week Numbers
        var checkbox = createSettingsCheckbox(R.string.week_numbers)
        checkbox.apply {
            settingsCheckbox.isChecked = config.showWeekNumbers
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.showWeekNumbers = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Show a grid
        checkbox = createSettingsCheckbox(R.string.show_a_grid)
        checkbox.apply {
            settingsCheckbox.isChecked = config.showGrid
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.showGrid = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // Add to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupWidgetsSection() {
        val holderBinding = createHolder()
        // Font Size
        val fontSize = createSettingsDoubleView(R.string.font_size, getFontSizeText())
        fontSize.settingsDoubleViewHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_EXTRA_SMALL, getString(R.string.extra_small)),
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                fontSize.settingsValue.text = getFontSizeText()
                updateWidgets()
            }
        }
        holderBinding.root.addView(fontSize.root)
        // Show Event description
        val checkbox = createSettingsCheckbox(R.string.show_widget_description)
        checkbox.apply {
            settingsCheckbox.isChecked = config.showWidgetDescription
            settingsCheckboxHolder.setOnClickListener {
                settingsCheckbox.toggle()
                config.showWidgetDescription = settingsCheckbox.isChecked
            }
        }
        holderBinding.root.addView(checkbox.root)
        // View to open from event list widget
        val view = createSettingsDoubleView(R.string.view_to_open_from_widget, getDefaultViewText())
        view.settingsDoubleViewHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
                RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
                RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
                RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
                RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
                RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list)),
                RadioItem(LAST_VIEW, getString(R.string.last_view))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.listWidgetViewToOpen) {
                config.listWidgetViewToOpen = it as Int
                view.settingsValue.text = getDefaultViewText()
                updateWidgets()
            }
        }
        holderBinding.root.addView(view.root)
        // Add holder to main view
        binding.settingsHolder.addView(holderBinding.root)
    }

    private fun setupBackupSection() {
        val holderBinding = createHolder()
        // Export Settings
        var view = createSettingsSingleView(R.string.export_settings)
        view.settingsItemHolder.setOnClickListener { exportSettings() }
        holderBinding.root.addView(view.root)
        // Import Settings
        view = createSettingsSingleView(R.string.import_settings)
        view.settingsItemHolder.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"

                    try {
                        startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            } else {
                handlePermission(PERMISSION_READ_STORAGE) {
                    if (it) {
                        FilePickerDialog(this) {
                            ensureBackgroundThread {
                                parseFile(File(it).inputStream())
                            }
                        }
                    }
                }
            }
        }
        holderBinding.root.addView(view.root)
        // Add to main layout
        binding.settingsHolder.addView(holderBinding.root)
    }

    /*
    private fun checkPrimaryColor() {
        if (getProperAccentColor() != mStoredAccentColor) {
            ensureBackgroundThread {
                val eventTypes = eventsHelper.getEventTypesSync()
                if (eventTypes.filter { it.caldavCalendarId == 0 }.size == 1) {
                    val eventType = eventTypes.first { it.caldavCalendarId == 0 }
                    eventType.color = getProperAccentColor()
                    eventsHelper.insertOrUpdateEventTypeSync(eventType)
                }
            }
        }
    }*/

    private fun updateReminderSound(alarmSound: AlarmSound) {
        config.reminderSoundTitle = alarmSound.title
        config.reminderSoundUri = alarmSound.uri
        //binding.settingsReminderSound.text = alarmSound.title
    }

    private fun toggleCaldavSync(enable: Boolean): Boolean {
        var success = false
        if (enable) {
            success = showCalendarPicker()
        } else {
            config.caldavSync = false

            ensureBackgroundThread {
                config.getSyncedCalendarIdsAsList().forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                }
                eventTypesDB.deleteEventTypesWithCalendarId(config.getSyncedCalendarIdsAsList())
                //updateDefaultEventTypeText()
            }
        }
        return success
    }

    private fun showCalendarPicker(): Boolean {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()
        var success = false
        SelectCalendarsDialog(this) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            if (newCalendarIds.isEmpty() && !config.caldavSync) {
                return@SelectCalendarsDialog
            }
            success = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            if (success) {
                toast(R.string.syncing)
            }

            ensureBackgroundThread {
                if (newCalendarIds.isNotEmpty()) {
                    val existingEventTypeNames = eventsHelper.getEventTypesSync().map {
                        it.getDisplayTitle().lowercase(Locale.getDefault())
                    } as ArrayList<String>

                    getSyncedCalDAVCalendars().forEach {
                        val calendarTitle = it.getFullTitle()
                        if (!existingEventTypeNames.contains(calendarTitle.lowercase(Locale.getDefault()))) {
                            val eventType = EventType(null, it.displayName, it.color, it.id, it.displayName, it.accountName)
                            existingEventTypeNames.add(calendarTitle.lowercase(Locale.getDefault()))
                            eventsHelper.insertOrUpdateEventType(this, eventType)
                        }
                    }

                    syncCalDAVCalendars {
                        calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                            if (success) {
                                toast(R.string.synchronization_completed)
                            }
                        }
                    }
                }

                val removedCalendarIds = oldCalendarIds.filter { !newCalendarIds.contains(it) }
                removedCalendarIds.forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                    eventsHelper.getEventTypeWithCalDAVCalendarId(it)?.apply {
                        eventsHelper.deleteEventTypes(arrayListOf(this), true)
                    }
                }

                eventTypesDB.deleteEventTypesWithCalendarId(removedCalendarIds)
                //updateDefaultEventTypeText()
            }
        }
        return success
    }

    private fun getAudioStreamText() = getString(
        when (config.reminderAudioStream) {
            AudioManager.STREAM_ALARM -> R.string.alarm_stream
            AudioManager.STREAM_SYSTEM -> R.string.system_stream
            AudioManager.STREAM_NOTIFICATION -> R.string.notification_stream
            else -> R.string.ring_stream
        }
    )

    private fun getHoursString(hours: Int) = String.format("%02d:00", hours)

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0) {
            getString(R.string.never)
        } else {
            getFormattedMinutes(displayPastEvents, false)
        }
    }

    private fun getDefaultViewText() = getString(
        when (config.listWidgetViewToOpen) {
            DAILY_VIEW -> R.string.daily_view
            WEEKLY_VIEW -> R.string.weekly_view
            MONTHLY_VIEW -> R.string.monthly_view
            MONTHLY_DAILY_VIEW -> R.string.monthly_daily_view
            YEARLY_VIEW -> R.string.yearly_view
            EVENTS_LIST_VIEW -> R.string.simple_event_list
            else -> R.string.last_view
        }
    )

    private fun updateDefaultTaskTypeText(doubleView: ItemSettingsDoubleTextviewBinding) {
        if (config.defaultTaskTypeId == -1L) {
            runOnUiThread {
                doubleView.settingsValue.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(config.defaultTaskTypeId)
                if (eventType != null) {
                    runOnUiThread {
                        doubleView.settingsValue.text = eventType.title
                    }
                } else {
                    config.defaultTaskTypeId = -1
                    updateDefaultTaskTypeText(doubleView)
                }
            }
        }
    }

   private fun updateDefaultStartTimeText(view: ItemSettingsDoubleTextviewBinding) {
        when (config.defaultStartTime) {
            DEFAULT_START_TIME_CURRENT_TIME -> view.settingsValue.text = getString(R.string.current_time)
            DEFAULT_START_TIME_NEXT_FULL_HOUR -> view.settingsValue.text = getString(R.string.next_full_hour)
            else -> {
                val hours = config.defaultStartTime / 60
                val minutes = config.defaultStartTime % 60
                view.settingsValue.text = String.format("%02d:%02d", hours, minutes)
            }
        }
   }

    private fun updateDefaultDurationText(view: ItemSettingsDoubleTextviewBinding) {
        val duration = config.defaultDuration
        view.settingsValue.text = if (duration == 0) {
            "0 ${getString(R.string.minutes_raw)}"
        } else {
            getFormattedMinutes(duration, false)
        }
    }

    private fun updateDefaultEventTypeText(doubleView: ItemSettingsDoubleTextviewBinding) {
        if (config.defaultEventTypeId == -1L) {
            runOnUiThread {
                doubleView.settingsValue.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(config.defaultEventTypeId)
                if (eventType != null) {
                    config.lastUsedCaldavCalendarId = eventType.caldavCalendarId
                    runOnUiThread {
                        doubleView.settingsValue.text = eventType.title
                    }
                } else {
                    config.defaultEventTypeId = -1L
                    updateDefaultEventTypeText(doubleView)
                }
            }
        }
    }

    private fun exportSettings() {
            val configItems = LinkedHashMap<String, Any>().apply {
                //------------ Secure Commons Options -------------------
                put(LAST_VERSION, config.lastVersion)
                put(IS_USING_SHARED_THEME, config.isUsingSharedTheme)
                put(TEXT_COLOR, config.textColor)
                put(BACKGROUND_COLOR, config.backgroundColor)
                put(PRIMARY_COLOR, config.primaryColor)
                put(ACCENT_COLOR, config.accentColor)
                put(CUSTOM_TEXT_COLOR, config.customTextColor)
                put(CUSTOM_BACKGROUND_COLOR, config.customBackgroundColor)
                put(CUSTOM_PRIMARY_COLOR, config.customPrimaryColor)
                put(CUSTOM_ACCENT_COLOR, config.customAccentColor)
                put(USE_ENGLISH, config.useEnglish)
                put(WAS_USE_ENGLISH_TOGGLED, config.wasUseEnglishToggled)
                put(WIDGET_BG_COLOR, config.widgetBgColor)
                put(WIDGET_TEXT_COLOR, config.widgetTextColor)
                put(WIDGET_DAY_COLOR, config.widgetDayColor)
                put(WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN, config.wasCustomThemeSwitchDescriptionShown)
                put(IS_USING_SYSTEM_THEME, config.isUsingSystemTheme)
                put(IS_USING_AUTO_THEME, config.isUsingAutoTheme)
                put(FONT_SIZE, config.fontSize)
                put(USE_SAME_SNOOZE, config.useSameSnooze)
                put(SNOOZE_TIME, config.snoozeTime)
                put(USE_24_HOUR_FORMAT, config.use24HourFormat)
                put(SUNDAY_FIRST, config.isSundayFirst)
                //---------END SECURE Commons Options -----------------------
                //------- liturgical calendar options -------------------
                put(WEEK_NUMBERS, config.showWeekNumbers)
                put(START_WEEKLY_AT, config.startWeeklyAt)
                put(START_WEEK_WITH_CURRENT_DAY, config.startWeekWithCurrentDay)
                put(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, config.showMidnightSpanningEventsAtTop)
                put(ALLOW_CUSTOMIZE_DAY_COUNT, config.allowCustomizeDayCount)
                put(VIBRATE, config.vibrateOnReminder)
                put(REMINDER_SOUND_URI, config.reminderSoundUri)
                put(REMINDER_SOUND_TITLE, config.reminderSoundTitle)
                put(LAST_SOUND_URI, config.lastSoundUri)
                put(VIEW, config.storedView)
                put(LAST_EVENT_REMINDER_MINUTES, config.lastEventReminderMinutes1)
                put(LAST_EVENT_REMINDER_MINUTES_2, config.lastEventReminderMinutes2)
                put(LAST_EVENT_REMINDER_MINUTES_3, config.lastEventReminderMinutes3)
                put(DISPLAY_PAST_EVENTS, config.displayPastEvents)
                put(LIST_WIDGET_VIEW_TO_OPEN, config.listWidgetViewToOpen)
                put(LIST_WIDGET_DAY_POSITION, config.listWidgetDayPosition)
                put(CALDAV_SYNC, config.caldavSync)
                put(CALDAV_SYNCED_CALENDAR_IDS, config.caldavSyncedCalendarIds)
                put(LAST_USED_LOCAL_EVENT_TYPE_ID, config.lastUsedLocalEventTypeId)
                put(LAST_USED_TASK_TYPE_ID, config.lastUsedTaskTypeId)
                put(REMINDER_AUDIO_STREAM, config.reminderAudioStream)
                put(REPLACE_DESCRIPTION, config.replaceDescription)
                put(DISPLAY_DESCRIPTION, config.displayDescription)
                put(SHOW_GRID, config.showGrid)
                put(LOOP_REMINDERS, config.loopReminders)
                put(DIM_PAST_EVENTS, config.dimPastEvents)
                put(DIM_COMPLETED_TASKS, config.dimCompletedTasks)
                put(USE_PREVIOUS_EVENT_REMINDERS, config.usePreviousEventReminders)
                put(DEFAULT_REMINDER_1, config.defaultReminder1)
                put(DEFAULT_REMINDER_2, config.defaultReminder2)
                put(DEFAULT_REMINDER_3, config.defaultReminder3)
                put(PULL_TO_REFRESH, config.pullToRefresh)
                put(LAST_VIBRATE_ON_REMINDER, config.lastVibrateOnReminder)
                put(DEFAULT_START_TIME, config.defaultStartTime)
                put(DEFAULT_DURATION, config.defaultDuration)
                put(DEFAULT_EVENT_TYPE_ID, config.defaultEventTypeId)
                put(DEFAULT_TASK_TYPE_ID, config.defaultTaskTypeId)
                put(ALLOW_CHANGING_TIME_ZONES, config.allowChangingTimeZones)
                put(ADD_BIRTHDAYS_AUTOMATICALLY, config.addBirthdaysAutomatically)
                put(ADD_ANNIVERSARIES_AUTOMATICALLY, config.addAnniversariesAutomatically)
                put(WEEKLY_VIEW_DAYS, config.weeklyViewDays)
                put(HIGHLIGHT_WEEKENDS, config.highlightWeekends)
                put(SHOW_WHATS_NEW, config.showWhatsNewDialog)
                put(HIGHLIGHT_WEEKENDS_COLOR, config.highlightWeekendsColor)
                put(ALLOW_CREATING_TASKS, config.allowCreatingTasks)
                put(SHOW_BIRTH_ANN_DESCRIPTION, config.showBirthdayAnniversaryDescription)
                put(SHOW_WIDGET_DESCRIPTION, config.showWidgetDescription)
            }

            exportSettings(configItems)
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var importedItems = 0
        val configValues = LinkedHashMap<String, Any>()
        inputStream.bufferedReader().use {
            while (true) {
                try {
                    val line = it.readLine() ?: break
                    val split = line.split("=".toRegex(), 2)
                    if (split.size == 2) {
                        configValues[split[0]] = split[1]
                    }
                    importedItems++
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

        for ((key, value) in configValues) {
            when (key) {
                //------ Secure Commons Options ----------------------------
                LAST_VERSION -> config.lastVersion = value.toInt()
                IS_USING_SHARED_THEME -> config.isUsingSharedTheme = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                FONT_SIZE -> config.fontSize = value.toInt()
                USE_SAME_SNOOZE -> config.useSameSnooze = value.toBoolean()
                SNOOZE_TIME -> config.snoozeTime = value.toInt()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                SUNDAY_FIRST -> config.isSundayFirst = value.toBoolean()
                //----- End Secure Commons Options --------------------------
                //----- TLC Options -----------------------------------------
                WEEK_NUMBERS -> config.showWeekNumbers = value.toBoolean()
                START_WEEKLY_AT -> config.startWeeklyAt = value.toInt()
                START_WEEK_WITH_CURRENT_DAY -> config.startWeekWithCurrentDay = value.toBoolean()
                SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP -> config.showMidnightSpanningEventsAtTop = value.toBoolean()
                ALLOW_CUSTOMIZE_DAY_COUNT -> config.allowCustomizeDayCount = value.toBoolean()
                VIBRATE -> config.vibrateOnReminder = value.toBoolean()
                REMINDER_SOUND_URI -> config.reminderSoundUri = value.toString()
                REMINDER_SOUND_TITLE -> config.reminderSoundTitle = value.toString()
                LAST_SOUND_URI -> config.lastSoundUri = value.toString()
                VIEW -> config.storedView = value.toInt()
                LAST_EVENT_REMINDER_MINUTES -> config.lastEventReminderMinutes1 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_2 -> config.lastEventReminderMinutes2 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_3 -> config.lastEventReminderMinutes3 = value.toInt()
                DISPLAY_PAST_EVENTS -> config.displayPastEvents = value.toInt()
                LIST_WIDGET_VIEW_TO_OPEN -> config.listWidgetViewToOpen = value.toInt()
                CALDAV_SYNC -> config.caldavSync = value.toBoolean()
                CALDAV_SYNCED_CALENDAR_IDS -> config.caldavSyncedCalendarIds = value.toString()
                LAST_USED_LOCAL_EVENT_TYPE_ID -> config.lastUsedLocalEventTypeId = value.toString().toLong()
                LAST_USED_TASK_TYPE_ID -> config.lastUsedTaskTypeId = value.toString().toLong()
                REMINDER_AUDIO_STREAM -> config.reminderAudioStream = value.toInt()
                REPLACE_DESCRIPTION -> config.replaceDescription = value.toBoolean()
                DISPLAY_DESCRIPTION -> config.displayDescription = value.toBoolean()
                SHOW_GRID -> config.showGrid = value.toBoolean()
                LOOP_REMINDERS -> config.loopReminders = value.toBoolean()
                DIM_PAST_EVENTS -> config.dimPastEvents = value.toBoolean()
                DIM_COMPLETED_TASKS -> config.dimCompletedTasks = value.toBoolean()
                USE_PREVIOUS_EVENT_REMINDERS -> config.usePreviousEventReminders = value.toBoolean()
                DEFAULT_REMINDER_1 -> config.defaultReminder1 = value.toInt()
                DEFAULT_REMINDER_2 -> config.defaultReminder2 = value.toInt()
                DEFAULT_REMINDER_3 -> config.defaultReminder3 = value.toInt()
                PULL_TO_REFRESH -> config.pullToRefresh = value.toBoolean()
                DEFAULT_START_TIME -> config.defaultStartTime = value.toInt()
                DEFAULT_DURATION -> config.defaultDuration = value.toInt()
                DEFAULT_EVENT_TYPE_ID -> config.defaultEventTypeId = value.toString().toLong()
                DEFAULT_TASK_TYPE_ID -> config.defaultTaskTypeId = value.toString().toLong()
                ALLOW_CHANGING_TIME_ZONES -> config.allowChangingTimeZones = value.toBoolean()
                ADD_BIRTHDAYS_AUTOMATICALLY -> config.addBirthdaysAutomatically = value.toBoolean()
                ADD_ANNIVERSARIES_AUTOMATICALLY -> config.addAnniversariesAutomatically = value.toBoolean()
                WEEKLY_VIEW_DAYS -> config.weeklyViewDays = value.toInt()
                HIGHLIGHT_WEEKENDS -> config.highlightWeekends = value.toBoolean()
                SHOW_WHATS_NEW -> config.showWhatsNewDialog = value.toBoolean()
                HIGHLIGHT_WEEKENDS_COLOR -> config.highlightWeekendsColor = value.toInt()
                ALLOW_CREATING_TASKS -> config.allowCreatingTasks = value.toBoolean()
                SHOW_BIRTH_ANN_DESCRIPTION -> config.showBirthdayAnniversaryDescription = value.toBoolean()
                SHOW_WIDGET_DESCRIPTION -> config.showWidgetDescription = value.toBoolean()
            }
        }

        runOnUiThread {
            val msg = if (configValues.size > 0) R.string.settings_imported_successfully else R.string.no_entries_for_importing
            toast(msg)

            setupSettingItems()
            updateWidgets()
        }
    }
}
