package com.liturgical.calendar.dialogs

import android.app.Activity
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogEventTypeBinding
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.helpers.OTHER_EVENT
import com.liturgical.calendar.models.EventType
import com.secure.commons.dialogs.ColorPickerDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.ensureBackgroundThread

class EditEventTypeDialog(val activity: Activity, var eventType: EventType? = null, val callback: (eventType: EventType) -> Unit) {
    private var isNewEvent = eventType == null

    init {
        if (eventType == null) {
            eventType = EventType(null, "", activity.getProperPrimaryColor())
        }

        val binding = DialogEventTypeBinding.inflate(activity.layoutInflater).apply {
            setupColor(typeColor)
            typeTitle.setText(eventType!!.title)
            typeColor.setOnClickListener {
                if (eventType?.caldavCalendarId == 0) {
                    ColorPickerDialog(activity, eventType!!.color) { wasPositivePressed, color ->
                        if (wasPositivePressed) {
                            eventType!!.color = color
                            setupColor(typeColor)
                        }
                    }
                } else {
                    SelectEventTypeColorDialog(activity, eventType!!) {
                        eventType!!.color = it
                        setupColor(typeColor)
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, if (isNewEvent) R.string.add_new_type else R.string.edit_type) { alertDialog ->
                    alertDialog.showKeyboard(binding.typeTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        ensureBackgroundThread {
                            eventTypeConfirmed(binding.typeTitle.value, alertDialog)
                        }
                    }
                }
            }
    }

    private fun setupColor(view: ImageView) {
        view.setFillWithStroke(eventType!!.color, activity.getProperBackgroundColor())
    }

    private fun eventTypeConfirmed(title: String, dialog: AlertDialog) {
        val eventTypeClass = eventType?.type ?: OTHER_EVENT
        val eventTypeId = if (eventTypeClass == OTHER_EVENT) {
            activity.eventsHelper.getEventTypeIdWithTitle(title)
        } else {
            activity.eventsHelper.getEventTypeIdWithClass(eventTypeClass)
        }

        var isEventTypeTitleTaken = isNewEvent && eventTypeId != -1L
        if (!isEventTypeTitleTaken) {
            isEventTypeTitleTaken = !isNewEvent && eventType!!.id != eventTypeId && eventTypeId != -1L
        }

        if (title.isEmpty()) {
            activity.toast(R.string.title_empty)
            return
        } else if (isEventTypeTitleTaken) {
            activity.toast(R.string.type_already_exists)
            return
        }

        eventType!!.title = title
        if (eventType!!.caldavCalendarId != 0) {
            eventType!!.caldavDisplayName = title
        }

        eventType!!.id = activity.eventsHelper.insertOrUpdateEventTypeSync(eventType!!)

        if (eventType!!.id != -1L) {
            activity.runOnUiThread {
                dialog.dismiss()
                callback(eventType!!)
            }
        } else {
            activity.toast(R.string.editing_calendar_failed)
        }
    }
}
