package com.liturgical.calendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.adapters.MyDayPagerAdapter
import com.liturgical.calendar.databinding.DatePickerBinding
import com.liturgical.calendar.databinding.FragmentDaysHolderBinding
import com.liturgical.calendar.helpers.DAILY_VIEW
import com.liturgical.calendar.helpers.DAY_CODE
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.interfaces.NavigationListener
import com.secure.commons.extensions.getAlertDialogBuilder
import com.secure.commons.extensions.getDatePickerDialogTheme
import com.secure.commons.extensions.getProperBackgroundColor
import com.secure.commons.extensions.setupDialogStuff
import com.secure.commons.views.MyViewPager
import org.joda.time.DateTime

class DayFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_DAYS = 251

    private var viewPager: MyViewPager? = null
    private var defaultDailyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = DAILY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentDaysHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentDaysViewpager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val codes = getDays(currentDayCode)
        val dailyAdapter = MyDayPagerAdapter(requireActivity().supportFragmentManager, codes, this)
        defaultDailyPage = codes.size / 2


        viewPager!!.apply {
            adapter = dailyAdapter
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
            currentItem = defaultDailyPage
        }
        updateActionBarTitle()
    }

    private fun getDays(code: String): List<String> {
        val days = ArrayList<String>(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            days.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))
        }
        return days
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
        //val datePicker = datePickerBinding.findViewById<DatePicker>(R.id.date_picker)

        val dateTime = getCurrentDate()!!
        dateBinding.datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { dialog, which -> dateSelected(dateTime, dateBinding.datePicker) }
            .apply {
                activity?.setupDialogStuff(dateBinding.root, this)
            }
    }

    private fun dateSelected(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val day = datePicker.dayOfMonth
        val newDateTime = dateTime.withDate(year, month, day)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyDayPagerAdapter)?.updateCalendars(viewPager?.currentItem ?: 0)
    }

    override fun shouldGoToTodayBeVisible() = currentDayCode != todayDayCode

    override fun updateActionBarTitle() {
        (activity as MainActivity).updateTitle(getString(R.string.app_launcher_name))
    }

    override fun getNewEventDayCode() = currentDayCode

    override fun printView() {
        (viewPager?.adapter as? MyDayPagerAdapter)?.printCurrentView(viewPager?.currentItem ?: 0)
    }

    override fun getCurrentDate(): DateTime? {
        return if (currentDayCode != "") {
            Formatter.getDateTimeFromCode(currentDayCode)
        } else {
            null
        }
    }
}
