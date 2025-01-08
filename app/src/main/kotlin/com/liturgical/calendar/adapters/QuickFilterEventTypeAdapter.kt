package com.liturgical.calendar.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.QuickFilterEventTypeViewBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.models.EventType
import com.secure.commons.extensions.adjustAlpha
import com.secure.commons.extensions.getProperTextColor
import com.secure.commons.helpers.LOWER_ALPHA

class QuickFilterEventTypeAdapter(
    val activity: SimpleActivity,
    private val allEventTypes: List<EventType>,
    private val quickFilterEventTypeIds: Set<String>,
    val callback: () -> Unit
) :
    RecyclerView.Adapter<QuickFilterEventTypeAdapter.ViewHolder>() {
    private val activeKeys = HashSet<Long>()
    private val quickFilterEventTypes = ArrayList<EventType>()
    private val displayEventTypes = activity.config.displayEventTypes

    private val textColorActive = activity.getProperTextColor()
    private val textColorInactive = textColorActive.adjustAlpha(LOWER_ALPHA)

    private val minItemWidth = activity.resources.getDimensionPixelSize(R.dimen.quick_filter_min_width)
    private var lastClickTS = 0L

    init {
        quickFilterEventTypeIds.forEach { quickFilterEventType ->
            val eventType = allEventTypes.firstOrNull { eventType -> eventType.id.toString() == quickFilterEventType } ?: return@forEach
            quickFilterEventTypes.add(eventType)

            if (displayEventTypes.contains(eventType.id.toString())) {
                activeKeys.add(eventType.id!!)
            }
        }

        quickFilterEventTypes.sortBy { it.title.lowercase() }
    }

    private fun toggleItemSelection(select: Boolean, eventType: EventType, pos: Int) {
        if (select) {
            activeKeys.add(eventType.id!!)
        } else {
            activeKeys.remove(eventType.id)
        }

        notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val parentWidth = parent.measuredWidth
        val nrOfItems = quickFilterEventTypes.size
        val view = QuickFilterEventTypeViewBinding.inflate(activity.layoutInflater, parent, false).root
        if (nrOfItems * minItemWidth > parentWidth) view.layoutParams.width = minItemWidth
        else view.layoutParams.width = parentWidth / nrOfItems
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = quickFilterEventTypes[position]
        holder.bindView(eventType)
    }

    override fun getItemCount() = quickFilterEventTypes.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(eventType: EventType): View {
            val isSelected = activeKeys.contains(eventType.id)
            QuickFilterEventTypeViewBinding.bind(itemView).apply {
                quickFilterEventType.text = eventType.title
                val textColor = if (isSelected) textColorActive else textColorInactive
                quickFilterEventType.setTextColor(textColor)

                val indicatorHeightRes = if (isSelected) R.dimen.quick_filter_active_line_size else R.dimen.quick_filter_inactive_line_size
                quickFilterEventTypeColor.layoutParams.height = root.resources.getDimensionPixelSize(indicatorHeightRes)
                quickFilterEventTypeColor.setBackgroundColor(eventType.color)

                // avoid too quick clicks, could cause glitches
                quickFilterEventType.setOnClickListener {
                    if (System.currentTimeMillis() - lastClickTS > 300) {
                        lastClickTS = System.currentTimeMillis()
                        viewClicked(!isSelected, eventType)
                        callback()
                    }
                }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean, eventType: EventType) {
            activity.config.displayEventTypes = if (select) {
                activity.config.displayEventTypes.plus(eventType.id.toString())
            } else {
                activity.config.displayEventTypes.minus(eventType.id.toString())
            }

            toggleItemSelection(select, eventType, adapterPosition)
        }
    }
}
