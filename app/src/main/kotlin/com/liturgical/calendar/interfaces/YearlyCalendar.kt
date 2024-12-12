package com.liturgical.calendar.interfaces

import android.util.SparseArray
import com.liturgical.calendar.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
