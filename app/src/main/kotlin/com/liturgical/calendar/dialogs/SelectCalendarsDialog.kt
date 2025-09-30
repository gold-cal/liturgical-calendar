package com.liturgical.calendar.dialogs

import android.text.TextUtils
import android.widget.RelativeLayout
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.CalendarItemAccountBinding
import com.liturgical.calendar.databinding.CalendarItemCalendarBinding
import com.liturgical.calendar.databinding.DialogSelectCalendarsBinding
import com.liturgical.calendar.extensions.calDAVHelper
import com.liturgical.calendar.extensions.config
import com.secure.commons.extensions.beVisibleIf
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.setupDialogStuff
import com.secure.commons.views.MyAppCompatCheckbox

class SelectCalendarsDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var prevAccount = ""
    private var binding = DialogSelectCalendarsBinding.inflate(activity.layoutInflater, null, false)

    init {
        val ids = activity.config.getSyncedCalendarIdsAsList()
        val calendars = activity.calDAVHelper.getCalDAVCalendars("", true)
        binding.apply {
            dialogSelectCalendarsPlaceholder.beVisibleIf(calendars.isEmpty())
            dialogSelectCalendarsHolder.beVisibleIf(calendars.isNotEmpty())
        }

        val sorted = calendars.sortedWith(compareBy({ it.accountName }, { it.displayName }))
        sorted.forEach {
            if (prevAccount != it.accountName) {
                prevAccount = it.accountName
                addCalendarItem(false, it.accountName)
            }

            addCalendarItem(true, it.displayName, it.id, ids.contains(it.id))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, _ -> confirmSelection() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.select_caldav_calendars)
            }
    }

    private fun addCalendarItem(isEvent: Boolean, text: String, tag: Int = 0, shouldCheck: Boolean = false) {
        val itemBinding = if (isEvent) {
            CalendarItemCalendarBinding.inflate(activity.layoutInflater, binding.dialogSelectCalendarsHolder, false).apply {
                calendarItemCalendarSwitch.tag = tag
                calendarItemCalendarSwitch.text = text
                calendarItemCalendarSwitch.isChecked = shouldCheck
                root.setOnClickListener {
                    calendarItemCalendarSwitch.toggle()
                }
            }
        } else {
            CalendarItemAccountBinding.inflate(activity.layoutInflater, binding.dialogSelectCalendarsHolder, false).apply {
                calendarItemAccount.text = text
            }
        }

        binding.dialogSelectCalendarsHolder.addView(itemBinding.root)
        /*if (isEvent) {
            addCalendar(tag, shouldCheck)
        } else {
            addAccount(text)
        }*/
    }

    /*private fun addCalendar(tag: Int, shouldCheck: Boolean) {
        val calendarItem = CalendarItemCalendarBinding.inflate(activity.layoutInflater, null, false)
        calendarItem.calendarItemCalendarSwitch.apply {
            this.tag = tag
            this.text = text
            isChecked = shouldCheck
            calendarItem.root.setOnClickListener {
                toggle()
            }
        }
        binding.dialogSelectCalendarsHolder.addView(calendarItem.root)
    }

    private fun addAccount(text: String) {
        val calendarItem = CalendarItemAccountBinding.inflate(activity.layoutInflater, null, false)
        calendarItem.calendarItemAccount.text = text
        binding.dialogSelectCalendarsHolder.addView(calendarItem.root)
    }*/

    private fun confirmSelection() {
        val calendarIds = ArrayList<Int>()
        val childCnt = binding.dialogSelectCalendarsHolder.childCount
        for (i in 0..childCnt) {
            val child = binding.dialogSelectCalendarsHolder.getChildAt(i)
            if (child is RelativeLayout) {
                val check = child.getChildAt(0)
                if (check is MyAppCompatCheckbox && check.isChecked) {
                    calendarIds.add(check.tag as Int)
                }
            }
        }

        activity.config.caldavSyncedCalendarIds = TextUtils.join(",", calendarIds)
        callback()
    }
}
