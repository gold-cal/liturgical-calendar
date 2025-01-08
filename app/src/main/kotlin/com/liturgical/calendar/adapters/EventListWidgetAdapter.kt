package com.liturgical.calendar.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Paint
//import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.liturgical.calendar.R
import com.liturgical.calendar.R.id.event_item_holder
import com.liturgical.calendar.R.id.event_section_background
import com.liturgical.calendar.R.id.event_section_title
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.extensions.eventsHelper
import com.liturgical.calendar.extensions.getWidgetFontSize
import com.liturgical.calendar.extensions.seconds
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.models.*
import com.secure.commons.extensions.*
import com.secure.commons.helpers.MEDIUM_ALPHA
import org.joda.time.DateTime

class EventListWidgetAdapter(val context: Context, val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val ITEM_EVENT = 0
    private val ITEM_SECTION_DAY = 1
    private val ITEM_SECTION_MONTH = 2
    //private val GO_TO_TODAY = "go_to_today"

    //private val allDayString = context.resources.getString(R.string.all_day)
    private var events = ArrayList<ListItem>()
    private var textColor = context.config.widgetTextColor
    private var dayBackgroundColor = context.config.widgetDayColor
    private var weakTextColor = textColor.adjustAlpha(MEDIUM_ALPHA)
    private var displayDescription = context.config.displayDescription
    private var replaceDescription = context.config.replaceDescription
    private var dimPastEvents = context.config.dimPastEvents
    private var dimCompletedTasks = context.config.dimCompletedTasks
    private var showDescription = context.config.showWidgetDescription
    private var showBirthAnnDes = context.config.showBirthdayAnniversaryDescription
    private var fontSize = context.getWidgetFontSize()
    //private var mediumMargin = context.resources.getDimension(R.dimen.medium_margin).toInt()
    private var tinyMargin = context.resources.getDimension(R.dimen.tiny_margin).toInt()

    init {
        initConfigValues()
    }

    private fun initConfigValues() {
        textColor = context.config.widgetTextColor
        dayBackgroundColor = context.config.widgetDayColor
        weakTextColor = textColor.adjustAlpha(MEDIUM_ALPHA)
        displayDescription = context.config.displayDescription
        replaceDescription = context.config.replaceDescription
        dimPastEvents = context.config.dimPastEvents
        dimCompletedTasks = context.config.dimCompletedTasks
        fontSize = context.getWidgetFontSize()
        showDescription = context.config.showWidgetDescription
        showBirthAnnDes = context.config.showBirthdayAnniversaryDescription
    }

    override fun getViewAt(position: Int): RemoteViews {
        context.config.viewPosition = position
        val type = getItemViewType(position)
        val remoteView: RemoteViews

        if (type == ITEM_EVENT) {
            val event = events[position] as ListEvent
            val layout = R.layout.event_list_item_widget
            remoteView = RemoteViews(context.packageName, layout)
            setupListEvent(remoteView, event)
        } else if (type == ITEM_SECTION_DAY) {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_day_widget)
            val section = events.getOrNull(position) as? ListSectionDay
            if (section != null) {
                if (section.isToday) {
                    context.config.listWidgetDayPosition = position
                }
                setupListSectionDay(remoteView, section)
            }
        } else {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_month_widget)
            val section = events.getOrNull(position) as? ListSectionMonth
            if (section != null) {
                setupListSectionMonth(remoteView, section)
            }
        }

        return remoteView
    }

    private fun setupListEvent(remoteView: RemoteViews, item: ListEvent) {
        var curTextColor = textColor

        remoteView.apply {
            remoteView.applyColorFilter(R.id.event_item_color_bar, item.color)
            setText(R.id.event_item_title, item.title)

            var showItemTime = false
            var timeText = if (item.isAllDay) "" else Formatter.getTimeFromTS(context, item.startTS)
            val endText = Formatter.getTimeFromTS(context, item.endTS)
            if (item.startTS != item.endTS) {
                if (!item.isAllDay) {
                    timeText += " - $endText"
                }

                val startCode = Formatter.getDayCodeFromTS(item.startTS)
                val endCode = Formatter.getDayCodeFromTS(item.endTS)
                if (startCode != endCode) {
                    timeText += " (${Formatter.getDateDayTitle(endCode)})"
                }
            }
            showItemTime = when {
                (showBirthAnnDes && item.isSpecialEvent) -> true
                (showDescription && item.isAllDay) -> true
                else -> {false}
            }

            setText(R.id.event_item_time, timeText)

            setVisibleIf(R.id.event_item_time, !item.isAllDay || showItemTime)


            // we cannot change the event_item_color_bar rules dynamically, so do it like this
            val descriptionText = if (replaceDescription) item.location else item.description.replace("\n", " ")
            if (descriptionText.isNotEmpty()) {
                if (showItemTime) {
                    setText(R.id.event_item_time, descriptionText)
                } else {
                    setText(R.id.event_item_time, "$timeText\n$descriptionText")
                }
            } else {
                setVisibleIf(R.id.event_item_time, !item.isAllDay)
            }

            if (item.isTask && item.isTaskCompleted && dimCompletedTasks || dimPastEvents && item.isPastEvent) {
                curTextColor = weakTextColor
            }

            setTextColor(R.id.event_item_title, curTextColor)
            setTextColor(R.id.event_item_time, curTextColor)

            setTextSize(R.id.event_item_title, fontSize)
            setTextSize(R.id.event_item_time, fontSize)
            setTextSize(R.id.task_image_mask, fontSize)

            setVisibleIf(R.id.event_item_task_image, item.isTask)
            applyColorFilter(R.id.event_item_task_image, curTextColor)

            if (item.isTask) {
                setViewPadding(R.id.event_item_title, 0, 0, 0, 0)
            } else {
                setViewPadding(R.id.event_item_title, tinyMargin, 0, 0, 0)
            }

            if (item.isTaskCompleted) {
                setInt(R.id.event_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG)
            } else {
                setInt(R.id.event_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            }

            Intent().apply {
                putExtra(IS_TASK, item.isTask)
                putExtra(EVENT_ID, item.id)
                putExtra(EVENT_OCCURRENCE_TS, item.startTS)
                setOnClickFillInIntent(event_item_holder, this)
            }
        }
    }

    private fun setupListSectionDay(remoteView: RemoteViews, item: ListSectionDay) {
        var curTextColor = textColor
        if (dimPastEvents && item.isPastSection) {
            curTextColor = weakTextColor
        }

        remoteView.apply {
            setTextColor(event_section_title, curTextColor)
            setTextSize(event_section_title, fontSize - 3f)
            setText(event_section_title, item.title)

            applyColorFilter(event_section_background, dayBackgroundColor)

            Intent().apply {
                putExtra(DAY_CODE, item.code)
                putExtra(VIEW_TO_OPEN, DAILY_VIEW) //context.config.listWidgetViewToOpen)
                setOnClickFillInIntent(event_section_title, this)
            }
        }
    }

    private fun setupListSectionMonth(remoteView: RemoteViews, item: ListSectionMonth) {
        val curTextColor = textColor
        remoteView.apply {
            setTextColor(event_section_title, curTextColor)
            setTextSize(event_section_title, fontSize)
            setText(event_section_title, item.title)
        }
    }

    private fun getItemViewType(position: Int) = when {
        events.getOrNull(position) is ListEvent -> ITEM_EVENT
        events.getOrNull(position) is ListSectionDay -> ITEM_SECTION_DAY
        else -> ITEM_SECTION_MONTH
    }

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 3

    override fun onCreate() {}

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
        initConfigValues()
        val period = intent.getIntExtra(EVENT_LIST_PERIOD, 0)
        val currentDate = DateTime()
        val fromTS = currentDate.seconds() - context.config.displayPastEvents * 60
        val toTS = when (period) {
            0 -> currentDate.plusYears(1).seconds()
            EVENT_PERIOD_TODAY -> currentDate.withTime(23, 59, 59, 999).seconds()
            else -> currentDate.plusSeconds(period).seconds()
        }
        context.eventsHelper.getEventsSync(fromTS, toTS, applyTypeFilter = true) {
            val listItems = ArrayList<ListItem>(it.size)
            val replaceDescription = context.config.replaceDescription
            val sorted = it.sortedWith(compareBy<Event> { event ->
                if (event.getIsAllDay()) {
                    Formatter.getDayStartTS(Formatter.getDayCodeFromTS(event.startTS)) - 1
                } else {
                    event.startTS
                }
            }.thenBy { event ->
                if (event.getIsAllDay()) {
                    Formatter.getDayEndTS(Formatter.getDayCodeFromTS(event.endTS))
                } else {
                    event.endTS
                }
            }.thenBy { event -> event.title }.thenBy { event -> if (replaceDescription) event.location else event.description })

            var prevCode = ""
            var prevMonthLabel = ""
            val now = getNowSeconds()
            val today = Formatter.getDateDayTitle(Formatter.getDayCodeFromTS(now))
            var isSpecialEvent: Boolean

            sorted.forEach { event ->
                val code = Formatter.getDayCodeFromTS(event.startTS)
                val monthLabel = Formatter.getLongMonthYear(context, code)
                isSpecialEvent = event.source == SOURCE_CONTACT_BIRTHDAY || event.source == SOURCE_CONTACT_ANNIVERSARY

                if (monthLabel != prevMonthLabel) {
                    val listSectionMonth = ListSectionMonth(monthLabel)
                    listItems.add(listSectionMonth)
                    prevMonthLabel = monthLabel
                }

                if (code != prevCode) {
                    val day = Formatter.getDateDayTitle(code)
                    val isToday = day == today
                    val isPastSection = if (isToday) {
                        context.config.currentScrollPosition = listItems.size
                        false
                    } else {
                        event.startTS < now
                    }
                    val listSection = ListSectionDay(day, code, dayBackgroundColor, isToday, isPastSection)
                    listItems.add(listSection)
                    prevCode = code
                }

                val listEvent = ListEvent(
                    event.id!!,
                    event.startTS,
                    event.endTS,
                    event.title,
                    event.description,
                    event.getIsAllDay(),
                    event.color,
                    event.location,
                    event.isPastEvent,
                    event.repeatInterval > 0,
                    event.isTask(),
                    event.isTaskCompleted(),
                    isSpecialEvent
                )
                listItems.add(listEvent)
            }

            this@EventListWidgetAdapter.events = listItems
        }
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {}
}
