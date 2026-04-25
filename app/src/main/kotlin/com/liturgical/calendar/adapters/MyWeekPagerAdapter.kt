package com.liturgical.calendar.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
// Deprecated in api level 27
import androidx.fragment.app.FragmentStatePagerAdapter
//import androidx.viewpager2.adapter.FragmentStateAdapter
import com.liturgical.calendar.fragments.WeekFragment
import com.liturgical.calendar.helpers.WEEK_START_TIMESTAMP
import com.liturgical.calendar.interfaces.WeekFragmentListener

class MyWeekPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle, private val mWeekTimestamps: List<Long>,
                         private val mListener: WeekFragmentListener) : FragmentStatePagerAdapter(fm) /*FragmentStateAdapter(fm, lifecycle)*/ {
    private val mFragments = SparseArray<WeekFragment>()

    override fun getCount() = mWeekTimestamps.size

    // originally getItem()
    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val weekTimestamp = mWeekTimestamps[position]
        bundle.putLong(WEEK_START_TIMESTAMP, weekTimestamp)

        val fragment = WeekFragment()
        fragment.arguments = bundle
        fragment.listener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateScrollY(pos: Int, y: Int) {
        mFragments[pos - 1]?.updateScrollY(y)
        mFragments[pos + 1]?.updateScrollY(y)
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }

    fun updateNotVisibleScaleLevel(pos: Int) {
        mFragments[pos - 1]?.updateNotVisibleViewScaleLevel()
        mFragments[pos + 1]?.updateNotVisibleViewScaleLevel()
    }

    fun togglePrintMode(pos: Int) {
        mFragments[pos].togglePrintMode()
    }
}
