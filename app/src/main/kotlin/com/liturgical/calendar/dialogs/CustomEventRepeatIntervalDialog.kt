package com.liturgical.calendar.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.helpers.DAY
import com.liturgical.calendar.helpers.MONTH
import com.liturgical.calendar.helpers.WEEK
import com.liturgical.calendar.helpers.YEAR
import com.secure.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_custom_event_repeat_interval.view.*

class CustomEventRepeatIntervalDialog(val activity: Activity, val callback: (seconds: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private var view = activity.layoutInflater.inflate(R.layout.dialog_custom_event_repeat_interval, null) as ViewGroup

    init {
        view.dialog_radio_view.check(R.id.dialog_radio_days)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmRepeatInterval() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(view.dialog_custom_repeat_interval_value)
                }
            }
    }

    private fun confirmRepeatInterval() {
        val value = view.dialog_custom_repeat_interval_value.value
        val multiplier = getMultiplier(view.dialog_radio_view.checkedRadioButtonId)
        val days = Integer.valueOf(if (value.isEmpty()) "0" else value)
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
