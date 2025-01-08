package com.liturgical.calendar.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.adapters.MyWeekPagerAdapter
import com.liturgical.calendar.databinding.DatePickerBinding
import com.liturgical.calendar.databinding.FragmentWeekHolderBinding
import com.liturgical.calendar.databinding.WeeklyViewHourTextviewBinding
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.helpers.WEEKLY_VIEW
import com.liturgical.calendar.helpers.WEEK_START_DATE_TIME
import com.liturgical.calendar.interfaces.WeekFragmentListener
import com.liturgical.calendar.views.MyScrollView
import com.secure.commons.extensions.*
import com.secure.commons.helpers.WEEK_SECONDS
import com.secure.commons.views.MyViewPager
import org.joda.time.DateTime

class WeekFragmentsHolder : MyFragmentHolder(), WeekFragmentListener {
    private val PREFILLED_WEEKS = 151
    private val MAX_SEEKBAR_VALUE = 14

    private var viewPager: MyViewPager? = null
    private var weekHolder: FragmentWeekHolderBinding? = null
    private var defaultWeeklyPage = 0
    private var thisWeekTS = 0L
    private var currentWeekTS = 0L
    private var isGoToTodayVisible = false
    private var weekScrollY = 0

    override val viewType = WEEKLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(WEEK_START_DATE_TIME) ?: return
        currentWeekTS = (DateTime.parse(dateTimeString) ?: DateTime()).seconds()
        thisWeekTS = DateTime.parse(requireContext().getDatesWeekDateTime(DateTime())).seconds()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        weekHolder = FragmentWeekHolderBinding.inflate(inflater, container, false)
        weekHolder!!.root.background = ColorDrawable(requireContext().getProperBackgroundColor())

        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        weekHolder!!.weekViewHoursHolder.setPadding(0, 0, 0, itemHeight)

