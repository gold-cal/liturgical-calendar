package com.liturgical.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.EventListItemBinding
import com.liturgical.calendar.dialogs.DeleteEventDialog
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.models.Event
import com.secure.commons.adapters.MyRecyclerViewAdapter
import com.secure.commons.extensions.*
import com.secure.commons.helpers.MEDIUM_ALPHA
import com.secure.commons.helpers.ensureBackgroundThread
import com.secure.commons.views.MyRecyclerView

class DayEventsAdapter(activity: SimpleActivity, val events: ArrayList<Event>, recyclerView: MyRecyclerView, var dayCode: String, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    //private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private var isPrintVersion = false
    private var isDgb = activity.config.allowAppDbg
    private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_day

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = events.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = events.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = events.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        return ViewHolder(EventListItemBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.bindView(event, true, true) { itemView, layoutPosition ->
            setupView(itemView, event)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = events.size

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }

        notifyDataSetChanged()
    }

    private fun setupView(view: View, event: Event) {
        EventListItemBinding.bind(view).apply {
            eventItemHolder.isSelected = selectedKeys.contains(event.id?.toInt())
            eventItemHolder.background.applyColorFilter(textColor)
            eventItemTitle.text = event.title
            eventItemTitle.checkViewStrikeThrough(event.isTaskCompleted())
            eventItemTime.text = if (event.getIsAllDay()) "" else Formatter.getTimeFromTS(root.context, event.startTS)
            if (!isDgb) {
                if (event.getIsAllDay()) eventItemTime.beGone()
            }
            if (event.startTS != event.endTS && !event.getIsAllDay()) {
                val startDayCode = Formatter.getDayCodeFromTS(event.startTS)
                val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                val startDate = Formatter.getDayTitle(activity, startDayCode, false)
                val endDate = Formatter.getDayTitle(activity, endDayCode, false)
                val startTimeString = eventItemTime.text
                val endTimeString = Formatter.getTimeFromTS(root.context, event.endTS)
                val startDayString = if (startDayCode != dayCode) " ($startDate)" else ""
                val endDayString = if (endDayCode != dayCode) " ($endDate)" else ""
                val text = "$startTimeString$startDayString - $endTimeString$endDayString"
                eventItemTime.text = text
            }

            if (isDgb) {
                var text = "importId: " + event.importId
                eventItemTime.text = text
                text = "source: " + event.source
                eventItemDescription.text = text
            } else {
                eventItemDescription.text = if (replaceDescriptionWithLocation) event.location else event.description.replace("\n", " ")
                eventItemDescription.beVisibleIf(displayDescription && eventItemDescription.text.isNotEmpty())
            }
            eventItemColorBar.background.applyColorFilter(event.color)

            var newTextColor = textColor

            val adjustAlpha = if (event.isTask()) {
                dimCompletedTasks && event.isTaskCompleted()
            } else {
                dimPastEvents && event.isPastEvent && !isPrintVersion
            }

            if (adjustAlpha) {
                newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
            }

            eventItemTime.setTextColor(newTextColor)
            eventItemTitle.setTextColor(newTextColor)
            eventItemDescription.setTextColor(newTextColor)
            eventItemTaskImage.applyColorFilter(newTextColor)
            eventItemTaskImage.beVisibleIf(event.isTask())

            val startMargin = if (event.isTask()) {
                0
            } else {
                mediumMargin
            }

            (eventItemTitle.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }

    private fun shareEvents() = activity.shareEvents(selectedKeys.distinct().map { it.toLong() })

    private fun askConfirmDelete() {
        val eventIds = selectedKeys.map { it.toLong() }.toMutableList()
        val eventsToDelete = events.filter { selectedKeys.contains(it.id?.toInt()) }
        val timestamps = eventsToDelete.map { it.startTS }
        val positions = getSelectedItemPositions()

        val hasRepeatableEvent = eventsToDelete.any { it.repeatInterval > 0 }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) { it ->
            events.removeAll(eventsToDelete)

            ensureBackgroundThread {
                val nonRepeatingEventIDs = eventsToDelete.asSequence().filter { it.repeatInterval == 0 }.mapNotNull { it.id }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)

                val repeatingEventIDs = eventsToDelete.asSequence().filter { it.repeatInterval != 0 }.mapNotNull { it.id }.toList()
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    removeSelectedItems(positions)
                }
            }
        }
    }
}
