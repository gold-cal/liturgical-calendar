package com.liturgical.calendar.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.liturgical.calendar.R
import com.liturgical.calendar.databinding.MonthViewBinding
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.helpers.COLUMN_COUNT
import com.liturgical.calendar.helpers.ROW_COUNT
import com.liturgical.calendar.models.DayMonthly
import com.secure.commons.extensions.onGlobalLayout

// used in the Monthly view fragment, 1 view per screen
class MonthViewWrapper(context: Context, attrs: AttributeSet, defStyle: Int) : FrameLayout(context, attrs, defStyle) {
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var weekDaysLetterHeight = 0
    private var horizontalOffset = 0
    private var wereViewsAdded = false
    private var isMonthDayView = true
    private var days = ArrayList<DayMonthly>()
    private var inflater: LayoutInflater
    private var monthView: MonthView
    private var dayClickCallback: ((day: DayMonthly) -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        val normalTextSize = resources.getDimensionPixelSize(R.dimen.normal_text_size).toFloat()
        weekDaysLetterHeight = 2 * normalTextSize.toInt()

        inflater = LayoutInflater.from(context)
        monthView = MonthViewBinding.inflate(inflater, this, true).monthView
        setupHorizontalOffset()

        onGlobalLayout {
            if (!wereViewsAdded && days.isNotEmpty()) {
                measureSizes()
                addClickableBackgrounds()
                monthView.updateDays(days, isMonthDayView)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        measureSizes()
        var y = 0
        var x = 0
        var curLeft = dayWidth.toInt()
        val end = right + paddingRight

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is MonthView) {
                //ignore the MonthView layout
                continue
            }

            child.measure(
                MeasureSpec.makeMeasureSpec(dayWidth.toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dayHeight.toInt(), MeasureSpec.EXACTLY)
            )

            val childLeft = x * dayWidth + horizontalOffset - child.translationX
            val childTop = y * dayHeight + weekDaysLetterHeight - child.translationY
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            val childRight = childLeft + childWidth
            val childBottom = childTop + childHeight

            child.layout(childLeft.toInt(), childTop.toInt(), childRight.toInt(), childBottom.toInt())

            if (curLeft + childWidth < end) {
                curLeft += childWidth
                x++
            } else {
                y++
                x = 0
                curLeft = childWidth
            }
        }
    }

    fun updateDays(newDays: ArrayList<DayMonthly>, addEvents: Boolean, callback: ((DayMonthly) -> Unit)? = null) {
        setupHorizontalOffset()
        measureSizes()
        dayClickCallback = callback
        days = newDays
        if (dayWidth != 0f && dayHeight != 0f) {
            addClickableBackgrounds()
        }

        isMonthDayView = !addEvents
        monthView.updateDays(days, isMonthDayView)
    }

    private fun setupHorizontalOffset() {
        horizontalOffset = if (context.config.showWeekNumbers) resources.getDimensionPixelSize(R.dimen.smaller_text_size) * 2 else 0
    }

    private fun measureSizes() {
        dayWidth = (width - horizontalOffset) / 7f
        dayHeight = (height - weekDaysLetterHeight) / 6f
    }

    private fun addClickableBackgrounds() {
        removeAllViews()
        monthView = MonthViewBinding.inflate(inflater, this, true).monthView
        wereViewsAdded = true
        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0 until COLUMN_COUNT) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    addViewBackground(x, y, day)
                }
                curId++
            }
        }
    }

    private fun addViewBackground(viewX: Int, viewY: Int, day: DayMonthly) {
        val xPos = viewX * dayWidth + horizontalOffset
        val yPos = viewY * dayHeight + weekDaysLetterHeight

        inflater.inflate(R.layout.month_view_background, this, false).apply {
            if (isMonthDayView) {
                background = null
            }

            layoutParams.width = dayWidth.toInt()
            layoutParams.height = dayHeight.toInt()
            x = xPos
            y = yPos
            setOnClickListener {
                dayClickCallback?.invoke(day)

                if (isMonthDayView) {
                    monthView.updateCurrentlySelectedDay(viewX, viewY)
                }
            }
            addView(this)
        }
    }

    fun togglePrintMode() {
        monthView.togglePrintMode()
    }
}