        viewPager = weekHolder!!.weekViewViewPager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return weekHolder!!.root
    }

    override fun onResume() {
        super.onResume()
        context?.config?.allowCustomizeDayCount?.let { allow ->
            weekHolder!!.weekViewDaysCount.beVisibleIf(allow)
            weekHolder!!.weekViewSeekbar.beVisibleIf(allow)
        }
        setupSeekbar()
    }

    private fun setupFragment() {
        addHours()
        setupWeeklyViewPager()

        weekHolder!!.weekViewHoursScrollview.setOnTouchListener { view, motionEvent -> true }

        weekHolder!!.weekViewSeekbar.apply {
            progress = context?.config?.weeklyViewDays ?: 7
            max = MAX_SEEKBAR_VALUE

            onSeekBarChangeListener {
                if (it == 0) {
                    progress = 1
                }

                updateWeeklyViewDays(progress)
            }
        }

        updateActionBarTitle()
    }

    private fun setupWeeklyViewPager() {
        val weekTSs = getWeekTimestamps(currentWeekTS)
        val weeklyAdapter = MyWeekPagerAdapter(requireActivity().supportFragmentManager, weekTSs, this)

        defaultWeeklyPage = weekTSs.size / 2

        viewPager!!.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    currentWeekTS = weekTSs[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    setupWeeklyActionbarTitle(weekTSs[position])
                }
            })
            currentItem = defaultWeeklyPage
        }

        weekHolder!!.weekViewHoursScrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                weekScrollY = y
                weeklyAdapter.updateScrollY(viewPager!!.currentItem, y)
            }
        })
    }

    private fun addHours(textColor: Int = requireContext().getProperTextColor()) {
        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        weekHolder!!.weekViewHoursHolder.removeAllViews()
        val hourDateTime = DateTime().withDate(2000, 1, 1).withTime(0, 0, 0, 0)
        for (i in 1..23) {
            val formattedHours = Formatter.getHours(requireContext(), hourDateTime.withHourOfDay(i))
            WeeklyViewHourTextviewBinding.inflate(layoutInflater, null, false).root.apply {
                text = formattedHours
                setTextColor(textColor)
                height = itemHeight
                weekHolder!!.weekViewHoursHolder.addView(this)
            }
        }
    }

    private fun getWeekTimestamps(targetSeconds: Long): List<Long> {
        val weekTSs = ArrayList<Long>(PREFILLED_WEEKS)
        val dateTime = Formatter.getDateTimeFromTS(targetSeconds)
        val shownWeekDays = requireContext().config.weeklyViewDays
        var currentWeek = dateTime.minusDays(PREFILLED_WEEKS / 2 * shownWeekDays)
        for (i in 0 until PREFILLED_WEEKS) {
            weekTSs.add(currentWeek.seconds())
            currentWeek = currentWeek.plusDays(shownWeekDays)
        }
        return weekTSs
    }

    private fun setupWeeklyActionbarTitle(timestamp: Long) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val endDateTime = Formatter.getDateTimeFromTS(timestamp + WEEK_SECONDS)
        val startMonthName = Formatter.getMonthName(requireContext(), startDateTime.monthOfYear)
        if (startDateTime.monthOfYear == endDateTime.monthOfYear) {
            var newTitle = startMonthName
            if (startDateTime.year != DateTime().year) {
                newTitle += " - ${startDateTime.year}"
            }
            (activity as MainActivity).updateTitle(newTitle)
        } else {
            val endMonthName = Formatter.getMonthName(requireContext(), endDateTime.monthOfYear)
            (activity as MainActivity).updateTitle("$startMonthName - $endMonthName")
        }
        (activity as MainActivity).updateSubtitle("${getString(R.string.week)} ${startDateTime.plusDays(3).weekOfWeekyear}")
    }

    override fun goToToday() {
        currentWeekTS = thisWeekTS
        setupFragment()
    }

    override fun showGoToDateDialog() {
        requireActivity().setTheme(requireContext().getDatePickerDialogTheme())
        val view = DatePickerBinding.inflate(layoutInflater)
        //val datePicker = view.findViewById<DatePicker>(R.id.date_picker)

        val dateTime = getCurrentDate()!!
        view.datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { dialog, which -> dateSelected(dateTime, view.datePicker) }
            .apply {
                activity?.setupDialogStuff(view.root, this)
            }
    }

    private fun dateSelected(dateTime: DateTime, datePicker: DatePicker) {
        val isSundayFirst = requireContext().config.isSundayFirst
        val month = datePicker.month + 1
        val year = datePicker.year
        val day = datePicker.dayOfMonth
        var newDateTime = dateTime.withDate(year, month, day)

        if (isSundayFirst) {
            newDateTime = newDateTime.plusDays(1)
        }

        var selectedWeek = newDateTime.withDayOfWeek(1).withTimeAtStartOfDay().minusDays(if (isSundayFirst) 1 else 0)
        if (newDateTime.minusDays(7).seconds() > selectedWeek.seconds()) {
            selectedWeek = selectedWeek.plusDays(7)
        }

        currentWeekTS = selectedWeek.seconds()
        setupFragment()
    }

    private fun setupSeekbar() {
        if (context?.config?.allowCustomizeDayCount != true) {
            return
        }

        // avoid seekbar width changing if the days count changes to 1, 10 etc
        weekHolder!!.weekViewDaysCount.onGlobalLayout {
            if (weekHolder!!.weekViewSeekbar.width != 0) {
                weekHolder!!.weekViewSeekbar.layoutParams.width = weekHolder!!.weekViewSeekbar.width
            }
            (weekHolder!!.weekViewSeekbar.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.START_OF)
        }

        updateDaysCount(context?.config?.weeklyViewDays ?: 7)
    }

    private fun updateWeeklyViewDays(days: Int) {
        requireContext().config.weeklyViewDays = days
        updateDaysCount(days)
        setupWeeklyViewPager()
    }

    private fun updateDaysCount(cnt: Int) {
        weekHolder!!.weekViewDaysCount.text = requireContext().resources.getQuantityString(R.plurals.days, cnt, cnt)
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyWeekPagerAdapter)?.updateCalendars(viewPager!!.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentWeekTS != thisWeekTS

    override fun updateActionBarTitle() {
        setupWeeklyActionbarTitle(currentWeekTS)
    }

    override fun getNewEventDayCode(): String {
        val currentTS = System.currentTimeMillis() / 1000
        return if (currentTS > currentWeekTS && currentTS < currentWeekTS + WEEK_SECONDS) {
            Formatter.getTodayCode()
        } else {
            Formatter.getDayCodeFromTS(currentWeekTS)
        }
    }

    override fun scrollTo(y: Int) {
        weekHolder!!.weekViewHoursScrollview.scrollY = y
        weekScrollY = y
    }

    override fun updateHoursTopMargin(margin: Int) {
        weekHolder?.apply {
            weekViewHoursDivider.layoutParams?.height = margin
            weekViewHoursScrollview.requestLayout()
            weekViewHoursScrollview.onGlobalLayout {
                weekViewHoursScrollview.scrollY = weekScrollY
            }
        }
    }

    override fun getCurrScrollY() = weekScrollY

    override fun updateRowHeight(rowHeight: Int) {
        val childCnt = weekHolder!!.weekViewHoursHolder.childCount
        for (i in 0..childCnt) {
            val textView = weekHolder!!.weekViewHoursHolder.getChildAt(i) as? TextView ?: continue
            textView.layoutParams.height = rowHeight
        }

        weekHolder!!.weekViewHoursHolder.setPadding(0, 0, 0, rowHeight)
        (viewPager!!.adapter as? MyWeekPagerAdapter)?.updateNotVisibleScaleLevel(viewPager!!.currentItem)
    }

    override fun getFullFragmentHeight() =
        weekHolder!!.weekViewHolder.height - weekHolder!!.weekViewSeekbar.height - weekHolder!!.weekViewDaysCountDivider.divider.height

    override fun printView() {
        weekHolder!!.apply {
            weekViewDaysCountDivider.divider.beGone()
            weekViewSeekbar.beGone()
            weekViewDaysCount.beGone()
            addHours(resources.getColor(R.color.theme_light_text_color, null))
            root.background = ColorDrawable(Color.WHITE)
            (viewPager?.adapter as? MyWeekPagerAdapter)?.togglePrintMode(viewPager?.currentItem ?: 0)

            Handler(Looper.getMainLooper()).postDelayed({
                requireContext().printBitmap(weekHolder!!.weekViewHolder.getViewBitmap())

                Handler(Looper.getMainLooper()).postDelayed({
                    weekViewDaysCountDivider.divider.beVisible()
                    weekViewSeekbar.beVisible()
                    weekViewDaysCount.beVisible()
                    addHours()
                    root.background = ColorDrawable(requireContext().getProperBackgroundColor())
                    (viewPager?.adapter as? MyWeekPagerAdapter)?.togglePrintMode(viewPager?.currentItem ?: 0)
                }, 1000)
            }, 1000)
        }
    }

    override fun getCurrentDate(): DateTime? {
        return if (currentWeekTS != 0L) {
            Formatter.getUTCDateTimeFromTS(currentWeekTS)
        } else {
            null
        }
    }
}
