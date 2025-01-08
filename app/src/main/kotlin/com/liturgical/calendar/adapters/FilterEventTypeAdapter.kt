package com.liturgical.calendar.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.FilterEventTypeViewBinding
import com.liturgical.calendar.models.EventType
import com.secure.commons.extensions.getProperBackgroundColor
import com.secure.commons.extensions.getProperPrimaryColor
import com.secure.commons.extensions.getProperTextColor
import com.secure.commons.extensions.setFillWithStroke

class FilterEventTypeAdapter(val activity: SimpleActivity, val eventTypes: List<EventType>, val displayEventTypes: Set<String>) :
    RecyclerView.Adapter<FilterEventTypeAdapter.ViewHolder>() {
    private val selectedKeys = HashSet<Long>()

    init {
        eventTypes.forEachIndexed { index, eventType ->
            if (displayEventTypes.contains(eventType.id.toString())) {
                selectedKeys.add(eventType.id!!)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, eventType: EventType, pos: Int) {
        if (select) {
            selectedKeys.add(eventType.id!!)
        } else {
            selectedKeys.remove(eventType.id)
        }

        notifyItemChanged(pos)
    }

    fun getSelectedItemsList() = selectedKeys.asSequence().map { it }.toMutableList() as ArrayList<Long>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FilterEventTypeViewBinding.inflate(activity.layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        holder.bindView(eventType)
    }

    override fun getItemCount() = eventTypes.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(eventType: EventType): View {
            val isSelected = selectedKeys.contains(eventType.id)
            FilterEventTypeViewBinding.bind(itemView).apply {
                filterEventTypeCheckbox.isChecked = isSelected
                filterEventTypeCheckbox.setColors(activity.getProperTextColor(), activity.getProperPrimaryColor(), activity.getProperBackgroundColor())
                filterEventTypeCheckbox.text = eventType.getDisplayTitle()
                filterEventTypeColor.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
                filterEventTypeHolder.setOnClickListener { viewClicked(!isSelected, eventType) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean, eventType: EventType) {
            toggleItemSelection(select, eventType, adapterPosition)
        }
    }
}
