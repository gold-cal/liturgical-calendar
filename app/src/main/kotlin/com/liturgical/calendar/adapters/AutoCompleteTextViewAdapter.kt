package com.liturgical.calendar.adapters

import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.databinding.ItemAutocompleteEmailNameBinding
import com.liturgical.calendar.models.Attendee
import com.secure.commons.extensions.normalizeString
import com.secure.commons.helpers.SimpleContactsHelper

class AutoCompleteTextViewAdapter(val activity: SimpleActivity, val contacts: ArrayList<Attendee>) : ArrayAdapter<Attendee>(activity, 0, contacts) {
    var resultList = ArrayList<Attendee>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = resultList[position]
        var listItem = convertView
        if (listItem == null || listItem.tag != contact.name.isNotEmpty()) {
            //val layout = if (contact.name.isNotEmpty()) ItemAutocompleteEmailNameBinding else kd
                //R.layout.item_autocomplete_email_name else R.layout.item_autocomplete_email
            listItem = ItemAutocompleteEmailNameBinding.inflate(activity.layoutInflater).root
            //listItem = LayoutInflater.from(activity).inflate(layout, parent, false)
        }

        val nameToUse = when {
            contact.name.isNotEmpty() -> contact.name
            contact.email.isNotEmpty() -> contact.email
            else -> "A"
        }

        val placeholder = BitmapDrawable(activity.resources, SimpleContactsHelper(context).getContactLetterIcon(nameToUse))
        ItemAutocompleteEmailNameBinding.bind(listItem).apply {
            listItem.tag = contact.name.isNotEmpty()
            itemAutocompleteName.text = contact.name
            itemAutocompleteEmail.text = contact.email

            contact.updateImage(context, itemAutocompleteImage, placeholder)
        }

        return listItem
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = Filter.FilterResults()
            if (constraint != null) {
                resultList.clear()
                val searchString = constraint.toString().normalizeString()
                contacts.forEach {
                    if (it.email.contains(searchString, true) || it.name.contains(searchString, true)) {
                        resultList.add(it)
                    }
                }

                resultList.sortWith(compareBy<Attendee>
                { it.name.startsWith(searchString, true) }.thenBy
                { it.email.startsWith(searchString, true) }.thenBy
                { it.name.contains(searchString, true) }.thenBy
                { it.email.contains(searchString, true) })
                resultList.reverse()

                filterResults.values = resultList
                filterResults.count = resultList.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results?.count ?: -1 > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?) = (resultValue as? Attendee)?.getPublicName()
    }

    override fun getItem(index: Int) = resultList[index]

    override fun getCount() = resultList.size
}
