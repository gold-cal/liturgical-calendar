package com.liturgical.calendar.dialogs

import android.view.View
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.helpers.ANNIVERSARY_EVENT
import com.liturgical.calendar.helpers.BIRTHDAY_EVENT
import com.liturgical.calendar.helpers.OTHER_EVENT
import com.liturgical.calendar.helpers.REMINDER_OFF
import com.secure.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_set_reminders.view.*

class SetRemindersDialog(val activity: SimpleActivity, val eventType: Int, val callback: (reminders: ArrayList<Int>) -> Unit) {
    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var isAutomatic = false

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_set_reminders, null).apply {
            set_reminders_image.applyColorFilter(context.getProperTextColor())
            set_reminders_1.text = activity.getFormattedMinutes(mReminder1Minutes)
            set_reminders_2.text = activity.getFormattedMinutes(mReminder1Minutes)
            set_reminders_3.text = activity.getFormattedMinutes(mReminder1Minutes)

            set_reminders_1.setOnClickListener {
                activity.handleNotificationPermission { granted ->
                    if (granted) {
                        activity.showPickSecondsDialogHelper(mReminder1Minutes, showDuringDayOption = true) {
                            mReminder1Minutes = if (it == -1 || it == 0) it else it / 60
                            set_reminders_1.text = activity.getFormattedMinutes(mReminder1Minutes)
                            if (mReminder1Minutes != REMINDER_OFF) {
                                set_reminders_2.beVisible()
                            }
                        }
                    } else {
                        activity.toast(R.string.no_post_notifications_permissions)
                    }
                }
            }

            set_reminders_2.setOnClickListener {
                activity.showPickSecondsDialogHelper(mReminder2Minutes, showDuringDayOption = true) {
                    mReminder2Minutes = if (it == -1 || it == 0) it else it / 60
                    set_reminders_2.text = activity.getFormattedMinutes(mReminder2Minutes)
                    if (mReminder2Minutes != REMINDER_OFF) {
                        set_reminders_3.beVisible()
                    }
                }
            }

            set_reminders_3.setOnClickListener {
                activity.showPickSecondsDialogHelper(mReminder3Minutes, showDuringDayOption = true) {
                    mReminder3Minutes = if (it == -1 || it == 0) it else it / 60
                    set_reminders_3.text = activity.getFormattedMinutes(mReminder3Minutes)
                }
            }

            add_event_automatically_checkbox.apply {
                visibility = if (eventType == OTHER_EVENT) View.GONE else View.VISIBLE
                text = when (eventType) {
                    BIRTHDAY_EVENT -> activity.getString(R.string.add_birthdays_automatically)
                    ANNIVERSARY_EVENT -> activity.getString(R.string.add_anniversaries_automatically)
                    else -> ""
                }
                isChecked = when (eventType) {
                    BIRTHDAY_EVENT -> activity.config.addBirthdaysAutomatically
                    ANNIVERSARY_EVENT -> activity.config.addAnniversariesAutomatically
                    else -> false
                }
                isAutomatic = isChecked
                setOnCheckedChangeListener { _, isChecked -> isAutomatic = isChecked }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.event_reminders)
            }
    }

    private fun dialogConfirmed() {
        val tempReminders = arrayListOf(mReminder1Minutes, mReminder2Minutes, mReminder3Minutes).filter { it != REMINDER_OFF }.sorted()
        val reminders = arrayListOf(
            tempReminders.getOrNull(0) ?: REMINDER_OFF,
            tempReminders.getOrNull(1) ?: REMINDER_OFF,
            tempReminders.getOrNull(2) ?: REMINDER_OFF
        )

        if (eventType == BIRTHDAY_EVENT) {
            activity.config.addBirthdaysAutomatically = isAutomatic
            activity.config.birthdayReminders = reminders
        }

        if (eventType == ANNIVERSARY_EVENT) {
            activity.config.addAnniversariesAutomatically = isAutomatic
            activity.config.anniversaryReminders = reminders
        }

        callback(reminders)
    }
}
