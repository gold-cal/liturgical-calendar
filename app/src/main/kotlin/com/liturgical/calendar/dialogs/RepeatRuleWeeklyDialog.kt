package com.liturgical.calendar.dialogs

import android.app.Activity
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.DialogVerticalLinearLayoutBinding
import com.liturgical.calendar.databinding.MyCheckboxBinding
import com.liturgical.calendar.extensions.config
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.setupDialogStuff
import com.secure.commons.views.MyAppCompatCheckbox
import kotlin.math.pow

class RepeatRuleWeeklyDialog(val activity: Activity, val curRepeatRule: Int, val callback: (repeatRule: Int) -> Unit) {
    private val binding = DialogVerticalLinearLayoutBinding.inflate(activity.layoutInflater)

    init {
        val days = activity.resources.getStringArray(R.array.week_days)
        val checkboxes = ArrayList<MyAppCompatCheckbox>(7)
        for (i in 0..6) {
            val pow = 2.0.pow(i.toDouble()).toInt()
            MyCheckboxBinding.inflate(activity.layoutInflater).root.apply {
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
            binding.dialogVerticalLinearLayout.addView(it)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> callback(getRepeatRuleSum()) }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun getRepeatRuleSum(): Int {
        var sum = 0
        val cnt = binding.dialogVerticalLinearLayout.childCount
        for (i in 0 until cnt) {
            val child = binding.dialogVerticalLinearLayout.getChildAt(i)
            if (child is MyAppCompatCheckbox) {
                if (child.isChecked)
                    sum += child.id
            }
        }
        return sum
    }
}
