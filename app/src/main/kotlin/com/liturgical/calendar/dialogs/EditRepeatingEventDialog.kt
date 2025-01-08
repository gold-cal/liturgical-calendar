package com.liturgical.calendar.dialogs

import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.DialogEditRepeatingEventBinding
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.hideKeyboard
import com.secure.commons.extensions.setupDialogStuff

class EditRepeatingEventDialog(val activity: SimpleActivity, val isTask: Boolean = false, val callback: (allOccurrences: Int) -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogEditRepeatingEventBinding.inflate(activity.layoutInflater).apply {
            editRepeatingEventOneOnly.setOnClickListener { sendResult(0) }
            editRepeatingEventThisAndFutureOccurences.setOnClickListener { sendResult(1) }
            editRepeatingEventAllOccurrences.setOnClickListener { sendResult(2) }

            if (isTask) {
                editRepeatingEventTitle.setText(R.string.task_is_repeatable)
            } else {
                editRepeatingEventTitle.setText(R.string.event_is_repeatable)
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.hideKeyboard()
                }
            }
    }

    private fun sendResult(allOccurrences: Int) {
        callback(allOccurrences)
        dialog?.dismiss()
    }
}
