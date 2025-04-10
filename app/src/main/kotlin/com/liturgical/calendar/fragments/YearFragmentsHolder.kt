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
import com.liturgical.calendar.adapters.MyYearPagerAdapter
import com.liturgical.calendar.databinding.DatePickerBinding
import com.liturgical.calendar.databinding.FragmentYearsHolderBinding
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.helpers.YEARLY_VIEW
import com.liturgical.calendar.helpers.YEAR_TO_OPEN
import com.secure.commons.extensions.*
import com.secure.commons.views.MyViewPager
import org.joda.time.DateTime
import kotlin.text.toInt

class YearFragmentsHolder : MyFragmentHolder() {
    private val PREFILLED_YEARS = 61

    private var viewPager: MyViewPager? = null
    private var defaultYearlyPage = 0
    private var todayYear = 0
    private var currentYear = 0
    private var isGoToTodayVisible = false

    override val viewType = YEARLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(YEAR_TO_OPEN)
        currentYear = (if (dateTimeString != null) DateTime.parse(dateTimeString) else DateTime()).toString(Formatter.YEAR_PATTERN).toInt()
        todayYear = DateTime().toString(Formatter.YEAR_PATTERN).toInt()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragmentYearsHolderBinding.inflate(inflater, container, false)
        view.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = view.fragmentYearsViewpager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return view.root
    }

    private fun setupFragment() {
        val years = getYears(currentYear)
        val yearlyAdapter = MyYearPagerAdapter(requireActivity().supportFragmentManager, years)
        defaultYearlyPage = years.size / 2

        viewPager?.apply {
            adapter = yearlyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentYear = years[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    if (position < years.size) {
                        (activity as? MainActivity)?.updateTitle("${getString(R.string.app_launcher_name)} - ${years[position]}")
                    }
                }
            })
            currentItem = defaultYearlyPage
        }
        updateActionBarTitle()
    }

    private fun getYears(targetYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        years += targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2
        return years
    }

    override fun goToToday() {
        currentYear = todayYear
        setupFragment()
    }

    override fun showGoToDateDialog() {
        requireActivity().setTheme(requireContext().getDatePickerDialogTheme())
        val view = DatePickerBinding.inflate(layoutInflater)
        //val datePicker = view.findViewById<DatePicker>(R.id.date_picker)
        // TODO: get rid of findViewById
        view.datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()
        view.datePicker.findViewById<View>(Resources.getSystem().getIdentifier("month", "id", "android")).beGone()

        val dateTime = DateTime(Formatter.getDateTimeFromCode("${currentYear}0523").toString())
        view.datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { dialog, which -> datePicked(view.datePicker) }
            .apply {
                activity?.setupDialogStuff(view.root, this)
            }
    }

    private fun datePicked(datePicker: DatePicker) {
        val pickedYear = datePicker.year
        if (currentYear != pickedYear) {
            currentYear = datePicker.year
            setupFragment()
        }
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyYearPagerAdapter)?.updateCalendars(viewPager?.currentItem ?: 0)
    }

    override fun shouldGoToTodayBeVisible() = currentYear != todayYear

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateTitle("${getString(R.string.app_launcher_name)} - $currentYear")
    }

    override fun getNewEventDayCode() = Formatter.getTodayCode()

    override fun printView() {
        (viewPager?.adapter as? MyYearPagerAdapter)?.printCurrentView(viewPager?.currentItem ?: 0)
    }

    override fun getCurrentDate() = null
}
