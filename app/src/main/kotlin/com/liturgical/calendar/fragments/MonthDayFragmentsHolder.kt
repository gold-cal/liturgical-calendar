package com.liturgical.calendar.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.adapters.MyMonthDayPagerAdapter
import com.liturgical.calendar.databinding.DatePickerBinding
import com.liturgical.calendar.databinding.FragmentMonthsDaysHolderBinding
import com.liturgical.calendar.extensions.getMonthCode
import com.liturgical.calendar.helpers.DAY_CODE
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.helpers.MONTHLY_DAILY_VIEW
import com.liturgical.calendar.interfaces.NavigationListener
import com.secure.commons.extensions.*
import com.secure.commons.views.MyViewPager
import org.joda.time.DateTime

class MonthDayFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_MONTHS = 251

    private var viewPager: MyViewPager? = null
    private var defaultMonthlyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = MONTHLY_DAILY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMonthsDaysHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentMonthsDaysViewpager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val codes = getMonths(currentDayCode)
        val monthlyDailyAdapter = MyMonthDayPagerAdapter(requireActivity().supportFragmentManager, codes, this)
        defaultMonthlyPage = codes.size / 2

        viewPager!!.apply {
            adapter = monthlyDailyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentDayCode = codes[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }
            })
            currentItem = defaultMonthlyPage
        }
        updateActionBarTitle()
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code).withDayOfMonth(1)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }

        return months
    }

    override fun goLeft() {
        viewPager!!.currentItem -= 1
    }

    override fun goRight() {
        viewPager!!.currentItem += 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        currentDayCode = Formatter.getDayCodeFromDateTime(dateTime)
        setupFragment()
    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }

    override fun showGoToDateDialog() {
        requireActivity().setTheme(requireContext().getDatePickerDialogTheme())
        val dateBinding = DatePickerBinding.inflate(layoutInflater)
        //val datePicker = dateBinding.findViewById<DatePicker>(R.id.date_picker)
        dateBinding.datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()
        val dateTime = getCurrentDate()!!
        dateBinding.datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { dialog, which -> datePicked(dateTime, dateBinding.datePicker) }
            .apply {
                activity?.setupDialogStuff(dateBinding.root, this)
            }
    }

    private fun datePicked(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyMonthDayPagerAdapter)?.updateCalendars(viewPager?.currentItem ?: 0)
    }

    override fun shouldGoToTodayBeVisible() = currentDayCode.getMonthCode() != todayDayCode.getMonthCode()

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateTitle(getString(R.string.app_launcher_name))
    }

    override fun getNewEventDayCode() = (viewPager?.adapter as? MyMonthDayPagerAdapter)?.getNewEventDayCode(viewPager?.currentItem ?: 0)
        ?: if (shouldGoToTodayBeVisible()) {
            currentDayCode
        } else {
            todayDayCode
        }

    override fun printView() {}

    override fun getCurrentDate(): DateTime? {
        return if (currentDayCode != "") {
            DateTime(Formatter.getDateTimeFromCode(currentDayCode).toString())
        } else {
            null
        }
    }
}
