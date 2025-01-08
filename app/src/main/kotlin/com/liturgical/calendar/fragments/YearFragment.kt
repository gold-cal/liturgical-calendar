package com.liturgical.calendar.fragments

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.databinding.FragmentYearBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.getViewBitmap
import com.liturgical.calendar.extensions.printBitmap
import com.liturgical.calendar.helpers.YEAR_LABEL
import com.liturgical.calendar.helpers.YearlyCalendarImpl
import com.liturgical.calendar.interfaces.YearlyCalendar
import com.liturgical.calendar.models.DayYearly
import com.secure.commons.extensions.getProperPrimaryColor
import com.secure.commons.extensions.getProperTextColor
import com.secure.commons.extensions.updateTextColors
import org.joda.time.DateTime

class YearFragment : Fragment(), YearlyCalendar {
    private var mYear = 0
    private var mSundayFirst = false
    private var isPrintVersion = false
    private var lastHash = 0
    private var mCalendar: YearlyCalendarImpl? = null

    private lateinit var mView: FragmentYearBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mView = FragmentYearBinding.inflate(inflater, container, false)
        mYear = requireArguments().getInt(YEAR_LABEL)
        requireContext().updateTextColors(mView.calendarHolder)
        setupMonths()

        mCalendar = YearlyCalendarImpl(this, requireContext(), mYear)
        return mView.root
    }

    override fun onPause() {
        super.onPause()
        mSundayFirst = requireContext().config.isSundayFirst
    }

    override fun onResume() {
        super.onResume()
        val sundayFirst = requireContext().config.isSundayFirst
        if (sundayFirst != mSundayFirst) {
            mSundayFirst = sundayFirst
            setupMonths()
        }
        updateCalendar()
    }

    fun updateCalendar() {
        mCalendar?.getEvents(mYear)
    }

    private fun setupMonths() {
        val dateTime = DateTime().withDate(mYear, 2, 1).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        mView.month2.setDays(days)

        val now = DateTime()

        for (i in 1..12) {
            val monthView = when(i) {
                1 -> mView.month1
                2 -> mView.month2
                3 -> mView.month3
                4 -> mView.month4
                5 -> mView.month5
                6 -> mView.month6
                7 -> mView.month7
                8 -> mView.month8
                9 -> mView.month9
                10 -> mView.month10
                11 -> mView.month11
                12 -> mView.month12
                else -> { null }
            }
                //mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            if (!mSundayFirst) {
                dayOfWeek--
            }

            val monthLabel = when(i) {
                1 -> mView.month1Label
                2 -> mView.month2Label
                3 -> mView.month3Label
                4 -> mView.month4Label
                5 -> mView.month5Label
                6 -> mView.month6Label
                7 -> mView.month7Label
                8 -> mView.month8Label
                9 -> mView.month9Label
                10 -> mView.month10Label
                11 -> mView.month11Label
                12 -> mView.month12Label
                else -> { null }
            } as TextView
                //mView.findViewById<TextView>(resources.getIdentifier("month_${i}_label", "id", requireContext().packageName))
            val curTextColor = when {
                isPrintVersion -> resources.getColor(R.color.theme_light_text_color, null)
                else -> requireContext().getProperTextColor()
            }

            monthLabel.setTextColor(curTextColor)

            monthView?.firstDay = dayOfWeek
            monthView?.setOnClickListener {
                (activity as MainActivity).openMonthFromYearly(DateTime().withDate(mYear, i, 1))
            }
        }

        if (!isPrintVersion) {
            markCurrentMonth(now)
        }
    }

    private fun markCurrentMonth(now: DateTime) {
        if (now.year == mYear) {
            val monthLabel = when(now.monthOfYear) {
                1 -> mView.month1Label
                2 -> mView.month2Label
                3 -> mView.month3Label
                4 -> mView.month4Label
                5 -> mView.month5Label
                6 -> mView.month6Label
                7 -> mView.month7Label
                8 -> mView.month8Label
                9 -> mView.month9Label
                10 -> mView.month10Label
                11 -> mView.month11Label
                12 -> mView.month12Label
                else -> { null }
            } as TextView
                //mView.findViewById<TextView>(resources.getIdentifier("month_${now.monthOfYear}_label", "id", requireContext().packageName))
            monthLabel.setTextColor(requireContext().getProperPrimaryColor())

            val monthView = when(now.monthOfYear) {
                1 -> mView.month1
                2 -> mView.month2
                3 -> mView.month3
                4 -> mView.month4
                5 -> mView.month5
                6 -> mView.month6
                7 -> mView.month7
                8 -> mView.month8
                9 -> mView.month9
                10 -> mView.month10
                11 -> mView.month11
                12 -> mView.month12
                else -> { null }
            }
                //mView.findViewById<SmallMonthView>(resources.getIdentifier("month_${now.monthOfYear}", "id", requireContext().packageName))
            monthView?.todaysId = now.dayOfMonth
        }
    }

    override fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int) {
        if (!isAdded)
            return

        if (hashCode == lastHash) {
            return
        }

        lastHash = hashCode
        for (i in 1..12) {
            val monthView = when(i) {
                    1 -> mView.month1
                    2 -> mView.month2
                    3 -> mView.month3
                    4 -> mView.month4
                    5 -> mView.month5
                    6 -> mView.month6
                    7 -> mView.month7
                    8 -> mView.month8
                    9 -> mView.month9
                    10 -> mView.month10
                    11 -> mView.month11
                    12 -> mView.month12
                else -> { null }
            }
            //findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView?.setEvents(events.get(i))
        }
    }

    fun printCurrentView() {
        isPrintVersion = true
        setupMonths()
        toggleSmallMonthPrintModes()

        requireContext().printBitmap(mView.calendarHolder.getViewBitmap())

        isPrintVersion = false
        setupMonths()
        toggleSmallMonthPrintModes()
    }

    private fun toggleSmallMonthPrintModes() {
        for (i in 1..12) {
            val monthView = when(i) {
                1 -> mView.month1
                2 -> mView.month2
                3 -> mView.month3
                4 -> mView.month4
                5 -> mView.month5
                6 -> mView.month6
                7 -> mView.month7
                8 -> mView.month8
                9 -> mView.month9
                10 -> mView.month10
                11 -> mView.month11
                12 -> mView.month12
                else -> { null }
            }
                //mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView?.togglePrintMode()
        }
    }
}
