package com.liturgical.calendar.fragments

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Range
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.AllDayEventsHolderLineBinding
import com.liturgical.calendar.databinding.FragmentWeekBinding
import com.liturgical.calendar.databinding.WeekAllDayEventMarkerBinding
import com.liturgical.calendar.databinding.WeekEventMarkerBinding
import com.liturgical.calendar.databinding.WeekGridItemBinding
import com.liturgical.calendar.databinding.WeekNowMarkerBinding
import com.liturgical.calendar.databinding.WeeklyViewDayLetterBinding
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.interfaces.WeekFragmentListener
import com.liturgical.calendar.interfaces.WeeklyCalendar
import com.liturgical.calendar.models.Event
import com.liturgical.calendar.models.EventWeeklyView
import com.liturgical.calendar.views.MyScrollView
import com.secure.commons.dialogs.RadioGroupDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.*
import com.secure.commons.models.RadioItem
import org.joda.time.DateTime
import org.joda.time.Days
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeekFragment : Fragment(), WeeklyCalendar {
    private val WEEKLY_EVENT_ID_LABEL = "event_id_label"
    private val PLUS_FADEOUT_DELAY = 5000L
    private val MIN_SCALE_FACTOR = 0.3f
    private val MAX_SCALE_FACTOR = 5f
    private val MIN_SCALE_DIFFERENCE = 0.02f
    private val SCALE_RANGE = MAX_SCALE_FACTOR - MIN_SCALE_FACTOR

    var listener: WeekFragmentListener? = null
    private var weekTimestamp = 0L
    private var rowHeight = 0f
    private var todayColumnIndex = -1
    private var primaryColor = 0
    private var lastHash = 0
    private var prevScaleSpanY = 0f
    private var scaleCenterPercent = 0f
    private var defaultRowHeight = 0f
    private var screenHeight = 0
    private var rowHeightsAtScale = 0f
    private var prevScaleFactor = 0f
    private var mWasDestroyed = false
    private var isFragmentVisible = false
    private var wasFragmentInit = false
    private var wasExtraHeightAdded = false
    private var dimPastEvents = true
    private var dimCompletedTasks = true
    private var highlightWeekends = false
    private var wasScaled = false
    private var isPrintVersion = false
    private var selectedGrid: View? = null
    private var currentTimeView: ImageView? = null
    private var fadeOutHandler = Handler(Looper.getMainLooper())
    private var allDayHolders = ArrayList<RelativeLayout>()
    private var allDayRows = ArrayList<HashSet<Int>>()
    private var allDayEventToRow = LinkedHashMap<Event, Int>()
    private var currEvents = ArrayList<Event>()
    private var dayColumns = ArrayList<RelativeLayout>()
    private var eventTypeColors = LongSparseArray<Int>()
    private var eventTimeRanges = LinkedHashMap<String, LinkedHashMap<Long, EventWeeklyView>>()
    private var currentlyDraggedView: View? = null

    private lateinit var inflater: LayoutInflater
    private lateinit var mView: FragmentWeekBinding
    private lateinit var scrollView: MyScrollView
    private lateinit var res: Resources
    private lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        res = requireContext().resources
        config = requireContext().config
        rowHeight = requireContext().getWeeklyViewItemHeight()
        defaultRowHeight = res.getDimension(R.dimen.weekly_view_row_height)
        weekTimestamp = requireArguments().getLong(WEEK_START_TIMESTAMP)
        dimPastEvents = config.dimPastEvents
        dimCompletedTasks = config.dimCompletedTasks
        highlightWeekends = config.highlightWeekends
        primaryColor = requireContext().getProperPrimaryColor()
        allDayRows.add(HashSet())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.inflater = inflater

        val fullHeight = requireContext().getWeeklyViewItemHeight().toInt() * 24
        mView = FragmentWeekBinding.inflate(inflater, container, false).apply {
            scrollView = weekEventsScrollview
            weekHorizontalGridHolder.layoutParams.height = fullHeight
            weekEventsColumnsHolder.layoutParams.height = fullHeight

            val scaleDetector = getViewScaleDetector()
            scrollView.setOnTouchListener { view, motionEvent ->
                scaleDetector.onTouchEvent(motionEvent)
                if (motionEvent.action == MotionEvent.ACTION_UP && wasScaled) {
                    scrollView.isScrollable = true
                    wasScaled = false
                    true
                } else {
                    false
                }
            }
        }

        addDayColumns()
        scrollView.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                checkScrollLimits(y)
            }
        })

        scrollView.onGlobalLayout {
            if (fullHeight < scrollView.height) {
                scrollView.layoutParams.height = fullHeight - res.getDimension(R.dimen.one_dp).toInt()
            }

            val initialScrollY = (rowHeight * config.startWeeklyAt).toInt()
            updateScrollY(max(listener?.getCurrScrollY() ?: 0, initialScrollY))
        }

        wasFragmentInit = true
        return mView.root
    }

    override fun onResume() {
        super.onResume()
        requireContext().eventsHelper.getEventTypes(requireActivity(), false) {
            it.map { eventType ->
                eventTypeColors.put(eventType.id!!, eventType.color)
            }
        }

        setupDayLabels()
        updateCalendar()

        if (rowHeight != 0f && mView.root.width != 0) {
            addCurrentTimeIndicator()
        }
    }

    override fun onPause() {
        super.onPause()
        wasExtraHeightAdded = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mWasDestroyed = true
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible
        if (isFragmentVisible && wasFragmentInit) {
            listener?.updateHoursTopMargin(mView.weekTopHolder.height)
            checkScrollLimits(scrollView.scrollY)

            // fix some glitches like at swiping from a fully scaled out fragment with all-day events to an empty one
            val fullFragmentHeight = (listener?.getFullFragmentHeight() ?: 0) - mView.weekTopHolder.height
            if (scrollView.height < fullFragmentHeight) {
                config.weeklyViewItemHeightMultiplier = fullFragmentHeight / 24 / defaultRowHeight
                updateViewScale()
                listener?.updateRowHeight(rowHeight.toInt())
            }
        }
    }

    fun updateCalendar() {
        if (context != null) {
            WeeklyCalendarImpl(this, requireContext()).updateWeeklyCalendar(weekTimestamp)
        }
    }

    private fun addDayColumns() {
        mView.weekEventsColumnsHolder.removeAllViews()
        (0 until config.weeklyViewDays).forEach {
            val column = inflater.inflate(R.layout.weekly_view_day_column, mView.weekEventsColumnsHolder, false) as RelativeLayout
            column.tag = Formatter.getUTCDayCodeFromTS(weekTimestamp + it * DAY_SECONDS)
            mView.weekEventsColumnsHolder.addView(column)
            dayColumns.add(column)
        }
    }

    private fun setupDayLabels() {
        var curDay = Formatter.getUTCDateTimeFromTS(weekTimestamp)
        val todayCode = Formatter.getDayCodeFromDateTime(DateTime())
        val screenWidth = context?.usableScreenSize?.x ?: return
        val dayWidth = screenWidth / config.weeklyViewDays
        val useLongerDayLabels = dayWidth > res.getDimension(R.dimen.weekly_view_min_day_label)

        mView.weekLettersHolder.removeAllViews()
        for (i in 0 until config.weeklyViewDays) {
            val dayCode = Formatter.getDayCodeFromDateTime(curDay)
            val labelIDs = if (useLongerDayLabels) {
                R.array.week_days_short
            } else {
                R.array.week_day_letters
            }

            val dayLetters = res.getStringArray(labelIDs).toMutableList() as ArrayList<String>
            val dayLetter = dayLetters[curDay.dayOfWeek - 1]

            val textColor = if (isPrintVersion) {
                resources.getColor(R.color.theme_light_text_color)
            } else if (todayCode == dayCode) {
                primaryColor
            } else if (highlightWeekends && isWeekend(curDay.dayOfWeek, true)) {
                config.highlightWeekendsColor
            } else {
                requireContext().getProperTextColor()
            }

            val label = WeeklyViewDayLetterBinding.inflate(inflater, mView.weekLettersHolder, false) // as MyTextView
            label.root.text = "$dayLetter\n${curDay.dayOfMonth}"
            label.root.setTextColor(textColor)
            if (todayCode == dayCode) {
                todayColumnIndex = i
            }

            mView.weekLettersHolder.addView(label.root)
            curDay = curDay.plusDays(1)
        }
    }

    private fun checkScrollLimits(y: Int) {
        if (isFragmentVisible) {
            listener?.scrollTo(y)
        }
    }

    private fun initGrid() {
        (0 until config.weeklyViewDays).mapNotNull { dayColumns.getOrNull(it) }
            .forEachIndexed { index, layout ->
                layout.removeAllViews()
                val gestureDetector = getViewGestureDetector(layout, index)

                layout.setOnTouchListener { view, motionEvent ->
                    gestureDetector.onTouchEvent(motionEvent)
                    true
                }

                layout.setOnDragListener { view, dragEvent ->
                    when (dragEvent.action) {
                        DragEvent.ACTION_DRAG_STARTED -> dragEvent.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        DragEvent.ACTION_DRAG_ENTERED,
                        DragEvent.ACTION_DRAG_EXITED,
                        DragEvent.ACTION_DRAG_LOCATION,
                        DragEvent.ACTION_DRAG_ENDED -> true
                        DragEvent.ACTION_DROP -> {
                            try {
                                val eventId = dragEvent.clipData.getItemAt(0).text.toString().toLong()
                                val startHour = (dragEvent.y / rowHeight).toInt()
                                ensureBackgroundThread {
                                    val event = context?.eventsDB?.getEventOrTaskWithId(eventId)
                                    event?.let {
                                        val currentStartTime = Formatter.getDateTimeFromTS(it.startTS)
                                        val startTime = Formatter.getDateTimeFromTS(weekTimestamp + index * DAY_SECONDS)
                                            .withTime(
                                                startHour,
                                                currentStartTime.minuteOfHour,
                                                currentStartTime.secondOfMinute,
                                                currentStartTime.millisOfSecond
                                            ).seconds()
                                        val currentEventDuration = event.endTS - event.startTS
                                        val endTime = startTime + currentEventDuration
                                        context?.eventsHelper?.updateEvent(
                                            it.copy(
                                                startTS = startTime,
                                                endTS = endTime,
                                                flags = it.flags.removeBit(FLAG_ALL_DAY)
                                            ), updateAtCalDAV = true, showToasts = false
                                        ) {
                                            updateCalendar()
                                        }
                                    }
                                }
                                true
                            } catch (ignored: Exception) {
                                false
                            }
                        }
                        else -> false
                    }
                }
            }
    }

    private fun getViewGestureDetector(view: ViewGroup, index: Int): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                selectedGrid?.animation?.cancel()
                selectedGrid?.beGone()

                val hour = (event.y / rowHeight).toInt()
                selectedGrid = WeekGridItemBinding.inflate(inflater, null, false).root.apply {
                    view.addView(this)
                    background = ColorDrawable(primaryColor)
                    layoutParams.width = view.width
                    layoutParams.height = rowHeight.toInt()
                    y = hour * rowHeight
                    applyColorFilter(primaryColor.getContrastColor())

                    setOnClickListener {
                        val timestamp = Formatter.getDateTimeFromTS(weekTimestamp + index * DAY_SECONDS).withTime(hour, 0, 0, 0).seconds()
                        if (config.allowCreatingTasks) {
                            val items = arrayListOf(
                                RadioItem(TYPE_EVENT, getString(R.string.event)),
                                RadioItem(TYPE_TASK, getString(R.string.task))
                            )

                            RadioGroupDialog(activity!!, items) {
                                launchNewEventIntent(timestamp, it as Int == TYPE_TASK)
                            }
                        } else {
                            launchNewEventIntent(timestamp, false)
                        }
                    }

                    // do not use setStartDelay, it will trigger instantly if the device has disabled animations
                    fadeOutHandler.removeCallbacksAndMessages(null)
                    fadeOutHandler.postDelayed({
                        animate().alpha(0f).withEndAction {
                            beGone()
                        }
                    }, PLUS_FADEOUT_DELAY)
                }
                return super.onSingleTapUp(event)
            }
        })
    }

    private fun launchNewEventIntent(timestamp: Long, isTask: Boolean) {
        Intent(context, getActivityToOpen(isTask)).apply {
            putExtra(NEW_EVENT_START_TS, timestamp)
            putExtra(NEW_EVENT_SET_HOUR_DURATION, true)
            startActivity(this)
        }
    }

    private fun getViewScaleDetector(): ScaleGestureDetector {
        return ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val percent = (prevScaleSpanY - detector.currentSpanY) / screenHeight
                prevScaleSpanY = detector.currentSpanY

                val wantedFactor = config.weeklyViewItemHeightMultiplier - (SCALE_RANGE * percent)
                var newFactor = max(min(wantedFactor, MAX_SCALE_FACTOR), MIN_SCALE_FACTOR)
                if (scrollView.height > defaultRowHeight * newFactor * 24) {
                    newFactor = scrollView.height / 24f / defaultRowHeight
                }

                if (abs(newFactor - prevScaleFactor) > MIN_SCALE_DIFFERENCE) {
                    prevScaleFactor = newFactor
                    config.weeklyViewItemHeightMultiplier = newFactor
                    updateViewScale()
                    listener?.updateRowHeight(rowHeight.toInt())

                    val targetY = rowHeightsAtScale * rowHeight - scaleCenterPercent * getVisibleHeight()
                    scrollView.scrollTo(0, targetY.toInt())
                }
                return super.onScale(detector)
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                scaleCenterPercent = detector.focusY / scrollView.height
                rowHeightsAtScale = (scrollView.scrollY + scaleCenterPercent * getVisibleHeight()) / rowHeight
                scrollView.isScrollable = false
                prevScaleSpanY = detector.currentSpanY
                prevScaleFactor = config.weeklyViewItemHeightMultiplier
                wasScaled = true
                screenHeight = context!!.realScreenSize.y
                return super.onScaleBegin(detector)
            }
        })
    }

    private fun getVisibleHeight(): Float {
        val fullContentHeight = rowHeight * 24
        val visibleRatio = scrollView.height / fullContentHeight
        return fullContentHeight * visibleRatio
    }

    override fun updateWeeklyCalendar(events: ArrayList<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || mWasDestroyed || context == null) {
            return
        }

        lastHash = newHash

        requireActivity().runOnUiThread {
            if (context != null && activity != null && isAdded) {
                val replaceDescription = config.replaceDescription
                val sorted = events.sortedWith(
                    compareBy<Event> { it.startTS }.thenBy { it.endTS }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description }
                ).toMutableList() as ArrayList<Event>

                currEvents = sorted
                addEvents(sorted)
            }
        }
    }

    private fun updateViewScale() {
        rowHeight = context?.getWeeklyViewItemHeight() ?: return

        val oneDp = res.getDimension(R.dimen.one_dp).toInt()
        val fullHeight = max(rowHeight.toInt() * 24, scrollView.height + oneDp)
        scrollView.layoutParams.height = fullHeight - oneDp
        mView.weekHorizontalGridHolder.layoutParams.height = fullHeight
        mView.weekEventsColumnsHolder.layoutParams.height = fullHeight
        addEvents(currEvents)
    }

    private fun addEvents(events: ArrayList<Event>) {
        initGrid()
        allDayHolders.clear()
        allDayRows.clear()
        eventTimeRanges.clear()
        allDayRows.add(HashSet())
        mView.weekAllDayHolder.removeAllViews()
        addNewLine()
        allDayEventToRow.clear()

        val minuteHeight = rowHeight / 60
        val minimalHeight = res.getDimension(R.dimen.weekly_view_minimal_event_height).toInt()
        val density = res.displayMetrics.density.roundToInt()

        for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val startDayCode = Formatter.getDayCodeFromDateTime(startDateTime)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val endDayCode = Formatter.getDayCodeFromDateTime(endDateTime)

            if (event.getIsAllDay() || ((startDayCode != endDayCode) && config.showMidnightSpanningEventsAtTop)) {
                continue
            }

            var currentDateTime = startDateTime
            var currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
            do {
                val startMinutes = when (currentDayCode == startDayCode) {
                    true -> (startDateTime.minuteOfDay)
                    else -> 0
                }
                val duration = when (currentDayCode == endDayCode) {
                    true -> (endDateTime.minuteOfDay - startMinutes)
                    else -> 1440
                }

                var endMinutes = startMinutes + duration
                if ((endMinutes - startMinutes) * minuteHeight < minimalHeight) {
                    endMinutes = startMinutes + (minimalHeight / minuteHeight).toInt()
                }

                val range = Range(startMinutes, endMinutes)
                val eventWeekly = EventWeeklyView(range)

                if (!eventTimeRanges.containsKey(currentDayCode)) {
                    eventTimeRanges[currentDayCode] = LinkedHashMap()
                }
                eventTimeRanges[currentDayCode]?.put(event.id!!, eventWeekly)

                currentDateTime = currentDateTime.plusDays(1)
                currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
            } while (currentDayCode.toInt() <= endDayCode.toInt())
        }

        for ((_, eventDayList) in eventTimeRanges) {
            val eventsCollisionChecked = ArrayList<Long>()
            for ((eventId, eventWeeklyView) in eventDayList) {
                if (eventWeeklyView.slot == 0) {
                    eventWeeklyView.slot = 1
                    eventWeeklyView.slot_max = 1
                }

                eventsCollisionChecked.add(eventId)
                val eventWeeklyViewsToCheck = eventDayList.filterNot { eventsCollisionChecked.contains(it.key) }
                for ((toCheckId, eventWeeklyViewToCheck) in eventWeeklyViewsToCheck) {
                    val areTouching = eventWeeklyView.range.intersects(eventWeeklyViewToCheck.range)
                    val doHaveCommonMinutes = if (areTouching) {
                        eventWeeklyView.range.upper > eventWeeklyViewToCheck.range.lower || (eventWeeklyView.range.lower == eventWeeklyView.range.upper &&
                            eventWeeklyView.range.upper == eventWeeklyViewToCheck.range.lower)
                    } else {
                        false
                    }

                    if (areTouching && doHaveCommonMinutes) {
                        if (eventWeeklyViewToCheck.slot == 0) {
                            val nextSlot = eventWeeklyView.slot_max + 1
                            val slotRange = Array(eventWeeklyView.slot_max) { it + 1 }
                            val collisionEventWeeklyViews = eventDayList.filter { eventWeeklyView.collisions.contains(it.key) }
                            for ((_, collisionEventWeeklyView) in collisionEventWeeklyViews) {
                                if (collisionEventWeeklyView.range.intersects(eventWeeklyViewToCheck.range)) {
                                    slotRange[collisionEventWeeklyView.slot - 1] = nextSlot
                                }
                            }
                            slotRange[eventWeeklyView.slot - 1] = nextSlot
                            val slot = slotRange.minOrNull()
                            eventWeeklyViewToCheck.slot = slot!!
                            if (slot == nextSlot) {
                                eventWeeklyViewToCheck.slot_max = nextSlot
                                eventWeeklyView.slot_max = nextSlot
                                for ((_, collisionEventWeeklyView) in collisionEventWeeklyViews) {
                                    collisionEventWeeklyView.slot_max++
                                }
                            } else {
                                eventWeeklyViewToCheck.slot_max = eventWeeklyView.slot_max
                            }
                        }
                        eventWeeklyView.collisions.add(toCheckId)
                        eventWeeklyViewToCheck.collisions.add(eventId)
                    }
                }
            }
        }

        dayevents@ for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val startDayCode = Formatter.getDayCodeFromDateTime(startDateTime)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val endDayCode = Formatter.getDayCodeFromDateTime(endDateTime)
            if (event.getIsAllDay() || ((startDayCode != endDayCode) && config.showMidnightSpanningEventsAtTop)) {
                addAllDayEvent(event)
            } else {
                var currentDateTime = startDateTime
                var currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
                do {
                    val dayOfWeek = dayColumns.indexOfFirst { it.tag == currentDayCode }
                    if (dayOfWeek == -1 || dayOfWeek >= config.weeklyViewDays) {
                        continue@dayevents
                    }

                    val dayColumn = dayColumns[dayOfWeek]
                    WeekEventMarkerBinding.inflate(inflater, null, false).apply {
                        var backgroundColor = eventTypeColors.get(event.eventType, primaryColor)
                        var textColor = backgroundColor.getContrastColor()
                        val currentEventWeeklyView = eventTimeRanges[currentDayCode]!!.get(event.id)

                        val adjustAlpha = if (event.isTask()) {
                            dimCompletedTasks && event.isTaskCompleted()
                        } else {
                            dimPastEvents && event.isPastEvent && !isPrintVersion
                        }

                        if (adjustAlpha) {
                            backgroundColor = backgroundColor.adjustAlpha(MEDIUM_ALPHA)
                            textColor = textColor.adjustAlpha(HIGHER_ALPHA)
                        }

                        root.background = ColorDrawable(backgroundColor)
                        dayColumn.addView(this.root)
                        root.y = currentEventWeeklyView!!.range.lower * minuteHeight

                        weekEventTaskImage.beVisibleIf(event.isTask())
                        if (event.isTask()) {
                            weekEventTaskImage.applyColorFilter(textColor)
                        }

                        weekEventLabel.apply {
                            setTextColor(textColor)
                            maxLines = if (event.isTask() || event.startTS == event.endTS) {
                                1
                            } else {
                                3
                            }

                            text = event.title
                            checkViewStrikeThrough(event.isTaskCompleted())
                            contentDescription = text

                            minHeight = if (event.startTS == event.endTS) {
                                minimalHeight
                            } else {
                                ((currentEventWeeklyView.range.upper - currentEventWeeklyView.range.lower) * minuteHeight).toInt() - 1
                            }
                        }

                        (root.layoutParams as RelativeLayout.LayoutParams).apply {
                            width = (dayColumn.width - 1) / currentEventWeeklyView.slot_max
                            root.x = (width * (currentEventWeeklyView.slot - 1)).toFloat()
                            if (currentEventWeeklyView.slot > 1) {
                                root.x += density
                                width -= density
                            }
                        }

                        root.setOnClickListener {
                            Intent(context, getActivityToOpen(event.isTask())).apply {
                                putExtra(EVENT_ID, event.id!!)
                                putExtra(EVENT_OCCURRENCE_TS, event.startTS)
                                putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
                                startActivity(this)
                            }
                        }

                        root.setOnLongClickListener { view ->
                            currentlyDraggedView = view
                            val shadowBuilder = View.DragShadowBuilder(view)
                            val clipData = ClipData.newPlainText(WEEKLY_EVENT_ID_LABEL, event.id.toString())
                            if (isNougatPlus()) {
                                view.startDragAndDrop(clipData, shadowBuilder, null, 0)
                            } else {
                                view.startDrag(clipData, shadowBuilder, null, 0)
                            }
                            true
                        }

                        root.setOnDragListener(DragListener())
                    }

                    currentDateTime = currentDateTime.plusDays(1)
                    currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
                } while (currentDayCode.toInt() <= endDayCode.toInt())
            }
        }

        checkTopHolderHeight()
        addCurrentTimeIndicator()
    }

    private fun addNewLine() {
        val allDaysLine = AllDayEventsHolderLineBinding.inflate(inflater, null, false)
        mView.weekAllDayHolder.addView(allDaysLine.root)
        allDayHolders.add(allDaysLine.root)
    }

    private fun addCurrentTimeIndicator() {
        if (todayColumnIndex != -1) {
            val calendar = Calendar.getInstance()
            val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            if (todayColumnIndex >= dayColumns.size) {
                currentTimeView?.alpha = 0f
                return
            }

            if (currentTimeView != null) {
                mView.weekEventsHolder.removeView(currentTimeView)
            }

            if (isPrintVersion) {
                return
            }

            val weeklyViewDays = config.weeklyViewDays
            currentTimeView = WeekNowMarkerBinding.inflate(inflater, null, false).root.apply {
                applyColorFilter(primaryColor)
                mView.weekEventsHolder.addView(this, 0)
                val extraWidth = res.getDimension(R.dimen.activity_margin).toInt()
                val markerHeight = res.getDimension(R.dimen.weekly_view_now_height).toInt()
                val minuteHeight = rowHeight / 60
                (layoutParams as RelativeLayout.LayoutParams).apply {
                    width = (mView.root.width / weeklyViewDays) + extraWidth
                    height = markerHeight
                }

                x = if (weeklyViewDays == 1) {
                    0f
                } else {
                    (mView.root.width / weeklyViewDays * todayColumnIndex).toFloat() - extraWidth / 2f
                }

                y = minutes * minuteHeight - markerHeight / 2
            }
        }
    }

    private fun checkTopHolderHeight() {
        mView.weekTopHolder.onGlobalLayout {
            if (isFragmentVisible && activity != null && !mWasDestroyed) {
                listener?.updateHoursTopMargin(mView.weekTopHolder.height)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun addAllDayEvent(event: Event) {
        WeekAllDayEventMarkerBinding.inflate(inflater, null, false).apply {
            var backgroundColor = eventTypeColors.get(event.eventType, primaryColor)
            var textColor = backgroundColor.getContrastColor()

            val adjustAlpha = if (event.isTask()) {
                dimCompletedTasks && event.isTaskCompleted()
            } else {
                dimPastEvents && event.isPastEvent && !isPrintVersion
            }

            if (adjustAlpha) {
                backgroundColor = backgroundColor.adjustAlpha(LOWER_ALPHA)
                textColor = textColor.adjustAlpha(HIGHER_ALPHA)
            }

            root.background = ColorDrawable(backgroundColor)

            weekEventLabel.apply {
                setTextColor(textColor)
                maxLines = if (event.isTask()) 1 else 2
                text = event.title
                checkViewStrikeThrough(event.isTaskCompleted())
                contentDescription = text
            }

            weekEventTaskImage.beVisibleIf(event.isTask())
            if (event.isTask()) {
                weekEventTaskImage.applyColorFilter(textColor)
            }

            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val eventStartDayStartTime = startDateTime.withTimeAtStartOfDay().seconds()
            val eventEndDayStartTime = endDateTime.withTimeAtStartOfDay().seconds()

            val minTS = max(startDateTime.seconds(), weekTimestamp)
            val maxTS = min(endDateTime.seconds(), weekTimestamp + 2 * WEEK_SECONDS)

            // fix a visual glitch with all-day events or events lasting multiple days starting at midnight on monday, being shown the previous week too
            if (minTS == maxTS && (minTS - weekTimestamp == WEEK_SECONDS.toLong())) {
                return
            }

            val isStartTimeDay = Formatter.getDateTimeFromTS(maxTS) == Formatter.getDateTimeFromTS(maxTS).withTimeAtStartOfDay()
            val numDays = Days.daysBetween(Formatter.getDateTimeFromTS(minTS).toLocalDate(), Formatter.getDateTimeFromTS(maxTS).toLocalDate()).days
            val daysCnt = if (numDays == 1 && isStartTimeDay) 0 else numDays
            val startDateTimeInWeek = Formatter.getDateTimeFromTS(minTS)
            val firstDayIndex = startDateTimeInWeek.dayOfMonth // indices must be unique for the visible range (2 weeks)
            val lastDayIndex = firstDayIndex + daysCnt
            val dayIndices = firstDayIndex..lastDayIndex
            val isAllDayEvent = firstDayIndex == lastDayIndex
            val isRepeatingOverlappingEvent = eventEndDayStartTime - eventStartDayStartTime >= event.repeatInterval

            var doesEventFit: Boolean
            var wasEventHandled = false
            var drawAtLine = 0

            for (index in allDayRows.indices) {
                drawAtLine = index

                val row = allDayRows[index]
                doesEventFit = dayIndices.all { !row.contains(it) }

                val firstEvent = allDayEventToRow.keys.firstOrNull { it.id == event.id }
                val lastEvent = allDayEventToRow.keys.lastOrNull { it.id == event.id }
                val firstEventRowIdx = allDayEventToRow[firstEvent]
                val lastEventRowIdx = allDayEventToRow[lastEvent]
                val adjacentEvents = currEvents.filter { event.id == it.id }
                val repeatingEventIndex = adjacentEvents.indexOf(event)
                val isRowValidForEvent = lastEvent == null || firstEventRowIdx!! + repeatingEventIndex == index && lastEventRowIdx!! < index

                if (doesEventFit && (!isRepeatingOverlappingEvent || isAllDayEvent || isRowValidForEvent)) {
                    dayIndices.forEach {
                        row.add(it)
                    }
                    allDayEventToRow[event] = index
                    wasEventHandled = true
                } else {
                    // create new row
                    if (index == allDayRows.lastIndex) {
                        allDayRows.add(HashSet())
                        addNewLine()
                        drawAtLine++
                        val lastRow = allDayRows.last()
                        dayIndices.forEach {
                            lastRow.add(it)
                        }
                        allDayEventToRow[event] = allDayRows.lastIndex
                        wasEventHandled = true
                    }
                }

                if (wasEventHandled) {
                    break
                }
            }

            val dayCodeStart = Formatter.getDayCodeFromDateTime(startDateTime).toInt()
            val dayCodeEnd = Formatter.getDayCodeFromDateTime(endDateTime).toInt()
            val dayOfWeek = dayColumns.indexOfFirst { it.tag.toInt() == dayCodeStart || it.tag.toInt() in (dayCodeStart + 1)..dayCodeEnd }
            if (dayOfWeek == -1) {
                return
            }

            allDayHolders[drawAtLine].addView(this.root)
            val dayWidth = mView.root.width / config.weeklyViewDays
            (root.layoutParams as RelativeLayout.LayoutParams).apply {
                leftMargin = dayOfWeek * dayWidth
                bottomMargin = 1
                width = (dayWidth) * (daysCnt + 1)
            }

            calculateExtraHeight()

            root.setOnClickListener {
                Intent(context, getActivityToOpen(event.isTask())).apply {
                    putExtra(EVENT_ID, event.id)
                    putExtra(EVENT_OCCURRENCE_TS, event.startTS)
                    putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
                    startActivity(this)
                }
            }
        }
    }

    private fun calculateExtraHeight() {
        mView.weekTopHolder.onGlobalLayout {
            if (activity != null && !mWasDestroyed) {
                if (isFragmentVisible) {
                    listener?.updateHoursTopMargin(mView.weekTopHolder.height)
                }

                if (!wasExtraHeightAdded) {
                    wasExtraHeightAdded = true
                }
            }
        }
    }

    fun updateScrollY(y: Int) {
        if (wasFragmentInit) {
            scrollView.scrollY = y
        }
    }

    fun updateNotVisibleViewScaleLevel() {
        if (!isFragmentVisible) {
            updateViewScale()
        }
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        updateCalendar()
        setupDayLabels()
        addEvents(currEvents)
    }

    inner class DragListener : View.OnDragListener {
        override fun onDrag(view: View, dragEvent: DragEvent): Boolean {
            return when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> currentlyDraggedView == view
                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.beGone()
                    false
                }
                // handle ACTION_DRAG_LOCATION due to https://stackoverflow.com/a/19460338
                DragEvent.ACTION_DRAG_LOCATION -> true
                DragEvent.ACTION_DROP -> {
                    view.beVisible()
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!dragEvent.result) {
                        view.beVisible()
                    }
                    currentlyDraggedView = null
                    true
                }
                else -> false
            }
        }
    }
}
