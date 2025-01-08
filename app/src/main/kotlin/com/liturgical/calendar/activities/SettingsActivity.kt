package com.liturgical.calendar.activities

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.Toast
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.ActivitySettingsBinding
import com.liturgical.calendar.dialogs.SelectCalendarsDialog
import com.liturgical.calendar.dialogs.SelectEventTypeDialog
import com.liturgical.calendar.dialogs.SelectQuickFilterEventTypesDialog
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

    private var mStoredAccentColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mStoredAccentColor = getProperAccentColor()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)
        setupSettingItems()
    }

    private fun setupSettingItems() {
        setupCustomizeColors()
        setupCustomizeNotifications()
        setupUseEnglish()
        setupLanguage()
        setupManageEventTypes()
        setupManageQuickFilterEventTypes()
        setupHourFormat()
        setupAllowCreatingTasks()
        setupSundayFirst()
        setupHighlightWeekends()
        setupShowWhatsNew()
        setupHighlightWeekendsColor()
        setupDeleteAllEvents()
        setupDisplayDescription()
        setupReplaceDescription()
        setupBirthAnnLabel()
        setupWeekNumbers()
        setupShowGrid()
        setupWeeklyStart()
        setupMidnightSpanEvents()
        setupAllowCustomizeDayCount()
        setupStartWeekWithCurrentDay()
        setupVibrate()
        setupReminderSound()
        setupReminderAudioStream()
        setupUseSameSnooze()
        setupLoopReminders()
        setupSnoozeTime()
        setupCaldavSync()
        setupManageSyncedCalendars()
        setupDefaultStartTime()
        setupDefaultDuration()
        setupDefaultEventType()
        setupPullToRefresh()
        setupDefaultReminder()
        setupDefaultReminder1()
        setupDefaultReminder2()
        setupDefaultReminder3()
        setupDisplayPastEvents()
        setupFontSize()
        setupShowWidgetDescription()
        setupCustomizeWidgetColors()
        setupViewToOpenFromListWidget()
        setupDimEvents()
        setupDimCompletedTasks()
        setupSyncBirthdays()
        setupSyncAnniversaries()
        setupDefaultTaskType()
        setupAllowChangingTimeZones()
        updateTextColors(binding.settingsHolder)
        checkPrimaryColor()
        setupExportSettings()
        setupImportSettings()

        arrayOf(
            binding.settingsColorCustomizationLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsRemindersLabel,
            binding.settingsCaldavLabel,
            binding.settingsNewEventsLabel,
            binding.settingsWeeklyViewLabel,
            binding.settingsMonthlyViewLabel,
            binding.settingsEventListsLabel,
            binding.settingsWidgetsLabel,
            binding.settingsEventsLabel,
            binding.settingsTasksLabel,
            binding.settingsMigratingLabel
        ).forEach {
            it.setTextColor(getProperAccentColor())
        }

        arrayOf(
            binding.settingsColorCustomizationHolder,
            binding.settingsGeneralSettingsHolder,
            binding.settingsRemindersHolder,
            binding.settingsCaldavHolder,
            binding.settingsNewEventsHolder,
            binding.settingsWeeklyViewHolder,
            binding.settingsMonthlyViewHolder,
            binding.settingsEventListsHolder,
            binding.settingsWidgetsHolder,
            binding.settingsEventsHolder,
            binding.settingsTasksHolder,
            binding.settingsMigratingHolder
        ).forEach {
            it.background.applyColorFilter(getProperBackgroundColor().getContrastColor())
        }
    }

    override fun onPause() {
        super.onPause()
        mStoredAccentColor = getProperAccentColor()
    }

    override fun onStop() {
        super.onStop()
        val reminders = sortedSetOf(config.defaultReminder1, config.defaultReminder2, config.defaultReminder3).filter { it != REMINDER_OFF }
        config.defaultReminder1 = reminders.getOrElse(0) { REMINDER_OFF }
        config.defaultReminder2 = reminders.getOrElse(1) { REMINDER_OFF }
        config.defaultReminder3 = reminders.getOrElse(2) { REMINDER_OFF }
    }

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
    }

    private fun setupCustomizeColors() {
        binding.settingsCustomizeColorsHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupCustomizeNotifications() {
        binding.settingsCustomizeNotificationsHolder.beVisibleIf(isOreoPlus())

        if (binding.settingsCustomizeNotificationsHolder.isGone()) {
            binding.settingsReminderSoundHolder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        binding.settingsCustomizeNotificationsHolder.setOnClickListener {
            launchCustomizeNotificationsIntent()
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())

        if (binding.settingsUseEnglishHolder.isGone() && binding.settingsLanguageHolder.isGone()) {
            binding.settingsManageEventTypesHolder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        binding.settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageEventTypes() {
        binding.settingsManageEventTypesHolder.setOnClickListener {
            startActivity(Intent(this, ManageEventTypesActivity::class.java))
        }
    }

    private fun setupManageQuickFilterEventTypes() {
        binding.settingsManageQuickFilterEventTypesHolder.setOnClickListener {
            showQuickFilterPicker()
        }

        eventsHelper.getEventTypes(this, false) {
            binding.settingsManageQuickFilterEventTypesHolder.beGoneIf(it.size < 2)
        }
    }

    private fun setupHourFormat() {
        binding.settingsHourFormat.isChecked = config.use24HourFormat
        binding.settingsHourFormatHolder.setOnClickListener {
            binding.settingsHourFormat.toggle()
            config.use24HourFormat = binding.settingsHourFormat.isChecked
        }
    }

    private fun setupAllowCreatingTasks() {
        binding.settingsAllowCreatingTasks.isChecked = config.allowCreatingTasks
        binding.settingsAllowCreatingTasksHolder.setOnClickListener {
            binding.settingsAllowCreatingTasks.toggle()
            config.allowCreatingTasks = binding.settingsAllowCreatingTasks.isChecked
        }
    }

    private fun setupCaldavSync() {
        binding.settingsCaldavSync.isChecked = config.caldavSync
        checkCalDAVBackgrounds()
        binding.settingsCaldavSyncHolder.setOnClickListener {
            if (config.caldavSync) {
                toggleCaldavSync(false)
            } else {
                handlePermission(PERMISSION_WRITE_CALENDAR) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CALENDAR) {
                            if (it) {
                                handleNotificationPermission { granted ->
                                    if (granted) {
                                        toggleCaldavSync(true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupPullToRefresh() {
        binding.settingsCaldavPullToRefreshHolder.beVisibleIf(config.caldavSync)
        binding.settingsCaldavPullToRefresh.isChecked = config.pullToRefresh
        checkCalDAVBackgrounds()
        binding.settingsCaldavPullToRefreshHolder.setOnClickListener {
            binding.settingsCaldavPullToRefresh.toggle()
            config.pullToRefresh = binding.settingsCaldavPullToRefresh.isChecked
        }
    }

    private fun checkCalDAVBackgrounds() {
        if (config.caldavSync) {
            binding.settingsCaldavSyncHolder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
            binding.settingsManageSyncedCalendarsHolder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        } else {
            binding.settingsCaldavSyncHolder.background = resources.getDrawable(R.drawable.ripple_all_corners, theme)
        }
    }

    private fun setupManageSyncedCalendars() {
        binding.settingsManageSyncedCalendarsHolder.beVisibleIf(config.caldavSync)
        binding.settingsManageSyncedCalendarsHolder.setOnClickListener {
            showCalendarPicker()
        }
    }

    private fun toggleCaldavSync(enable: Boolean) {
        if (enable) {
            showCalendarPicker()
        } else {
            binding.settingsCaldavSync.isChecked = false
            config.caldavSync = false
            binding.settingsManageSyncedCalendarsHolder.beGone()
            binding.settingsCaldavPullToRefreshHolder.beGone()

            ensureBackgroundThread {
                config.getSyncedCalendarIdsAsList().forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                }
                eventTypesDB.deleteEventTypesWithCalendarId(config.getSyncedCalendarIdsAsList())
                updateDefaultEventTypeText()
            }
        }
        checkCalDAVBackgrounds()
    }

    private fun showCalendarPicker() {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()

        SelectCalendarsDialog(this) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            if (newCalendarIds.isEmpty() && !config.caldavSync) {
                return@SelectCalendarsDialog
            }

            binding.settingsManageSyncedCalendarsHolder.beVisibleIf(newCalendarIds.isNotEmpty())
            binding.settingsCaldavPullToRefreshHolder.beVisibleIf(newCalendarIds.isNotEmpty())
            binding.settingsCaldavSync.isChecked = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            if (binding.settingsCaldavSync.isChecked) {
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
                            if (binding.settingsCaldavSync.isChecked) {
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
                updateDefaultEventTypeText()
            }
        }
    }

    private fun showQuickFilterPicker() {
        SelectQuickFilterEventTypesDialog(this)
    }

    private fun setupSundayFirst() {
        binding.settingsSundayFirst.isChecked = config.isSundayFirst
        binding.settingsSundayFirstHolder.setOnClickListener {
            binding.settingsSundayFirst.toggle()
            config.isSundayFirst = binding.settingsSundayFirst.isChecked
        }
    }

    private fun setupHighlightWeekends() {
        binding.settingsHighlightWeekends.isChecked = config.highlightWeekends
        binding.settingsHighlightWeekendsColorHolder.beVisibleIf(config.highlightWeekends)
        setupHighlightWeekendColorBackground()
        binding.settingsHighlightWeekendsHolder.setOnClickListener {
            binding.settingsHighlightWeekends.toggle()
            config.highlightWeekends = binding.settingsHighlightWeekends.isChecked
            binding.settingsHighlightWeekendsColorHolder.beVisibleIf(config.highlightWeekends)
            setupHighlightWeekendColorBackground()
        }
    }

    private fun setupShowWhatsNew() {
        binding.settingsShowWhatsNew.isChecked = config.showWhatsNewDialog
        binding.settingsShowWhatsNewHolder.background = resources.getDrawable(R.drawable.ripple_background,theme)
        binding.settingsShowWhatsNewHolder.setOnClickListener {
            binding.settingsShowWhatsNew.toggle()
            config.showWhatsNewDialog = binding.settingsShowWhatsNew.isChecked
        }
    }

    private fun setupHighlightWeekendsColor() {
        binding.settingsHighlightWeekendsColor.setFillWithStroke(config.highlightWeekendsColor, getProperBackgroundColor())
        binding.settingsHighlightWeekendsColorHolder.setOnClickListener {
            ColorPickerDialog(this, config.highlightWeekendsColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    config.highlightWeekendsColor = color
                    binding.settingsHighlightWeekendsColor.setFillWithStroke(color, getProperBackgroundColor())
                }
            }
        }
    }

    private fun setupHighlightWeekendColorBackground() {
        if (binding.settingsHighlightWeekendsColorHolder.isVisible()) {
            binding.settingsHighlightWeekendsHolder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            binding.settingsHighlightWeekendsHolder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupDeleteAllEvents() {
        binding.settingsDeleteAllEventsHolder.setOnClickListener {
            ConfirmationDialog(this, messageId = R.string.delete_all_events_confirmation) {
                eventsHelper.deleteAllEvents()
            }
        }
    }

    private fun setupDisplayDescription() {
        binding.settingsDisplayDescription.isChecked = config.displayDescription
        setupDescriptionVisibility()
        binding.settingsDisplayDescriptionHolder.setOnClickListener {
            binding.settingsDisplayDescription.toggle()
            config.displayDescription = binding.settingsDisplayDescription.isChecked
            setupDescriptionVisibility()
        }
    }

    private fun setupDescriptionVisibility() {
        binding.settingsReplaceDescriptionHolder.beVisibleIf(config.displayDescription)
        if (binding.settingsReplaceDescriptionHolder.isVisible()) {
            binding.settingsDisplayDescriptionHolder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            binding.settingsDisplayDescriptionHolder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupReplaceDescription() {
        binding.settingsReplaceDescription.isChecked = config.replaceDescription
        binding.settingsReplaceDescriptionHolder.setOnClickListener {
            binding.settingsReplaceDescription.toggle()
            config.replaceDescription = binding.settingsReplaceDescription.isChecked
        }
    }

    private fun setupBirthAnnLabel() {
        binding.settingsShowBirthAnnDescription.isChecked = config.showBirthdayAnniversaryDescription
        binding.settingsShowBirthAnnDescriptionHolder.setOnClickListener {
            binding.settingsShowBirthAnnDescription.toggle()
            config.showBirthdayAnniversaryDescription = binding.settingsShowBirthAnnDescription.isChecked
        }
    }

    private fun setupWeeklyStart() {
        binding.settingsStartWeeklyAt.text = getHoursString(config.startWeeklyAt)
        binding.settingsStartWeeklyAtHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..16).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.startWeeklyAt) {
                config.startWeeklyAt = it as Int
                binding.settingsStartWeeklyAt.text = getHoursString(it)
            }
        }
    }

    private fun setupMidnightSpanEvents() {
        binding.settingsMidnightSpanEvent.isChecked = config.showMidnightSpanningEventsAtTop
        binding.settingsMidnightSpanEventsHolder.setOnClickListener {
            binding.settingsMidnightSpanEvent.toggle()
            config.showMidnightSpanningEventsAtTop = binding.settingsMidnightSpanEvent.isChecked
        }
    }

    private fun setupAllowCustomizeDayCount() {
        binding.settingsAllowCustomizeDayCount.isChecked = config.allowCustomizeDayCount
        binding.settingsAllowCustomizeDayCountHolder.setOnClickListener {
            binding.settingsAllowCustomizeDayCount.toggle()
            config.allowCustomizeDayCount = binding.settingsAllowCustomizeDayCount.isChecked
        }
    }

    private fun setupStartWeekWithCurrentDay() {
        binding.settingsStartWeekWithCurrentDay.isChecked = config.startWeekWithCurrentDay
        binding.settingsStartWeekWithCurrentDayHolder.setOnClickListener {
            binding.settingsStartWeekWithCurrentDay.toggle()
            config.startWeekWithCurrentDay = binding.settingsStartWeekWithCurrentDay.isChecked
        }
    }

    private fun setupWeekNumbers() {
        binding.settingsWeekNumbers.isChecked = config.showWeekNumbers
        binding.settingsWeekNumbersHolder.setOnClickListener {
            binding.settingsWeekNumbers.toggle()
            config.showWeekNumbers = binding.settingsWeekNumbers.isChecked
        }
    }

    private fun setupShowGrid() {
        binding.settingsShowGrid.isChecked = config.showGrid
        binding.settingsShowGridHolder.setOnClickListener {
            binding.settingsShowGrid.toggle()
            config.showGrid = binding.settingsShowGrid.isChecked
        }
    }

    private fun setupReminderSound() {
        binding.settingsReminderSoundHolder.beGoneIf(isOreoPlus())
        binding.settingsReminderSound.text = config.reminderSoundTitle

        binding.settingsReminderSoundHolder.setOnClickListener {
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
    }

    private fun updateReminderSound(alarmSound: AlarmSound) {
        config.reminderSoundTitle = alarmSound.title
        config.reminderSoundUri = alarmSound.uri
        binding.settingsReminderSound.text = alarmSound.title
    }

    private fun setupReminderAudioStream() {
        binding.settingsReminderAudioStream.text = getAudioStreamText()
        binding.settingsReminderAudioStreamHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_stream)),
                RadioItem(AudioManager.STREAM_SYSTEM, getString(R.string.system_stream)),
                RadioItem(AudioManager.STREAM_NOTIFICATION, getString(R.string.notification_stream)),
                RadioItem(AudioManager.STREAM_RING, getString(R.string.ring_stream))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.reminderAudioStream) {
                config.reminderAudioStream = it as Int
                binding.settingsReminderAudioStream.text = getAudioStreamText()
            }
        }
    }

    private fun getAudioStreamText() = getString(
        when (config.reminderAudioStream) {
            AudioManager.STREAM_ALARM -> R.string.alarm_stream
            AudioManager.STREAM_SYSTEM -> R.string.system_stream
            AudioManager.STREAM_NOTIFICATION -> R.string.notification_stream
            else -> R.string.ring_stream
        }
    )

    private fun setupVibrate() {
        binding.settingsVibrate.isChecked = config.vibrateOnReminder
        binding.settingsVibrateHolder.setOnClickListener {
            binding.settingsVibrate.toggle()
            config.vibrateOnReminder = binding.settingsVibrate.isChecked
        }
    }

    private fun setupLoopReminders() {
        binding.settingsLoopReminders.isChecked = config.loopReminders
        binding.settingsLoopRemindersHolder.setOnClickListener {
            binding.settingsLoopReminders.toggle()
            config.loopReminders = binding.settingsLoopReminders.isChecked
        }
    }

    private fun setupUseSameSnooze() {
        binding.settingsSnoozeTime.beVisibleIf(config.useSameSnooze)
        binding.settingsUseSameSnooze.isChecked = config.useSameSnooze
        setupSnoozeBackgrounds()
        binding.settingsUseSameSnoozeHolder.setOnClickListener {
            binding.settingsUseSameSnooze.toggle()
            config.useSameSnooze = binding.settingsUseSameSnooze.isChecked
            binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
            setupSnoozeBackgrounds()
        }
    }

    private fun setupSnoozeBackgrounds() {
        if (config.useSameSnooze) {
            binding.settingsUseSameSnoozeHolder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            binding.settingsUseSameSnoozeHolder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeTime()
        binding.settingsSnoozeTimeHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.snoozeTime, true) {
                config.snoozeTime = it / 60
                updateSnoozeTime()
            }
        }
    }

    private fun updateSnoozeTime() {
        binding.settingsSnoozeTime.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun setupDefaultReminder() {
        binding.settingsUseLastEventReminders.isChecked = config.usePreviousEventReminders
        toggleDefaultRemindersVisibility(!config.usePreviousEventReminders)
        binding.settingsUseLastEventRemindersHolder.setOnClickListener {
            binding.settingsUseLastEventReminders.toggle()
            config.usePreviousEventReminders = binding.settingsUseLastEventReminders.isChecked
            toggleDefaultRemindersVisibility(!binding.settingsUseLastEventReminders.isChecked)
        }
    }

    private fun setupDefaultReminder1() {
        binding.settingsDefaultReminder1.text = getFormattedMinutes(config.defaultReminder1)
        binding.settingsDefaultReminder1Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder1) {
                config.defaultReminder1 = if (it == -1 || it == 0) it else it / 60
                binding.settingsDefaultReminder1.text = getFormattedMinutes(config.defaultReminder1)
            }
        }
    }

    private fun setupDefaultReminder2() {
        binding.settingsDefaultReminder2.text = getFormattedMinutes(config.defaultReminder2)
        binding.settingsDefaultReminder2Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder2) {
                config.defaultReminder2 = if (it == -1 || it == 0) it else it / 60
                binding.settingsDefaultReminder2.text = getFormattedMinutes(config.defaultReminder2)
            }
        }
    }

    private fun setupDefaultReminder3() {
        binding.settingsDefaultReminder3.text = getFormattedMinutes(config.defaultReminder3)
        binding.settingsDefaultReminder3Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder3) {
                config.defaultReminder3 = if (it == -1 || it == 0) it else it / 60
                binding.settingsDefaultReminder3.text = getFormattedMinutes(config.defaultReminder3)
            }
        }
    }

    private fun toggleDefaultRemindersVisibility(show: Boolean) {
        arrayOf(binding.settingsDefaultReminder1Holder, binding.settingsDefaultReminder2Holder,
            binding.settingsDefaultReminder3Holder).forEach {
            it.beVisibleIf(show)
        }

        if (show) {
            binding.settingsUseLastEventRemindersHolder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            binding.settingsUseLastEventRemindersHolder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun getHoursString(hours: Int) = String.format("%02d:00", hours)

    private fun setupDisplayPastEvents() {
        var displayPastEvents = config.displayPastEvents
        updatePastEventsText(displayPastEvents)
        binding.settingsDisplayPastEventsHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, displayPastEvents * 60) {
                val result = it / 60
                displayPastEvents = result
                config.displayPastEvents = result
                updatePastEventsText(result)
            }
        }
    }

    private fun updatePastEventsText(displayPastEvents: Int) {
        binding.settingsDisplayPastEvents.text = getDisplayPastEventsText(displayPastEvents)
    }

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0) {
            getString(R.string.never)
        } else {
            getFormattedMinutes(displayPastEvents, false)
        }
    }

    private fun setupFontSize() {
        binding.settingsFontSize.text = getFontSizeText()
        binding.settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_EXTRA_SMALL, getString(R.string.extra_small)),
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                binding.settingsFontSize.text = getFontSizeText()
                updateWidgets()
            }
        }
    }

    private fun setupShowWidgetDescription() {
        binding.settingsShowWidgetDescription.isChecked = config.showWidgetDescription
        binding.settingsShowWidgetDescriptionHolder.setOnClickListener {
            binding.settingsShowWidgetDescription.toggle()
            config.showWidgetDescription = binding.settingsShowWidgetDescription.isChecked
        }
    }

    private fun setupCustomizeWidgetColors() {
        binding.settingsCustomizeWidgetColorsHolder.setOnClickListener {
            Intent(this, WidgetListConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupViewToOpenFromListWidget() {
        binding.settingsListWidgetViewToOpen.text = getDefaultViewText()
        binding.settingsListWidgetViewToOpenHolder.setOnClickListener {
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
                binding.settingsListWidgetViewToOpen.text = getDefaultViewText()
                updateWidgets()
            }
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

    private fun setupDimEvents() {
        binding.settingsDimPastEvents.isChecked = config.dimPastEvents
        binding.settingsDimPastEventsHolder.setOnClickListener {
            binding.settingsDimPastEvents.toggle()
            config.dimPastEvents = binding.settingsDimPastEvents.isChecked
        }
    }

    private fun setupSyncBirthdays() {
        binding.settingsSyncContactBirthdays.isChecked = config.addBirthdaysAutomatically
        binding.settingsSyncContactBirthdaysHolder.setOnClickListener {
            binding.settingsSyncContactBirthdays.toggle()
            config.addBirthdaysAutomatically = binding.settingsSyncContactBirthdays.isChecked
        }
    }

    private fun setupSyncAnniversaries() {
        binding.settingsSyncContactAnniversaries.isChecked = config.addAnniversariesAutomatically
        binding.settingsSyncContactAnniversariesHolder.setOnClickListener {
            binding.settingsSyncContactAnniversaries.toggle()
            config.addAnniversariesAutomatically = binding.settingsSyncContactAnniversaries.isChecked
        }
    }

    private fun setupDimCompletedTasks() {
        binding.settingsDimCompletedTasks.isChecked = config.dimCompletedTasks
        binding.settingsDimCompletedTasksHolder.setOnClickListener {
            binding.settingsDimCompletedTasks.toggle()
            config.dimCompletedTasks = binding.settingsDimCompletedTasks.isChecked
        }
    }

    private fun setupDefaultTaskType() {
        updateDefaultTaskTypeText()
        //binding.settingsdefault_task_type.text = getString(R.string.regular_event)
        binding.settingsDefaultTaskTypeHolder.setOnClickListener {
            SelectEventTypeDialog(this, config.defaultTaskTypeId, true, false, true, true) {
                config.defaultTaskTypeId = it.id!!
                updateDefaultTaskTypeText()
            }
        }
    }

    private fun updateDefaultTaskTypeText() {
        if (config.defaultTaskTypeId == -1L) {
            runOnUiThread {
                binding.settingsDefaultTaskType.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(config.defaultTaskTypeId)
                if (eventType != null) {
                    runOnUiThread {
                        binding.settingsDefaultTaskType.text = eventType.title
                    }
                } else {
                    config.defaultTaskTypeId = -1
                    updateDefaultTaskTypeText()
                }
            }
        }
    }

    private fun setupAllowChangingTimeZones() {
        binding.settingsAllowChangingTimeZones.isChecked = config.allowChangingTimeZones
        binding.settingsAllowChangingTimeZonesHolder.setOnClickListener {
            binding.settingsAllowChangingTimeZones.toggle()
            config.allowChangingTimeZones = binding.settingsAllowChangingTimeZones.isChecked
        }
    }

    private fun setupDefaultStartTime() {
        updateDefaultStartTimeText()
        binding.settingsDefaultStartTimeHolder.setOnClickListener {
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
                    updateDefaultStartTimeText()
                } else {
                    val timeListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                        config.defaultStartTime = hourOfDay * 60 + minute
                        updateDefaultStartTimeText()
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
    }

    private fun updateDefaultStartTimeText() {
        when (config.defaultStartTime) {
            DEFAULT_START_TIME_CURRENT_TIME -> binding.settingsDefaultStartTime.text = getString(R.string.current_time)
            DEFAULT_START_TIME_NEXT_FULL_HOUR -> binding.settingsDefaultStartTime.text = getString(R.string.next_full_hour)
            else -> {
                val hours = config.defaultStartTime / 60
                val minutes = config.defaultStartTime % 60
                binding.settingsDefaultStartTime.text = String.format("%02d:%02d", hours, minutes)
            }
        }
    }

    private fun setupDefaultDuration() {
        updateDefaultDurationText()
        binding.settingsDefaultDurationHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, config.defaultDuration * 60) {
                val result = it / 60
                config.defaultDuration = result
                updateDefaultDurationText()
            }
        }
    }

    private fun updateDefaultDurationText() {
        val duration = config.defaultDuration
        binding.settingsDefaultDuration.text = if (duration == 0) {
            "0 ${getString(R.string.minutes_raw)}"
        } else {
            getFormattedMinutes(duration, false)
        }
    }

    private fun setupDefaultEventType() {
        updateDefaultEventTypeText()
        //binding.settingsdefault_event_type.text = getString(R.string.last_used_one)
        binding.settingsDefaultEventTypeHolder.setOnClickListener {
            SelectEventTypeDialog(this, config.defaultEventTypeId, true, false, true, true) {
                config.defaultEventTypeId = it.id!!
                updateDefaultEventTypeText()
            }
        }
    }

    private fun updateDefaultEventTypeText() {
        if (config.defaultEventTypeId == -1L) {
            runOnUiThread {
                binding.settingsDefaultEventType.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(config.defaultEventTypeId)
                if (eventType != null) {
                    config.lastUsedCaldavCalendarId = eventType.caldavCalendarId
                    runOnUiThread {
                        binding.settingsDefaultEventType.text = eventType.title
                    }
                } else {
                    config.defaultEventTypeId = -1
                    updateDefaultEventTypeText()
                }
            }
        }
    }

    private fun setupExportSettings() {
        binding.settingsExportHolder.setOnClickListener {
            val configItems = LinkedHashMap<String, Any>().apply {
                put(IS_USING_SHARED_THEME, config.isUsingSharedTheme)
                put(TEXT_COLOR, config.textColor)
                put(BACKGROUND_COLOR, config.backgroundColor)
                put(PRIMARY_COLOR, config.primaryColor)
                put(ACCENT_COLOR, config.accentColor)
                put(USE_ENGLISH, config.useEnglish)
                put(WAS_USE_ENGLISH_TOGGLED, config.wasUseEnglishToggled)
                put(WIDGET_BG_COLOR, config.widgetBgColor)
                put(WIDGET_TEXT_COLOR, config.widgetTextColor)
                put(WEEK_NUMBERS, config.showWeekNumbers)
                put(START_WEEKLY_AT, config.startWeeklyAt)
                put(VIBRATE, config.vibrateOnReminder)
                put(LAST_EVENT_REMINDER_MINUTES, config.lastEventReminderMinutes1)
                put(LAST_EVENT_REMINDER_MINUTES_2, config.lastEventReminderMinutes2)
                put(LAST_EVENT_REMINDER_MINUTES_3, config.lastEventReminderMinutes3)
                put(DISPLAY_PAST_EVENTS, config.displayPastEvents)
                put(FONT_SIZE, config.fontSize)
                put(LIST_WIDGET_VIEW_TO_OPEN, config.listWidgetViewToOpen)
                put(REMINDER_AUDIO_STREAM, config.reminderAudioStream)
                put(DISPLAY_DESCRIPTION, config.displayDescription)
                put(REPLACE_DESCRIPTION, config.replaceDescription)
                put(SHOW_GRID, config.showGrid)
                put(LOOP_REMINDERS, config.loopReminders)
                put(DIM_PAST_EVENTS, config.dimPastEvents)
                put(DIM_COMPLETED_TASKS, config.dimCompletedTasks)
                put(ADD_BIRTHDAYS_AUTOMATICALLY, config.addBirthdaysAutomatically)
                put(ADD_ANNIVERSARIES_AUTOMATICALLY, config.addAnniversariesAutomatically)
                put(ALLOW_CHANGING_TIME_ZONES, config.allowChangingTimeZones)
                put(USE_PREVIOUS_EVENT_REMINDERS, config.usePreviousEventReminders)
                put(DEFAULT_REMINDER_1, config.defaultReminder1)
                put(DEFAULT_REMINDER_2, config.defaultReminder2)
                put(DEFAULT_REMINDER_3, config.defaultReminder3)
                put(PULL_TO_REFRESH, config.pullToRefresh)
                put(DEFAULT_START_TIME, config.defaultStartTime)
                put(DEFAULT_DURATION, config.defaultDuration)
                put(USE_SAME_SNOOZE, config.useSameSnooze)
                put(SNOOZE_TIME, config.snoozeTime)
                put(USE_24_HOUR_FORMAT, config.use24HourFormat)
                put(SUNDAY_FIRST, config.isSundayFirst)
                put(HIGHLIGHT_WEEKENDS, config.highlightWeekends)
                put(HIGHLIGHT_WEEKENDS_COLOR, config.highlightWeekendsColor)
                put(ALLOW_CREATING_TASKS, config.allowCreatingTasks)
            }

            exportSettings(configItems)
        }
    }

    private fun setupImportSettings() {
        binding.settingsImportHolder.setOnClickListener {
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
                IS_USING_SHARED_THEME -> config.isUsingSharedTheme = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                WEEK_NUMBERS -> config.showWeekNumbers = value.toBoolean()
                START_WEEKLY_AT -> config.startWeeklyAt = value.toInt()
                VIBRATE -> config.vibrateOnReminder = value.toBoolean()
                LAST_EVENT_REMINDER_MINUTES -> config.lastEventReminderMinutes1 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_2 -> config.lastEventReminderMinutes2 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_3 -> config.lastEventReminderMinutes3 = value.toInt()
                DISPLAY_PAST_EVENTS -> config.displayPastEvents = value.toInt()
                FONT_SIZE -> config.fontSize = value.toInt()
                LIST_WIDGET_VIEW_TO_OPEN -> config.listWidgetViewToOpen = value.toInt()
                REMINDER_AUDIO_STREAM -> config.reminderAudioStream = value.toInt()
                DISPLAY_DESCRIPTION -> config.displayDescription = value.toBoolean()
                REPLACE_DESCRIPTION -> config.replaceDescription = value.toBoolean()
                SHOW_GRID -> config.showGrid = value.toBoolean()
                LOOP_REMINDERS -> config.loopReminders = value.toBoolean()
                DIM_PAST_EVENTS -> config.dimPastEvents = value.toBoolean()
                DIM_COMPLETED_TASKS -> config.dimCompletedTasks = value.toBoolean()
                ADD_BIRTHDAYS_AUTOMATICALLY -> config.addBirthdaysAutomatically = value.toBoolean()
                ADD_ANNIVERSARIES_AUTOMATICALLY -> config.addAnniversariesAutomatically = value.toBoolean()
                ALLOW_CHANGING_TIME_ZONES -> config.allowChangingTimeZones = value.toBoolean()
                USE_PREVIOUS_EVENT_REMINDERS -> config.usePreviousEventReminders = value.toBoolean()
                DEFAULT_REMINDER_1 -> config.defaultReminder1 = value.toInt()
                DEFAULT_REMINDER_2 -> config.defaultReminder2 = value.toInt()
                DEFAULT_REMINDER_3 -> config.defaultReminder3 = value.toInt()
                PULL_TO_REFRESH -> config.pullToRefresh = value.toBoolean()
                DEFAULT_START_TIME -> config.defaultStartTime = value.toInt()
                DEFAULT_DURATION -> config.defaultDuration = value.toInt()
                USE_SAME_SNOOZE -> config.useSameSnooze = value.toBoolean()
                SNOOZE_TIME -> config.snoozeTime = value.toInt()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                SUNDAY_FIRST -> config.isSundayFirst = value.toBoolean()
                HIGHLIGHT_WEEKENDS -> config.highlightWeekends = value.toBoolean()
                HIGHLIGHT_WEEKENDS_COLOR -> config.highlightWeekendsColor = value.toInt()
                ALLOW_CREATING_TASKS -> config.allowCreatingTasks = value.toBoolean()
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
