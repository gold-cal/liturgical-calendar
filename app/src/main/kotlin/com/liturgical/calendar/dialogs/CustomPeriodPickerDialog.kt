package com.liturgical.calendar.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogCustomPeriodPickerBinding
import com.secure.commons.extensions.*
import com.secure.commons.helpers.DAY_SECONDS
import com.secure.commons.helpers.MONTH_SECONDS
import com.secure.commons.helpers.WEEK_SECONDS

class CustomPeriodPickerDialog(val activity: Activity, val callback: (value: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding = DialogCustomPeriodPickerBinding.inflate(activity.layoutInflater)

    init {
        binding.dialogCustomPeriodValue.setText("")
        binding.dialogRadioView.check(R.id.dialog_radio_days)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, _ -> confirmReminder() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(binding.dialogCustomPeriodValue)
                }
            }
    }

    private fun calculatePeriod(selectedPeriodValue: Int, selectedPeriodValueType: Int) = when (selectedPeriodValueType) {
        R.id.dialog_radio_days -> selectedPeriodValue * DAY_SECONDS
        R.id.dialog_radio_weeks -> selectedPeriodValue * WEEK_SECONDS
        else -> selectedPeriodValue * MONTH_SECONDS
    }

    private fun confirmReminder() {
        val value = binding.dialogCustomPeriodValue.value
        val type = binding.dialogRadioView.checkedRadioButtonId
        val periodValue = value.ifEmpty { "0" }

        val period = calculatePeriod(Integer.valueOf(periodValue), type)
        callback(period)
        activity.hideKeyboard()
        dialog?.dismiss()
    }
}
