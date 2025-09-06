package com.liturgical.calendar.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.databinding.FragmentMonthBinding
import com.liturgical.calendar.databinding.TopNavigationBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.getViewBitmap
import com.liturgical.calendar.extensions.printBitmap
import com.liturgical.calendar.helpers.Config
import com.liturgical.calendar.helpers.DAY_CODE
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.helpers.MonthlyCalendarImpl
import com.liturgical.calendar.interfaces.MonthlyCalendar
import com.liturgical.calendar.interfaces.NavigationListener
import com.liturgical.calendar.models.DayMonthly
import com.secure.commons.extensions.applyColorFilter
import com.secure.commons.extensions.beGone
import com.secure.commons.extensions.beVisible
import com.secure.commons.extensions.getProperTextColor
import org.joda.time.DateTime

class MonthFragment : Fragment(), MonthlyCalendar {
    private var mTextColor = 0
    private var mSundayFirst = false
    private var mShowWeekNumbers = false
    private var mDayCode = ""
    private var mPackageName = ""
    private var mLastHash = 0L
    private var mCalendar: MonthlyCalendarImpl? = null

    var listener: NavigationListener? = null

    lateinit var mRes: Resources
    private lateinit var mHolder: FragmentMonthBinding
    private lateinit var mTopNav: TopNavigationBinding
    lateinit var mConfig: Config

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mHolder = FragmentMonthBinding.inflate(inflater, container, false)
        mTopNav = TopNavigationBinding.bind(mHolder.root)
        mRes = resources
        mPackageName = requireActivity().packageName
        mDayCode = requireArguments().getString(DAY_CODE)!!
        mConfig = requireContext().config
        storeStateVariables()

        setupButtons()
        mCalendar = MonthlyCalendarImpl(this, requireContext())

        return mHolder.root
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.showWeekNumbers != mShowWeekNumbers) {
            mLastHash = -1L
        }

        mCalendar!!.apply {
            mTargetDate = Formatter.getDateTimeFromCode(mDayCode)
            getDays(false)    // prefill the screen asap, even if without events
        }

        storeStateVariables()
        updateCalendar()
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mSundayFirst = isSundayFirst
            mShowWeekNumbers = showWeekNumbers
        }
    }

    fun updateCalendar() {
        mCalendar?.updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
    }

    override fun updateMonthlyCalendar(context: Context, month: String, days: ArrayList<DayMonthly>, checkedEvents: Boolean, currTargetDate: DateTime) {
        val newHash = month.hashCode() + days.hashCode().toLong()
        if ((mLastHash != 0L && !checkedEvents) || mLastHash == newHash) {
            return
        }

        mLastHash = newHash

        activity?.runOnUiThread {
            mTopNav.topValue.apply {
                text = month
                contentDescription = text

                if (activity != null) {
                    setTextColor(requireActivity().getProperTextColor())
                }
            }
            updateDays(days)
        }
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        mTopNav.topLeftArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                listener?.goLeft()
            }

            val pointerLeft = AppCompatResources.getDrawable(context, R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        mTopNav.topRightArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                listener?.goRight()
            }

            val pointerRight = AppCompatResources.getDrawable(context, R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        mTopNav.topValue.apply {
            setTextColor(requireContext().getProperTextColor())
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
        }
    }

    private fun updateDays(days: ArrayList<DayMonthly>) {
        mHolder.monthViewWrapper.updateDays(days, true) {
            (activity as MainActivity).openDayFromMonthly(Formatter.getDateTimeFromCode(it.code))
        }
    }

    fun printCurrentView() {
        mHolder.apply {
            mTopNav.topLeftArrow.beGone()
            mTopNav.topRightArrow.beGone()
            mTopNav.topValue.setTextColor(resources.getColor(R.color.theme_light_text_color, null))
            monthViewWrapper.togglePrintMode()

            requireContext().printBitmap(monthCalendarHolder.getViewBitmap())

            mTopNav.topLeftArrow.beVisible()
            mTopNav.topRightArrow.beVisible()
            mTopNav.topValue.setTextColor(requireContext().getProperTextColor())
            monthViewWrapper.togglePrintMode()
        }
    }
}
