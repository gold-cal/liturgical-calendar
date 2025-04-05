package com.liturgical.calendar.adapters

import android.view.*
import android.widget.PopupMenu
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.ItemEventTypeBinding
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.interfaces.DeleteEventTypesListener
import com.liturgical.calendar.models.EventType
import com.secure.commons.adapters.MyRecyclerViewAdapter
import com.secure.commons.dialogs.ConfirmationDialog
import com.secure.commons.dialogs.RadioGroupDialog
import com.secure.commons.extensions.*
import com.secure.commons.models.RadioItem
import com.secure.commons.views.MyRecyclerView

class ManageEventTypesAdapter(
    activity: SimpleActivity, val eventTypes: ArrayList<EventType>, val listener: DeleteEventTypesListener?, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_event_type

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editEventType()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = eventTypes.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = eventTypes.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = eventTypes.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        return createViewHolder(ItemEventTypeBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        holder.bindView(eventType, true, true) { itemView, layoutPosition ->
            setupView(itemView, eventType)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = eventTypes.size

    private fun getItemWithKey(key: Int): EventType? = eventTypes.firstOrNull { it.id?.toInt() == key }

    private fun getSelectedItems() = eventTypes.filter { selectedKeys.contains(it.id?.toInt()) } as ArrayList<EventType>

    private fun setupView(view: View, eventType: EventType) {
        ItemEventTypeBinding.bind(view).apply {
            eventItemFrame.isSelected = selectedKeys.contains(eventType.id?.toInt())
            eventTypeTitle.text = eventType.getDisplayTitle()
            eventTypeColor.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
            eventTypeTitle.setTextColor(textColor)
            /*if (eventType.title == "TLC") {
                reloadMenuItem.beVisible()
            }*/

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, eventType)
            }
        }
    }

    private fun showPopupMenu(view: View, eventType: EventType) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val eventTypeId = eventType.id!!.toInt()
                when (item.itemId) {
                    R.id.cab_edit -> {
                        executeItemMenuOperation(eventTypeId) {
                            itemClick(eventType)
                        }
                    }
                    R.id.cab_delete -> {
                        executeItemMenuOperation(eventTypeId) {
                            askConfirmDelete()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(eventTypeId: Int, callback: () -> Unit) {
        selectedKeys.clear()
        selectedKeys.add(eventTypeId)
        callback()
    }

    private fun editEventType() {
        itemClick.invoke(getSelectedItems().first())
        finishActMode()
    }

    private fun askConfirmDelete() {
        val eventTypes = eventTypes.filter { selectedKeys.contains(it.id?.toInt()) }.map { it.id } as ArrayList<Long>

        activity.eventsHelper.doEventTypesContainEvents(eventTypes) {
            activity.runOnUiThread {
                if (it) {
                    val MOVE_EVENTS = 0
                    val DELETE_EVENTS = 1
                    val res = activity.resources
                    val items = ArrayList<RadioItem>().apply {
                        add(RadioItem(MOVE_EVENTS, res.getString(R.string.move_events_into_default)))
                        add(RadioItem(DELETE_EVENTS, res.getString(R.string.remove_affected_events)))
                    }
                    RadioGroupDialog(activity, items) {
                        deleteEventTypes(it == DELETE_EVENTS)
                    }
                } else {
                    ConfirmationDialog(activity) {
                        deleteEventTypes(true)
                    }
                }
            }
        }
    }

    private fun deleteEventTypes(deleteEvents: Boolean) {
        val eventTypesToDelete = getSelectedItems()

        for (key in selectedKeys) {
            val type = getItemWithKey(key) ?: continue
            if (type.id == REGULAR_EVENT_TYPE_ID || type.id == BIRTHDAY_EVENT_TYPE_ID ||
                type.id == LITURGICAL_EVENT_TYPE_ID || type.id == ANNI_EVENT_TYPE_ID || type.id == HOLY_DAY_EVENT_TYPE_ID) {
                activity.toast(R.string.cannot_delete_default_type)
                eventTypesToDelete.remove(type)
                toggleItemSelection(false, getItemKeyPosition(type.id!!.toInt()))
                break
            }
        }

        if (listener?.deleteEventTypes(eventTypesToDelete, deleteEvents) == true) {
            val positions = getSelectedItemPositions()
            eventTypes.removeAll(eventTypesToDelete.toSet())
            removeSelectedItems(positions)
        }
    }
}
