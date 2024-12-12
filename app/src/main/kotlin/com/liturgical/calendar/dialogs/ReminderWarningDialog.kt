package com.liturgical.calendar.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.hideKeyboard
import com.secure.commons.extensions.setupDialogStuff
import com.secure.commons.extensions.showErrorToast

class ReminderWarningDialog(val activity: Activity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_reminder_warning, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNeutralButton(R.string.settings, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.disclaimer, cancelOnTouchOutside = false) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        redirectToSettings()
                    }
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }

    private fun redirectToSettings() {
        activity.hideKeyboard()
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)

            try {
                activity.startActivity(this)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }
}
