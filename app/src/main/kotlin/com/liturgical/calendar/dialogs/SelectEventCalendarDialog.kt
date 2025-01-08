package com.liturgical.calendar.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogSelectRadioGroupBinding
import com.liturgical.calendar.databinding.RadioButtonWithColorBinding
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.helpers.STORED_LOCALLY_ONLY
import com.liturgical.calendar.models.CalDAVCalendar
import com.secure.commons.extensions.*
import com.secure.commons.helpers.ensureBackgroundThread

class SelectEventCalendarDialog(val activity: Activity, val calendars: List<CalDAVCalendar>, val currCalendarId: Int, val callback: (id: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false

    init {
        val binding = DialogSelectRadioGroupBinding.inflate(activity.layoutInflater)
        radioGroup = binding.dialogRadioGroup

        ensureBackgroundThread {
            calendars.forEach {
                val localEventType = activity.eventsHelper.getEventTypeWithCalDAVCalendarId(it.id)
                if (localEventType != null) {
                    it.color = localEventType.color
                }
            }

            activity.runOnUiThread {
                calendars.forEach {
                    addRadioButton(it.getFullTitle(), it.id, it.color)
                }
                addRadioButton(activity.getString(R.string.store_locally_only), STORED_LOCALLY_ONLY, Color.TRANSPARENT)
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

    private fun addRadioButton(title: String, typeId: Int, color: Int) {
        val colorBinding = RadioButtonWithColorBinding.inflate(activity.layoutInflater)
        (colorBinding.dialogRadioButton as RadioButton).apply {
            text = title
            isChecked = typeId == currCalendarId
            id = typeId
        }

        if (typeId != STORED_LOCALLY_ONLY) {
            colorBinding.dialogRadioColor.setFillWithStroke(color, activity.getProperBackgroundColor())
        }

        colorBinding.root.setOnClickListener { viewClicked(typeId) }
        radioGroup.addView(colorBinding.root, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(typeId: Int) {
        if (wasInit) {
            callback(typeId)
            dialog?.dismiss()
        }
    }
}
