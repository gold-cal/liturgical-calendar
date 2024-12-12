package com.liturgical.calendar.dialogs

import android.app.Activity
import com.liturgical.calendar.R
import com.liturgical.calendar.extensions.config
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.setupDialogStuff
import com.secure.commons.views.MyAppCompatCheckbox
import kotlinx.android.synthetic.main.dialog_vertical_linear_layout.view.*

class RepeatRuleWeeklyDialog(val activity: Activity, val curRepeatRule: Int, val callback: (repeatRule: Int) -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_vertical_linear_layout, null)

    init {
        val days = activity.resources.getStringArray(R.array.week_days)
        val checkboxes = ArrayList<MyAppCompatCheckbox>(7)
        for (i in 0..6) {
            val pow = Math.pow(2.0, i.toDouble()).toInt()
            (activity.layoutInflater.inflate(R.layout.my_checkbox, null) as MyAppCompatCheckbox).apply {
                isChecked = curRepeatRule and pow != 0
                text = days[i]
                id = pow
                checkboxes.add(this)
            }
        }

        if (activity.config.isSundayFirst) {
            checkboxes.add(0, checkboxes.removeAt(6))
        }

        checkboxes.forEach {
            view.dialog_vertical_linear_layout.addView(it)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> callback(getRepeatRuleSum()) }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun getRepeatRuleSum(): Int {
        var sum = 0
        val cnt = view.dialog_vertical_linear_layout.childCount
        for (i in 0 until cnt) {
            val child = view.dialog_vertical_linear_layout.getChildAt(i)
            if (child is MyAppCompatCheckbox) {
                if (child.isChecked)
                    sum += child.id
            }
        }
        return sum
    }
}
