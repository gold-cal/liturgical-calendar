package com.liturgical.calendar.adapters

/* This adapter sets up the view for configuring the widget colors
* TODO: Make this view and normal list view use the same item_event.xml
*/

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.EventListItemWidgetBinding
import com.liturgical.calendar.databinding.EventListSectionDayWidgetBinding
import com.liturgical.calendar.databinding.EventListSectionMonthWidgetBinding
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.models.ListEvent
import com.liturgical.calendar.models.ListItem
import com.liturgical.calendar.models.ListSectionDay
import com.liturgical.calendar.models.ListSectionMonth
import com.secure.commons.adapters.MyRecyclerViewAdapter
import com.secure.commons.extensions.*
import com.secure.commons.helpers.MEDIUM_ALPHA
import com.secure.commons.interfaces.RefreshRecyclerViewListener
import com.secure.commons.views.MyRecyclerView

class WidgetConfigAdapter(
    activity: SimpleActivity, var listItems: ArrayList<ListItem>, val allowLongClick: Boolean, val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    //private val allDayString = resources.getString(R.string.all_day)
    //private val displayDescription = activity.config.displayDescription
    //private val replaceDescription = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private val now = getNowSeconds()
    //private var use24HourFormat = activity.config.use24HourFormat
    //private var currentItemsHash = listItems.hashCode()
    private var isPrintVersion = false
    //private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()

    init {
        setupDragListener(true)
        val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            activity.runOnUiThread {
                recyclerView.scrollToPosition(firstNonPastSectionIndex)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_event_list

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = listItems.filterIsInstance<ListEvent>().size

    override fun getIsItemSelectable(position: Int) = listItems[position] is ListEvent

    override fun getItemSelectionKey(position: Int) = (listItems.getOrNull(position) as? ListEvent)?.hashCode()

    override fun getItemKeyPosition(key: Int) = listItems.indexOfFirst { (it as? ListEvent)?.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        val layoutId = when (viewType) {
            ITEM_SECTION_DAY -> EventListSectionDayWidgetBinding.inflate(layoutInflater, parent, false).root
            ITEM_SECTION_MONTH -> EventListSectionMonthWidgetBinding.inflate(layoutInflater, parent, false).root
            else -> EventListItemWidgetBinding.inflate(layoutInflater, parent, false).root
        }
        return createViewHolder(layoutId)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val listItem = listItems[position]
        holder.bindView(listItem, true, allowLongClick && listItem is ListEvent) { itemView, layoutPosition ->
            when (listItem) {
                is ListSectionDay -> setupListSectionDay(itemView, listItem)
                is ListEvent -> setupListEvent(itemView, listItem)
                is ListSectionMonth -> setupListSectionMonth(itemView, listItem)
            }
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    override fun getItemViewType(position: Int) = when {
        listItems[position] is ListEvent -> ITEM_EVENT
        listItems[position] is ListSectionDay -> ITEM_SECTION_DAY
        else -> ITEM_SECTION_MONTH
    }

    /*fun toggle24HourFormat(use24HourFormat: Boolean) {
        this.use24HourFormat = use24HourFormat
        notifyDataSetChanged()
    }*/

    /*fun updateListItems(newListItems: ArrayList<ListItem>) {
        if (newListItems.hashCode() != currentItemsHash) {
            currentItemsHash = newListItems.hashCode()
            listItems = newListItems.clone() as ArrayList<ListItem>
            recyclerView.resetItemCount()
            notifyDataSetChanged()
            finishActMode()
        }
    }*/

    /*fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }
        notifyDataSetChanged()
    }*/

    private fun setupListEvent(view: View, listEvent: ListEvent) {
        EventListItemWidgetBinding.bind(view).apply {
            eventItemHolder.isSelected = selectedKeys.contains(listEvent.hashCode())
            eventItemTitle.text = listEvent.title
            eventItemTitle.checkViewStrikeThrough(listEvent.isTaskCompleted)
            eventItemTime.text = if (listEvent.isAllDay) "" else Formatter.getTimeFromTS(root.context, listEvent.startTS)
            if (listEvent.isAllDay) eventItemTime.beGone()
            if (listEvent.startTS != listEvent.endTS) {
                if (!listEvent.isAllDay) {
                    eventItemTime.text = "${eventItemTime.text} - ${Formatter.getTimeFromTS(root.context, listEvent.endTS)}"
                }

                val startCode = Formatter.getDayCodeFromTS(listEvent.startTS)
                val endCode = Formatter.getDayCodeFromTS(listEvent.endTS)
                if (startCode != endCode) {
                    eventItemTime.text = "${eventItemTime.text} (${Formatter.getDateDayTitle(endCode)})"
                }
            }

            //event_item_description.text = if (replaceDescription) listEvent.location else listEvent.description.replace("\n", " ")
            //event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            eventItemColorBar.applyColorFilter(listEvent.color)

            var newTextColor = textColor
            if (listEvent.isAllDay || listEvent.startTS <= now && listEvent.endTS <= now) {
                if (listEvent.isAllDay && Formatter.getDayCodeFromTS(listEvent.startTS) == Formatter.getDayCodeFromTS(now) && !isPrintVersion) {
                    newTextColor = properAccentColor
                }

                val adjustAlpha = if (listEvent.isTask) {
                    dimCompletedTasks && listEvent.isTaskCompleted
                } else {
                    dimPastEvents && listEvent.isPastEvent && !isPrintVersion
                }
                if (adjustAlpha) {
                    newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
                }
            } else if (listEvent.startTS <= now && listEvent.endTS >= now && !isPrintVersion) {
                newTextColor = properAccentColor
            }

            eventItemTime.setTextColor(newTextColor)
            eventItemTitle.setTextColor(newTextColor)
            //event_item_description.setTextColor(newTextColor)
            eventItemTaskImage.applyColorFilter(newTextColor)
            eventItemTaskImage.beVisibleIf(listEvent.isTask)
        }
    }

    private fun setupListSectionDay(view: View, listSectionDay: ListSectionDay) {
        EventListSectionDayWidgetBinding.bind(view).apply {
            eventSectionTitle.text = listSectionDay.title
            val dayColor = if (listSectionDay.isToday) properPrimaryColor else textColor
            eventSectionTitle.setTextColor(dayColor)
            eventSectionBackground.applyColorFilter(dayBackgroundColor)
        }
    }

    private fun setupListSectionMonth(view: View, listSectionMonth: ListSectionMonth) {
        EventListSectionMonthWidgetBinding.bind(view).apply {
            eventSectionTitle.text = listSectionMonth.title
            eventSectionTitle.setTextColor(properAccentColor)
        }
    }

    //private fun shareEvents() = activity.shareEvents(getSelectedEventIds())

    /*private fun getSelectedEventIds() =
        listItems.filter { it is ListEvent && selectedKeys.contains(it.hashCode()) }.map { (it as ListEvent).id }.toMutableList() as ArrayList<Long>*/

    /*private fun askConfirmDelete() {
        val eventIds = getSelectedEventIds()
        val eventsToDelete = listItems.filter { selectedKeys.contains((it as? ListEvent)?.hashCode()) } as List<ListEvent>
        val timestamps = eventsToDelete.mapNotNull { (it as? ListEvent)?.startTS }

        val hasRepeatableEvent = eventsToDelete.any { it.isRepeatable }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) {
            listItems.removeAll(eventsToDelete)

            ensureBackgroundThread {
                val nonRepeatingEventIDs = eventsToDelete.filter { !it.isRepeatable }.mapNotNull { it.id }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)

                val repeatingEventIDs = eventsToDelete.filter { it.isRepeatable }.map { it.id }
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }*/
}
