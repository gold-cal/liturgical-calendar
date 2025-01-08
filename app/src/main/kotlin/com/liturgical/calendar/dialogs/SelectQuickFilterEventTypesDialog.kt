package com.liturgical.calendar.dialogs

import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.adapters.FilterEventTypeAdapter
import com.liturgical.calendar.databinding.DialogFilterEventTypesBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsHelper
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.setupDialogStuff

class SelectQuickFilterEventTypesDialog(val activity: SimpleActivity) {
    private var dialog: AlertDialog? = null
    private val binding = DialogFilterEventTypesBinding.inflate(activity.layoutInflater)

    init {
        activity.eventsHelper.getEventTypes(activity, false) {
            val quickFilterEventTypes = activity.config.quickFilterEventTypes
            binding.filterEventTypesList.adapter = FilterEventTypeAdapter(activity, it, quickFilterEventTypes)

            activity.getAlertDialogBuilder()
                .setPositiveButton(R.string.ok) { dialogInterface, _ -> confirmEventTypes() }
                .setNegativeButton(R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
                        dialog = alertDialog
                    }
                }
        }
    }

    private fun confirmEventTypes() {
        val selectedItems = (binding.filterEventTypesList.adapter as FilterEventTypeAdapter).getSelectedItemsList().map {
            it.toString()
        }.toHashSet()

        if (activity.config.quickFilterEventTypes != selectedItems) {
            activity.config.quickFilterEventTypes = selectedItems
        }
        dialog?.dismiss()
    }
}
