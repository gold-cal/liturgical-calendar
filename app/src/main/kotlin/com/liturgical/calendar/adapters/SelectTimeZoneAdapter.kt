package com.liturgical.calendar.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.ItemSelectTimeZoneBinding
import com.liturgical.calendar.models.MyTimeZone
import com.secure.commons.extensions.getProperTextColor

class SelectTimeZoneAdapter(val activity: SimpleActivity, var timeZones: ArrayList<MyTimeZone>, val itemClick: (Any) -> Unit) :
    RecyclerView.Adapter<SelectTimeZoneAdapter.ViewHolder>() {
    val textColor = activity.getProperTextColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSelectTimeZoneBinding.inflate(activity.layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeZone = timeZones[position]
        holder.bindView(timeZone)
    }

    override fun getItemCount() = timeZones.size

    fun updateTimeZones(newTimeZones: ArrayList<MyTimeZone>) {
        timeZones = newTimeZones.clone() as ArrayList<MyTimeZone>
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(timeZone: MyTimeZone): View {
            ItemSelectTimeZoneBinding.bind(itemView).apply {
                itemTimeZoneTitle.text = timeZone.zoneName
                itemTimeZoneShift.text = timeZone.title

                itemTimeZoneTitle.setTextColor(textColor)
                itemTimeZoneShift.setTextColor(textColor)

                itemSelectTimeZoneHolder.setOnClickListener {
                    itemClick(timeZone)
                }
            }

            return itemView
        }
    }
}
