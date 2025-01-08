package com.liturgical.calendar.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogDeleteEventBinding
import com.liturgical.calendar.helpers.DELETE_ALL_OCCURRENCES
import com.liturgical.calendar.helpers.DELETE_FUTURE_OCCURRENCES
import com.liturgical.calendar.helpers.DELETE_SELECTED_OCCURRENCE
import com.secure.commons.extensions.beVisibleIf
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.setupDialogStuff

class DeleteEventDialog(
    val activity: Activity,
    eventIds: List<Long>,
    hasRepeatableEvent: Boolean,
    isTask: Boolean = false,
    val callback: (deleteRule: Int) -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogDeleteEventBinding.inflate(activity.layoutInflater).apply {
            deleteEventRepeatDescription.beVisibleIf(hasRepeatableEvent)
            deleteEventRadioView.beVisibleIf(hasRepeatableEvent)
            if (!hasRepeatableEvent) {
                deleteEventRadioView.check(R.id.delete_event_all)
            }

            if (eventIds.size > 1) {
                deleteEventRepeatDescription.setText(R.string.selection_contains_repetition)
            }

            if (isTask) {
                deleteEventRepeatDescription.setText(R.string.task_is_repeatable)
            } else {
                deleteEventRepeatDescription.setText(R.string.event_is_repeatable)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.yes) { dialog, which -> dialogConfirmed(binding) }
            .setNegativeButton(R.string.no, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed(view: DialogDeleteEventBinding) {
        val deleteRule = when (view.deleteEventRadioView.checkedRadioButtonId) {
            R.id.delete_event_all -> DELETE_ALL_OCCURRENCES
            R.id.delete_event_future -> DELETE_FUTURE_OCCURRENCES
            else -> DELETE_SELECTED_OCCURRENCE
        }
        dialog?.dismiss()
        callback(deleteRule)
    }
}
