package com.liturgical.calendar.activities

import android.os.Bundle
import com.liturgical.calendar.R
import com.liturgical.calendar.adapters.ManageEventTypesAdapter
import com.liturgical.calendar.dialogs.EditEventTypeDialog
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.interfaces.DeleteEventTypesListener
import com.liturgical.calendar.models.EventType
import com.secure.commons.extensions.toast
import com.secure.commons.extensions.updateTextColors
import com.secure.commons.helpers.NavigationIcon
import com.secure.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.activity_manage_event_types.*

class ManageEventTypesActivity : SimpleActivity(), DeleteEventTypesListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)
        setupOptionsMenu()

        getEventTypes()
        updateTextColors(manage_event_types_list)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(manage_event_types_toolbar, NavigationIcon.Arrow)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        EditEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        eventsHelper.getEventTypes(this, false) {
            val adapter = ManageEventTypesAdapter(this, it, this, manage_event_types_list) {
                showEventTypeDialog(it as EventType)
            }
            manage_event_types_list.adapter = adapter
        }
    }

    private fun setupOptionsMenu() {
        manage_event_types_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_event_type -> showEventTypeDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean): Boolean {
        if (eventTypes.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
            if (eventTypes.size == 1) {
                return false
            }
        }

        ensureBackgroundThread {
            eventsHelper.deleteEventTypes(eventTypes, deleteEvents)
        }
        return true
    }
}