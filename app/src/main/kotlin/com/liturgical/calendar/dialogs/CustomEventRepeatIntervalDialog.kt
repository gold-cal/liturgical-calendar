package com.liturgical.calendar.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogCustomEventRepeatIntervalBinding
import com.liturgical.calendar.helpers.DAY
import com.liturgical.calendar.helpers.MONTH
import com.liturgical.calendar.helpers.WEEK
import com.liturgical.calendar.helpers.YEAR
import com.secure.commons.extensions.*

class CustomEventRepeatIntervalDialog(val activity: Activity, val callback: (seconds: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding = DialogCustomEventRepeatIntervalBinding.inflate(activity.layoutInflater)

    init {
        binding.dialogRadioView.check(R.id.dialog_radio_days)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, _ -> confirmRepeatInterval() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(binding.dialogCustomRepeatIntervalValue)
                }
            }
    }

    private fun confirmRepeatInterval() {
        val value = binding.dialogCustomRepeatIntervalValue.value
        val multiplier = getMultiplier(binding.dialogRadioView.checkedRadioButtonId)
        val days = Integer.valueOf(value.ifEmpty { "0" })
        callback(days * multiplier)
        activity.hideKeyboard()
        dialog?.dismiss()
    }

    private fun getMultiplier(id: Int) = when (id) {
        R.id.dialog_radio_weeks -> WEEK
        R.id.dialog_radio_months -> MONTH
        R.id.dialog_radio_years -> YEAR
        else -> DAY
    }
}
