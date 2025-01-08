package com.liturgical.calendar.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogSelectEventTypeColorBinding
import com.liturgical.calendar.databinding.RadioButtonWithColorBinding
import com.liturgical.calendar.extensions.calDAVHelper
import com.liturgical.calendar.models.EventType
import com.secure.commons.dialogs.ColorPickerDialog
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.getProperBackgroundColor
import com.secure.commons.extensions.setFillWithStroke
import com.secure.commons.extensions.setupDialogStuff

class SelectEventTypeColorDialog(val activity: Activity, val eventType: EventType, val callback: (color: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private val colors = activity.calDAVHelper.getAvailableCalDAVCalendarColors(eventType)

    init {
        val binding = DialogSelectEventTypeColorBinding.inflate(activity.layoutInflater)
        radioGroup = binding.dialogSelectEventTypeColorRadio
        binding.dialogSelectEventTypeOtherValue.setOnClickListener {
            showCustomColorPicker()
        }

        colors.forEachIndexed { index, value ->
            addRadioButton(index, value)
        }

        wasInit = true
        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }

                if (colors.isEmpty()) {
                    showCustomColorPicker()
                }
            }
    }

    private fun addRadioButton(colorKey: Int, color: Int) {
        val colorBinding = RadioButtonWithColorBinding.inflate(activity.layoutInflater)
        (colorBinding.dialogRadioButton as RadioButton).apply {
            text = if (color == 0) activity.getString(R.string.transparent) else String.format("#%06X", 0xFFFFFF and color)
            isChecked = color == eventType.color
            id = colorKey
        }

        colorBinding.dialogRadioColor.setFillWithStroke(color, activity.getProperBackgroundColor())
        colorBinding.root.setOnClickListener {
            viewClicked(colorKey)
        }
        radioGroup.addView(colorBinding.root, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(colorKey: Int) {
        if (!wasInit)
            return

        callback(colors[colorKey])
        dialog?.dismiss()
    }

    private fun showCustomColorPicker() {
        ColorPickerDialog(activity, eventType.color) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                callback(color)
            }
            dialog?.dismiss()
        }
    }
}
