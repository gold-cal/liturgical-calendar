package com.liturgical.calendar.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogSelectRadioGroupBinding
import com.liturgical.calendar.databinding.RadioButtonWithColorBinding
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.models.EventType
import com.secure.commons.extensions.*

class SelectEventTypeDialog(
    val activity: Activity, val currEventType: Long, val showCalDAVCalendars: Boolean, val showNewEventTypeOption: Boolean,
    val addLastUsedOneAsFirstOption: Boolean, val showOnlyWritable: Boolean, val callback: (eventType: EventType) -> Unit
) {
    private val NEW_EVENT_TYPE_ID = -2L
    private val LAST_USED_EVENT_TYPE_ID = -1L

    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private var eventTypes = ArrayList<EventType>()

    init {
        val binding = DialogSelectRadioGroupBinding.inflate(activity.layoutInflater)
        radioGroup = binding.dialogRadioGroup

        activity.eventsHelper.getEventTypes(activity, showOnlyWritable) {
            eventTypes = it
            activity.runOnUiThread {
                if (addLastUsedOneAsFirstOption) {
                    val lastUsedEventType = EventType(LAST_USED_EVENT_TYPE_ID, activity.getString(R.string.last_used_one), Color.TRANSPARENT, 0)
                    addRadioButton(lastUsedEventType)
                }
                eventTypes.filter { showCalDAVCalendars || it.caldavCalendarId == 0 }.forEach {
                    addRadioButton(it)
                }
                if (showNewEventTypeOption) {
                    val newEventType = EventType(NEW_EVENT_TYPE_ID, activity.getString(R.string.add_new_type), Color.TRANSPARENT, 0)
                    addRadioButton(newEventType)
                }
                wasInit = true
                activity.updateTextColors(binding.dialogRadioHolder)
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun addRadioButton(eventType: EventType) {
        val colorBinding = RadioButtonWithColorBinding.inflate(activity.layoutInflater)
        colorBinding.dialogRadioButton.apply {
            text = eventType.getDisplayTitle()
            isChecked = eventType.id == currEventType
            id = eventType.id!!.toInt()
        }

        if (eventType.color != Color.TRANSPARENT) {
            colorBinding.dialogRadioColor.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
        }

        colorBinding.root.setOnClickListener { viewClicked(eventType) }
        radioGroup.addView(colorBinding.root, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(eventType: EventType) {
        if (!wasInit) {
            return
        }

        if (eventType.id == NEW_EVENT_TYPE_ID) {
            EditEventTypeDialog(activity) {
                callback(it)
                activity.hideKeyboard()
                dialog?.dismiss()
            }
        } else {
            callback(eventType)
            dialog?.dismiss()
        }
    }
}
