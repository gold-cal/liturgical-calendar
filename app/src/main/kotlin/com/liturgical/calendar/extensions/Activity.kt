package com.liturgical.calendar.extensions

import android.app.Activity
import com.liturgical.calendar.BuildConfig
import com.liturgical.calendar.R
import com.liturgical.calendar.dialogs.CustomEventRepeatIntervalDialog
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.models.Event
import com.secure.commons.activities.BaseSimpleActivity
import com.secure.commons.dialogs.RadioGroupDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.ensureBackgroundThread
import com.secure.commons.models.RadioItem
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = eventsDB.getEventsWithIds(ids) as ArrayList<Event>
        if (events.isEmpty()) {
            toast(R.string.no_items_found)
        }

        getFileOutputStream(file.toFileDirItem(this), true) {
            IcsExporter().exportEvents(this, it, events, false) { result ->
                if (result == IcsExporter.ExportResult.EXPORT_OK) {
                    sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
                }
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(YEAR)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    items.add(RadioItem(-1, getString(R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
            CustomEventRepeatIntervalDialog(this) { result ->
                callback(result)
            }
        } else {
            callback(it as Int)
        }
    }
}
