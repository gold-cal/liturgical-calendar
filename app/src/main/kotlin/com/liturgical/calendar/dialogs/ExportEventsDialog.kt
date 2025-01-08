package com.liturgical.calendar.dialogs

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.adapters.FilterEventTypeAdapter
import com.liturgical.calendar.databinding.DialogExportEventsBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsHelper
import com.secure.commons.dialogs.FilePickerDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.ensureBackgroundThread
import java.io.File

@SuppressLint("SetTextI18n")
class ExportEventsDialog(
    val activity: SimpleActivity, val path: String, val hidePath: Boolean,
    val callback: (file: File, eventTypes: ArrayList<Long>) -> Unit
) {
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config

    init {
        val binding = DialogExportEventsBinding.inflate(activity.layoutInflater).apply {
            exportEventsFolder.setText(activity.humanizePath(realPath))
            exportEventsFilename.setText("${activity.getString(R.string.events)}_${activity.getCurrentFormattedDateTime()}")
            exportPastEventsCheckbox.isChecked = config.exportPastEvents
            exportPastEventsCheckboxHolder.setOnClickListener {
                exportPastEventsCheckbox.toggle()
            }

            if (hidePath) {
                exportEventsFolderHint.beGone()
                exportEventsFolder.beGone()
            } else {
                exportEventsFolder.setOnClickListener {
                    activity.hideKeyboard(exportEventsFilename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportEventsFolder.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }

            activity.eventsHelper.getEventTypes(activity, false) { eventTypeArrayList ->
                val eventTypes = HashSet<String>()
                eventTypeArrayList.mapTo(eventTypes) { it.id.toString() }

                exportEventsTypesList.adapter = FilterEventTypeAdapter(activity, eventTypeArrayList, eventTypes)
                if (eventTypeArrayList.size > 1) {
                    exportEventsPickTypes.beVisible()
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_events) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportEventsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.ics")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastExportPath = file.absolutePath.getParentPath()
                                    config.exportPastEvents = binding.exportPastEventsCheckbox.isChecked

                                    val eventTypes = (binding.exportEventsTypesList.adapter as FilterEventTypeAdapter).getSelectedItemsList()
                                    callback(file, eventTypes)
                                    alertDialog.dismiss()
                                }
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
