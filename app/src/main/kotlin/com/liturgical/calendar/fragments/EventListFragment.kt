package com.liturgical.calendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.MainActivity
import com.liturgical.calendar.activities.SimpleActivity
import com.liturgical.calendar.adapters.EventListAdapter
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.EVENTS_LIST_VIEW
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.models.Event
import com.liturgical.calendar.models.ListEvent
import com.liturgical.calendar.models.ListItem
import com.liturgical.calendar.models.ListSectionDay
import com.secure.commons.extensions.*
import com.secure.commons.helpers.MONTH_SECONDS
import com.secure.commons.interfaces.RefreshRecyclerViewListener
import com.secure.commons.views.MyLinearLayoutManager
import com.secure.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime

class EventListFragment : MyFragmentHolder(), RefreshRecyclerViewListener {
    private val NOT_UPDATING = 0
    private val UPDATE_TOP = 1
    private val UPDATE_BOTTOM = 2

    private var FETCH_INTERVAL = 3 * MONTH_SECONDS
    private var MIN_EVENTS_TRESHOLD = 30

    private var mEvents = ArrayList<Event>()
    private var minFetchedTS = 0L
    private var maxFetchedTS = 0L
    private var wereInitialEventsAdded = false
    private var hasBeenScrolled = false
    private var bottomItemAtRefresh: ListItem? = null

    private var use24HourFormat = false

    lateinit var mView: View

