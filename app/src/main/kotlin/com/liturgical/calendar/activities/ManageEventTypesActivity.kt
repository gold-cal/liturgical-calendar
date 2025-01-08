package com.liturgical.calendar.activities

import android.os.Bundle
import com.liturgical.calendar.R
import com.liturgical.calendar.adapters.ManageEventTypesAdapter
import com.liturgical.calendar.databinding.ActivityManageEventTypesBinding
import com.liturgical.calendar.dialogs.EditEventTypeDialog
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.interfaces.DeleteEventTypesListener
import com.liturgical.calendar.models.EventType
import com.secure.commons.extensions.toast
import com.secure.commons.extensions.updateTextColors
import com.secure.commons.extensions.viewBinding
import com.secure.commons.helpers.NavigationIcon
import com.secure.commons.helpers.ensureBackgroundThread

class ManageEventTypesActivity : SimpleActivity(), DeleteEventTypesListener {
    private val binding by viewBinding(ActivityManageEventTypesBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        getEventTypes()
        updateTextColors(binding.manageEventTypesList)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.manageEventTypesToolbar, NavigationIcon.Arrow)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        EditEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        eventsHelper.getEventTypes(this, false) { eventTypes ->
            val adapter = ManageEventTypesAdapter(this, eventTypes, this, binding.manageEventTypesList) {
                showEventTypeDialog(it as EventType)
            }
            binding.manageEventTypesList.adapter = adapter
        }
    }

    private fun setupOptionsMenu() {
        binding.manageEventTypesToolbar.setOnMenuItemClickListener { menuItem ->
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
