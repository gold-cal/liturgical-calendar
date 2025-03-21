package com.liturgical.calendar.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.databinding.DialogYearPickerBinding
import com.secure.commons.R
import com.secure.commons.extensions.*

class YearPickerDialog(val activity: Activity, val callback: (value: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding = DialogYearPickerBinding.inflate(activity.layoutInflater)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmReminder() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(binding.dialogYearPickerValue)
                }
            }
    }

    private fun confirmReminder() {
        val value = binding.dialogYearPickerValue.value
        //val multiplier = getMultiplier(binding.dialogRadioView.checkedRadioButtonId)
        val years = Integer.valueOf(value.ifEmpty { "0" })
        callback(years)
        activity.hideKeyboard()
        dialog?.dismiss()
    }
}
