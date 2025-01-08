package com.liturgical.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.adapters.DayEventsAdapter
import com.liturgical.calendar.databinding.FragmentDayBinding
import com.liturgical.calendar.databinding.TopNavigationBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.extensions.getViewBitmap
import com.liturgical.calendar.extensions.printBitmap
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.interfaces.NavigationListener
import com.liturgical.calendar.models.Event
import com.secure.commons.extensions.*

class DayFragment : Fragment() {
    var mListener: NavigationListener? = null
    private var mTextColor = 0
    private var mDayCode = ""
    private var lastHash = 0

    private lateinit var mHolder: FragmentDayBinding
    private lateinit var mTopNav: TopNavigationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mHolder = FragmentDayBinding.inflate(inflater, container,false)
        mTopNav = TopNavigationBinding.bind(mHolder.root)

        mDayCode = requireArguments().getString(DAY_CODE)!!
        setupButtons()
        return mHolder.root
    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        mTopNav.topLeftArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        mTopNav.topRightArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        val day = Formatter.getDayTitle(requireContext(), mDayCode)
        mTopNav.topValue.apply {
            text = day
            contentDescription = text
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
            setTextColor(context.getProperTextColor())
        }
    }

    fun updateCalendar() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        context?.eventsHelper?.getEvents(startTS, endTS) {
            receivedEvents(it)
        }
    }

    private fun receivedEvents(events: List<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || !isAdded) {
            return
        }
        lastHash = newHash

        val replaceDescription = requireContext().config.replaceDescription
        val sorted = ArrayList(events.sortedWith(compareBy({ !it.getIsAllDay() }, { it.startTS }, { it.endTS }, { it.title }, {
            if (replaceDescription) it.location else it.description
        })))

        activity?.runOnUiThread {
            updateEvents(sorted)
        }
    }

    private fun updateEvents(events: ArrayList<Event>) {
        if (activity == null)
            return

        DayEventsAdapter(activity as SimpleActivity, events, mHolder.dayEvents, mDayCode) {
            editEvent(it as Event)
        }.apply {
            mHolder.dayEvents.adapter = this
        }

        if (requireContext().areSystemAnimationsEnabled) {
            mHolder.dayEvents.scheduleLayoutAnimation()
        }
    }

    private fun editEvent(event: Event) {
        Intent(context, getActivityToOpen(event.isTask())).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
            startActivity(this)
        }
    }

    fun printCurrentView() {
        mTopNav.apply {
            topLeftArrow.beGone()
            topRightArrow.beGone()
            topValue.setTextColor(resources.getColor(R.color.theme_light_text_color))
            (mHolder.dayEvents.adapter as? DayEventsAdapter)?.togglePrintMode()

            Handler(Looper.getMainLooper()).postDelayed({
                requireContext().printBitmap(mHolder.dayHolder.getViewBitmap())

                Handler(Looper.getMainLooper()).postDelayed({
                    topLeftArrow.beVisible()
                    topRightArrow.beVisible()
                    topValue.setTextColor(requireContext().getProperTextColor())
                    (mHolder.dayEvents.adapter as? DayEventsAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)
        }
    }
}
