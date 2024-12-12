package com.liturgical.calendar.adapters

/* This adapter sets up the view for configuring the widget colors */

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
//import com.liturgical.calendar.dialogs.DeleteEventDialog
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.models.ListEvent
import com.liturgical.calendar.models.ListItem
import com.liturgical.calendar.models.ListSectionDay
import com.liturgical.calendar.models.ListSectionMonth
import com.secure.commons.adapters.MyRecyclerViewAdapter
import com.secure.commons.extensions.*
import com.secure.commons.helpers.MEDIUM_ALPHA
//import com.secure.commons.helpers.ensureBackgroundThread
import com.secure.commons.interfaces.RefreshRecyclerViewListener
import com.secure.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_list_item_widget.view.*
import kotlinx.android.synthetic.main.event_list_section_day_widget.view.*

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

    override fun getSelectableItemCount() = listItems.filter { it is ListEvent }.size

    override fun getIsItemSelectable(position: Int) = listItems[position] is ListEvent

    override fun getItemSelectionKey(position: Int) = (listItems.getOrNull(position) as? ListEvent)?.hashCode()

    override fun getItemKeyPosition(key: Int) = listItems.indexOfFirst { (it as? ListEvent)?.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        val layoutId = when (viewType) {
            ITEM_SECTION_DAY -> R.layout.event_list_section_day_widget
            ITEM_SECTION_MONTH -> R.layout.event_list_section_month_widget
            else -> R.layout.event_list_item_widget
        }
        return createViewHolder(layoutId, parent)
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
        view.apply {
            event_item_holder.isSelected = selectedKeys.contains(listEvent.hashCode())
            event_item_title.text = listEvent.title
            event_item_title.checkViewStrikeThrough(listEvent.isTaskCompleted)
            event_item_time.text = if (listEvent.isAllDay) "" else Formatter.getTimeFromTS(context, listEvent.startTS)
            if (listEvent.isAllDay) event_item_time.beGone()
            if (listEvent.startTS != listEvent.endTS) {
                if (!listEvent.isAllDay) {
                    event_item_time.text = "${event_item_time.text} - ${Formatter.getTimeFromTS(context, listEvent.endTS)}"
                }

                val startCode = Formatter.getDayCodeFromTS(listEvent.startTS)
                val endCode = Formatter.getDayCodeFromTS(listEvent.endTS)
                if (startCode != endCode) {
                    event_item_time.text = "${event_item_time.text} (${Formatter.getDateDayTitle(endCode)})"
                }
            }

            //event_item_description.text = if (replaceDescription) listEvent.location else listEvent.description.replace("\n", " ")
            //event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            event_item_color_bar.applyColorFilter(listEvent.color)

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

            event_item_time.setTextColor(newTextColor)
            event_item_title.setTextColor(newTextColor)
            //event_item_description.setTextColor(newTextColor)
            event_item_task_image.applyColorFilter(newTextColor)
            event_item_task_image.beVisibleIf(listEvent.isTask)
        }
    }

    private fun setupListSectionDay(view: View, listSectionDay: ListSectionDay) {
        view.event_section_title.apply {
            text = listSectionDay.title
            val dayColor = if (listSectionDay.isToday) properPrimaryColor else textColor
            setTextColor(dayColor)
        }
        view.event_section_background.applyColorFilter(dayBackgroundColor)
    }

    private fun setupListSectionMonth(view: View, listSectionMonth: ListSectionMonth) {
        view.event_section_title.apply {
            text = listSectionMonth.title
            setTextColor(properAccentColor)
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