    override val viewType = EVENTS_LIST_VIEW

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_event_list, container, false)
        mView.background = ColorDrawable(requireContext().getProperBackgroundColor())
        mView.calendar_events_list_holder?.id = (System.currentTimeMillis() % 100000).toInt()
        mView.calendar_empty_list_placeholder_2.apply {
            setTextColor(context.getProperPrimaryColor())
            underlineText()
            setOnClickListener {
                activity?.hideKeyboard()
                context.launchNewEventIntent(getNewEventDayCode())
            }
        }

        use24HourFormat = requireContext().config.use24HourFormat
        updateActionBarTitle()
        return mView
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
        val use24Hour = requireContext().config.use24HourFormat
        if (use24Hour != use24HourFormat) {
            use24HourFormat = use24Hour
            (mView.calendar_events_list.adapter as? EventListAdapter)?.toggle24HourFormat(use24HourFormat)
        }
    }

    override fun onPause() {
        super.onPause()
        use24HourFormat = requireContext().config.use24HourFormat
    }

    private fun checkEvents() {
        if (!wereInitialEventsAdded) {
            minFetchedTS = DateTime().minusMinutes(requireContext().config.displayPastEvents).seconds()
            maxFetchedTS = DateTime().plusMonths(6).seconds()
        }

        requireContext().eventsHelper.getEvents(minFetchedTS, maxFetchedTS) {
            if (it.size >= MIN_EVENTS_TRESHOLD) {
                receivedEvents(it, NOT_UPDATING)
            } else {
                if (!wereInitialEventsAdded) {
                    maxFetchedTS += FETCH_INTERVAL
                }

                requireContext().eventsHelper.getEvents(minFetchedTS, maxFetchedTS) {
                    mEvents = it
                    receivedEvents(mEvents, NOT_UPDATING, !wereInitialEventsAdded)
                }
            }
            wereInitialEventsAdded = true
        }
    }

    private fun receivedEvents(events: ArrayList<Event>, updateStatus: Int, forceRecreation: Boolean = false) {
        if (context == null || activity == null) {
            return
        }

        mEvents = events
        val listItems = requireContext().getEventListItems(mEvents)

        activity?.runOnUiThread {
            if (activity == null) {
                return@runOnUiThread
            }

            val currAdapter = mView.calendar_events_list.adapter
            if (currAdapter == null || forceRecreation) {
                EventListAdapter(activity as SimpleActivity, listItems, true, this, mView.calendar_events_list) {
                    if (it is ListEvent) {
                        context?.editEvent(it)
                    }
                }.apply {
                    mView.calendar_events_list.adapter = this
                }

                if (requireContext().areSystemAnimationsEnabled) {
                    mView.calendar_events_list.scheduleLayoutAnimation()
                }

                mView.calendar_events_list.endlessScrollListener = object : MyRecyclerView.EndlessScrollListener {
                    override fun updateTop() {
                        fetchPreviousPeriod()
                    }

                    override fun updateBottom() {
                        fetchNextPeriod()
                    }
                }

                mView.calendar_events_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (!hasBeenScrolled) {
                            hasBeenScrolled = true
                            (activity as? MainActivity)?.refreshItems()
                            (activity as? MainActivity)?.refreshMenuItems()
                        }
                    }
                })
            } else {
                (currAdapter as EventListAdapter).updateListItems(listItems)
                if (updateStatus == UPDATE_TOP) {
                    val item = listItems.indexOfFirst { it == bottomItemAtRefresh }
                    if (item != -1) {
                        mView.calendar_events_list.scrollToPosition(item)
                    }
                } else if (updateStatus == UPDATE_BOTTOM) {
                    mView.calendar_events_list.smoothScrollBy(0, requireContext().resources.getDimension(R.dimen.endless_scroll_move_height).toInt())
                }
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        mView.calendar_empty_list_placeholder.beVisibleIf(mEvents.isEmpty())
        mView.calendar_empty_list_placeholder_2.beVisibleIf(mEvents.isEmpty())
        mView.calendar_events_list.beGoneIf(mEvents.isEmpty())
        if (activity != null)
            mView.calendar_empty_list_placeholder.setTextColor(requireActivity().getProperTextColor())
    }

    private fun fetchPreviousPeriod() {
        val lastPosition = (mView.calendar_events_list.layoutManager as MyLinearLayoutManager).findLastVisibleItemPosition()
        bottomItemAtRefresh = (mView.calendar_events_list.adapter as EventListAdapter).listItems[lastPosition]

        val oldMinFetchedTS = minFetchedTS - 1
        minFetchedTS -= FETCH_INTERVAL
        requireContext().eventsHelper.getEvents(minFetchedTS, oldMinFetchedTS) {
            it.forEach { event ->
                if (mEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                    mEvents.add(0, event)
                }
            }

            receivedEvents(mEvents, UPDATE_TOP)
        }
    }

    private fun fetchNextPeriod() {
        val oldMaxFetchedTS = maxFetchedTS + 1
        maxFetchedTS += FETCH_INTERVAL
        requireContext().eventsHelper.getEvents(oldMaxFetchedTS, maxFetchedTS) {
            it.forEach { event ->
                if (mEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                    mEvents.add(0, event)
                }
            }

            receivedEvents(mEvents, UPDATE_BOTTOM)
        }
    }

    override fun refreshItems() {
        checkEvents()
    }

    override fun goToToday() {
        val listItems = requireContext().getEventListItems(mEvents)
        val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            (mView.calendar_events_list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(firstNonPastSectionIndex, 0)
            mView.calendar_events_list.onGlobalLayout {
                hasBeenScrolled = false
                (activity as? MainActivity)?.refreshItems()
                (activity as? MainActivity)?.refreshMenuItems()
            }
        }
    }

    override fun showGoToDateDialog() {}

    override fun refreshEvents() {
        checkEvents()
    }

    override fun shouldGoToTodayBeVisible() = hasBeenScrolled

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateTitle(getString(R.string.app_launcher_name))
    }

    override fun getNewEventDayCode() = Formatter.getTodayCode()

    override fun printView() {
        mView.apply {
            if (calendar_events_list.isGone()) {
                context.toast(R.string.no_items_found)
                return@apply
            }

            (calendar_events_list.adapter as? EventListAdapter)?.togglePrintMode()
            Handler().postDelayed({
                requireContext().printBitmap(calendar_events_list.getViewBitmap())

                Handler().postDelayed({
                    (calendar_events_list.adapter as? EventListAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)
        }
    }

    override fun getCurrentDate() = null
}
