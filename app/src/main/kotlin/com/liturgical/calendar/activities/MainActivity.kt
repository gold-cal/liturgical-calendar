package com.liturgical.calendar.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuItemCompat
import com.liturgical.calendar.BuildConfig
import com.liturgical.calendar.R
import com.liturgical.calendar.adapters.EventListAdapter
import com.liturgical.calendar.adapters.QuickFilterEventTypeAdapter
import com.liturgical.calendar.databases.EventsDatabase
import com.liturgical.calendar.databinding.ActivityMainBinding
import com.liturgical.calendar.dialogs.ExportEventsDialog
import com.liturgical.calendar.dialogs.FilterEventTypesDialog
import com.liturgical.calendar.dialogs.ImportEventsDialog
import com.liturgical.calendar.dialogs.SetRemindersDialog
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.fragments.*
import com.liturgical.calendar.helpers.*
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.helpers.IcsExporter.ExportResult
import com.liturgical.calendar.helpers.IcsImporter.ImportResult
import com.liturgical.calendar.jobs.CalDAVUpdateListener
import com.liturgical.calendar.models.CheckEvent
import com.liturgical.calendar.models.Event
import com.liturgical.calendar.models.ListEvent
import com.secure.commons.dialogs.FilePickerDialog
import com.secure.commons.dialogs.RadioGroupDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.*
import com.secure.commons.interfaces.RefreshRecyclerViewListener
import com.secure.commons.models.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private val PICK_IMPORT_SOURCE_INTENT = 1
    private val PICK_EXPORT_FILE_INTENT = 2

    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mIsSearchOpen = false
    private var mLatestSearchQuery = ""
    private var mSearchMenuItem: MenuItem? = null
    private var shouldGoToTodayBeVisible = false
    private var goToTodayButton: MenuItem? = null
    private var currentFragments = ArrayList<MyFragmentHolder>()
    private var eventTypesToExport = ArrayList<Long>()

    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredIsSundayFirst = false
    private var mStoredMidnightSpan = true
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true
    private var mStoredDimCompletedTasks = true
    private var mStoredHighlightWeekends = false
    private var mStoredStartWeekWithCurrentDay = false
    private var mStoredHighlightWeekendsColor = 0

    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))

        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        //checkWhatsNewDialog()
        binding.calendarFab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
        binding.calendarFab.setOnClickListener {
            if (config.allowCreatingTasks) {
                if (binding.fabExtendedOverlay.isVisible()) {
                    openNewEvent()
                } else {
                    showExtendedFab()
                }
            } else {
                openNewEvent()
            }
        }
        binding.fabEventLabel.setOnClickListener { openNewEvent() }
        binding.fabTaskLabel.setOnClickListener { openNewTask() }

        binding.fabExtendedOverlay.setOnClickListener {
            hideExtendedFab()
        }

        binding.fabTaskIcon.setOnClickListener {
            openNewTask()

            /*Handler().postDelayed({
                hideExtendedFab()
            }, 300)*/
        }

        storeStateVariables()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshCalDAVCalendars(false)
        }

        checkIsViewIntent()

        if (!checkIsOpenIntent()) {
            updateViewPager()
        }

        checkAppOnSDCard()

        if (savedInstanceState == null) {
            checkCalDAVUpdateListener()
        }

        addBirthdaysAnniversariesAtStart()
        // Add all the liturgical calendar stuff
        addLiturgicalCalendar()
        // Add the birthday and anniversary calendar types
        createEventTypes()
        // Check if there are old calendar items to delete
        checkDeleteOldEvents()

    }

    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != getProperTextColor() || mStoredBackgroundColor != getProperBackgroundColor() || mStoredPrimaryColor != getProperPrimaryColor()
            || mStoredDayCode != Formatter.getTodayCode() || mStoredDimPastEvents != config.dimPastEvents || mStoredDimCompletedTasks != config.dimCompletedTasks
            || mStoredHighlightWeekends != config.highlightWeekends || mStoredHighlightWeekendsColor != config.highlightWeekendsColor
        ) {
            updateViewPager()
        }

        eventsHelper.getEventTypes(this, false) {
            val newShouldFilterBeVisible = it.size > 1 || config.displayEventTypes.isEmpty()
            if (newShouldFilterBeVisible != mShouldFilterBeVisible) {
                mShouldFilterBeVisible = newShouldFilterBeVisible
                refreshMenuItems()
            }
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredIsSundayFirst != config.isSundayFirst || mStoredUse24HourFormat != config.use24HourFormat
                || mStoredMidnightSpan != config.showMidnightSpanningEventsAtTop || mStoredStartWeekWithCurrentDay != config.startWeekWithCurrentDay
            ) {
                updateViewPager()
            }
        }

        storeStateVariables()
        updateWidgets()
        updateTextColors(binding.calendarCoordinator)
        binding.fabExtendedOverlay.background = ColorDrawable(getProperBackgroundColor().adjustAlpha(0.8f))
        binding.fabEventLabel.setTextColor(getProperTextColor())
        binding.fabTaskLabel.setTextColor(getProperTextColor())

        binding.fabTaskIcon.drawable.applyColorFilter(mStoredPrimaryColor.getContrastColor())
        binding.fabTaskIcon.background.applyColorFilter(mStoredPrimaryColor)

        binding.searchHolder.background = ColorDrawable(getProperBackgroundColor())
        checkSwipeRefreshAvailability()
        checkShortcuts()

        setupToolbar(binding.mainToolbar, searchMenuItem = mSearchMenuItem)
        if (!mIsSearchOpen) {
            refreshMenuItems()
        }

        setupQuickFilter()

        binding.mainToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if (config.caldavSync) {
            updateCalDAVEvents()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            EventsDatabase.destroyInstance()
            stopCalDAVUpdateListener()
        }
    }

    fun refreshMenuItems() {
        if (binding.fabExtendedOverlay.isVisible()) {
            hideExtendedFab()
        }

        shouldGoToTodayBeVisible = currentFragments.lastOrNull()?.shouldGoToTodayBeVisible() ?: false
        binding.mainToolbar.menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.print).isVisible = config.storedView != MONTHLY_DAILY_VIEW
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible && !mIsSearchOpen
            findItem(R.id.go_to_date).isVisible = config.storedView != EVENTS_LIST_VIEW
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
        }
    }

    private fun setupOptionsMenu() {
        setupSearch(binding.mainToolbar.menu)
        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            if (binding.fabExtendedOverlay.isVisible()) {
                hideExtendedFab()
            }

            when (menuItem.itemId) {
                R.id.change_view -> showViewDialog()
                R.id.go_to_today -> goToToday()
                R.id.go_to_date -> showGoToDateDialog()
                R.id.print -> printView()
                R.id.filter -> showFilterDialog()
                R.id.refresh_caldav_calendars -> refreshCalDAVCalendars(true)
                R.id.add_holidays -> addHolidays()
                R.id.add_birthdays -> tryAddBirthdays()
                R.id.add_anniversaries -> tryAddAnniversaries()
                // R.id.add_custom_events -> tryAddCustomEvents()
                R.id.import_events -> tryImportEvents()
                R.id.export_events -> tryExportEvents()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mIsSearchOpen) {
            closeSearch()
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
            checkSwipeRefreshAvailability()
            when {
                binding.fabExtendedOverlay.isVisible() -> hideExtendedFab()
                currentFragments.size > 1 -> removeTopFragment()
                else -> super.onBackPressed()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIsOpenIntent()
        checkIsViewIntent()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportEventsFromFile(resultData.data!!)
        } else if (requestCode == PICK_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportEventsTo(eventTypesToExport, outputStream)
        }
    }

    private fun storeStateVariables() {
        mStoredTextColor = getProperTextColor()
        mStoredPrimaryColor = getProperPrimaryColor()
        mStoredBackgroundColor = getProperBackgroundColor()
        config.apply {
            mStoredIsSundayFirst = isSundayFirst
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
            mStoredDimCompletedTasks = dimCompletedTasks
            mStoredHighlightWeekends = highlightWeekends
            mStoredHighlightWeekendsColor = highlightWeekendsColor
            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
            mStoredStartWeekWithCurrentDay = startWeekWithCurrentDay
        }
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                mIsSearchOpen = true
                binding.searchHolder.beVisible()
                binding.calendarFab.beGone()
                searchQueryChanged("")
                refreshMenuItems()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                mIsSearchOpen = false
                binding.searchHolder.beGone()
                binding.calendarFab.beVisibleIf(currentFragments.last() !is YearFragmentsHolder && currentFragments.last() !is WeekFragmentsHolder)
                refreshMenuItems()
                return true
            }
        })
    }

    private fun setupQuickFilter() {
        eventsHelper.getEventTypes(this, false) {
            val quickFilterEventTypes = config.quickFilterEventTypes
            binding.quickEventTypeFilter.adapter = QuickFilterEventTypeAdapter(this, it, quickFilterEventTypes) {
                refreshViewPager()
                updateWidgets()
            }
        }
    }

    private fun closeSearch() {
        mSearchMenuItem?.collapseActionView()
    }

    private fun checkCalDAVUpdateListener() {
        if (isNougatPlus()) {
            val updateListener = CalDAVUpdateListener()
            if (config.caldavSync) {
                if (!updateListener.isScheduled(applicationContext)) {
                    updateListener.scheduleJob(applicationContext)
                }
            } else {
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    private fun stopCalDAVUpdateListener() {
        if (isNougatPlus()) {
            if (!config.caldavSync) {
                val updateListener = CalDAVUpdateListener()
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        //val appIconColor = config.appIconColor
        if (isNougatMR1Plus()) { // && config.lastHandledShortcutColor != appIconColor) {
            val newEvent = getNewEventShortcut() //appIconColor)
            val shortcuts = arrayListOf(newEvent)

            if (config.allowCreatingTasks) {
                shortcuts.add(getNewTaskShortcut()) //appIconColor))
            }

            try {
                shortcutManager.dynamicShortcuts = shortcuts
                //config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getNewEventShortcut(): ShortcutInfo { // appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_event)
        val newEventDrawable = resources.getDrawable(R.drawable.shortcut_event, theme)
        //(newEventDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_event_background).applyColorFilter(appIconColor)
        val newEventBitmap = newEventDrawable.convertToBitmap()

        val newEventIntent = Intent(this, SplashActivity::class.java)
        newEventIntent.action = SHORTCUT_NEW_EVENT
        return ShortcutInfo.Builder(this, "new_event")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(newEventBitmap))
            .setIntent(newEventIntent)
            .build()
    }

    @SuppressLint("NewApi")
    private fun getNewTaskShortcut(): ShortcutInfo { // appIconColor: Int): ShortcutInfo {
        val newTask = getString(R.string.new_task)
        val newTaskDrawable = ResourcesCompat.getDrawable(resources, R.drawable.shortcut_task, null)
            //resources.getDrawable(R.drawable.shortcut_task, theme)
        //(newTaskDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_task_background).applyColorFilter(appIconColor)
        val newTaskBitmap = newTaskDrawable?.convertToBitmap()
        val newTaskIntent = Intent(this, SplashActivity::class.java)
        newTaskIntent.action = SHORTCUT_NEW_TASK
        return ShortcutInfo.Builder(this, "new_task")
            .setShortLabel(newTask)
            .setLongLabel(newTask)
            .setIcon(Icon.createWithBitmap(newTaskBitmap))
            .setIntent(newTaskIntent)
            .build()
    }

    private fun checkIsOpenIntent(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val viewToOpen = intent.getIntExtra(VIEW_TO_OPEN, DAILY_VIEW)
        intent.removeExtra(VIEW_TO_OPEN)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            binding.calendarFab.beVisible()
            if (viewToOpen != LAST_VIEW) {
                config.storedView = viewToOpen
            }
            updateViewPager(dayCodeToOpen)
            return true
        }

        val isTask = intent.getBooleanExtra(IS_TASK, false)
        val eventIdToOpen = intent.getLongExtra(EVENT_ID, 0L)
        val eventOccurrenceToOpen = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
        intent.removeExtra(IS_TASK)
        intent.removeExtra(EVENT_ID)
        intent.removeExtra(EVENT_OCCURRENCE_TS)
        // check to see if it is a task
        // if it is a task, open the task activity
        if (isTask && eventIdToOpen != 0L && eventOccurrenceToOpen != 0L) {
            hideKeyboard()
            Intent(this, TaskActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }

        } else {
            if (eventIdToOpen != 0L && eventOccurrenceToOpen != 0L) {
                hideKeyboard()
                Intent(this, EventActivity::class.java).apply {
                    putExtra(EVENT_ID, eventIdToOpen)
                    putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                    startActivity(this)
                }
            }
        }

        return false
    }

    private fun checkIsViewIntent() {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri?.authority?.equals("com.android.calendar") == true || uri?.authority?.substringAfter("@") == "com.android.calendar") {
                if (uri.path!!.startsWith("/events")) {
                    ensureBackgroundThread {
                        // intents like content://com.android.calendar/events/1756
                        val eventId = uri.lastPathSegment
                        val id = eventsDB.getEventIdWithLastImportId("%-$eventId")
                        if (id != null) {
                            hideKeyboard()
                            Intent(this, EventActivity::class.java).apply {
                                putExtra(EVENT_ID, id)
                                startActivity(this)
                            }
                        } else {
                            toast(R.string.caldav_event_not_found, Toast.LENGTH_LONG)
                        }
                    }
                } else if (uri.path!!.startsWith("/time") || intent?.extras?.getBoolean("DETAIL_VIEW", false) == true) {
                    // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                    // or content://0@com.android.calendar/time/1584958526435
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt(timestamp.toLong())
                        return
                    }
                }
            } else {
                tryImportEventsFromFile(uri!!)
            }
        }
    }

    private fun showViewDialog() {
        val items = arrayListOf(
            RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
            RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
            RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
            RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
            RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
            RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list))
        )

        RadioGroupDialog(this, items, config.storedView) {
            resetActionBarTitle()
            closeSearch()
            updateView(it as Int)
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun goToToday() {
        currentFragments.last().goToToday()
    }

    fun showGoToDateDialog() {
        currentFragments.last().showGoToDateDialog()
    }

    private fun printView() {
        currentFragments.last().printView()
    }

    private fun resetActionBarTitle() {
        binding.mainToolbar.title = getString(R.string.app_launcher_name)
        binding.mainToolbar.subtitle = ""
    }

    fun updateTitle(text: String) {
        binding.mainToolbar.title = text
    }

    fun updateSubtitle(text: String) {
        binding.mainToolbar.subtitle = text
    }

    private fun showFilterDialog() {
        FilterEventTypesDialog(this) {
            refreshViewPager()
            setupQuickFilter()
            updateWidgets()
        }
    }

    fun toggleGoToTodayVisibility(beVisible: Boolean) {
        shouldGoToTodayBeVisible = beVisible
        if (goToTodayButton?.isVisible != beVisible) {
            refreshMenuItems()
        }
    }

    private fun updateCalDAVEvents() {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(showToasts = false, scheduleNextSync = true) {
                refreshViewPager()
            }
        }
    }

    private fun refreshCalDAVCalendars(showRefreshToast: Boolean) {
        showCalDAVRefreshToast = showRefreshToast
        if (showRefreshToast) {
            toast(R.string.refreshing)
        }
        updateCalDAVEvents()
        syncCalDAVCalendars {
            calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                calDAVChanged()
            }
        }
    }

    private fun calDAVChanged() {
        refreshViewPager()
        if (showCalDAVRefreshToast) {
            toast(R.string.refreshing_complete)
        }
        runOnUiThread {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun addHolidays() {
        val items = getHolidayRadioItems()
        RadioGroupDialog(this, items) { selectedHoliday ->
            SetRemindersDialog(this, OTHER_EVENT) {
                val reminders = it
                toast(R.string.importing)
                ensureBackgroundThread {
                    val holidays = getString(R.string.holidays)
                    var eventTypeId = eventsHelper.getEventTypeIdWithClass(HOLIDAY_EVENT)
                    if (eventTypeId == -1L) {
                        eventTypeId = eventsHelper.createPredefinedEventType(holidays, R.color.default_holidays_color, HOLIDAY_EVENT, true)
                    }
                    val result = IcsImporter(this).importEvents(false, selectedHoliday as String, eventTypeId, 0, false, reminders)
                    handleParseResult(result)
                    if (result != ImportResult.IMPORT_FAIL) {
                        runOnUiThread {
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }
        }
    }

    private fun tryAddBirthdays() {
        handlePermission(PERMISSION_READ_CONTACTS) { isTrue ->
            if (isTrue) {
                SetRemindersDialog(this, BIRTHDAY_EVENT) { birthdayReminders ->
                    val privateCursor = getMyContactsCursor(false, false)
                    ensureBackgroundThread {
                        val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
                        addPrivateEvents(true, privateContacts, birthdayReminders) { eventsFound, eventsAdded ->
                            addContactEvents(true, birthdayReminders, eventsFound, eventsAdded) {
                                when {
                                    it > 0 -> {
                                        toast(R.string.birthdays_added)
                                        updateViewPager()
                                        setupQuickFilter()
                                    }

                                    it == -1 -> toast(R.string.no_new_birthdays)
                                    else -> toast(R.string.no_birthdays)
                                }
                            }
                        }
                    }
                }
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }

    private fun tryAddAnniversaries() {
        handlePermission(PERMISSION_READ_CONTACTS) { isTrue ->
            if (isTrue) {
                SetRemindersDialog(this, ANNIVERSARY_EVENT) { anniversaryReminders ->
                    val privateCursor = getMyContactsCursor(false, false)

                    ensureBackgroundThread {
                        val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
                        addPrivateEvents(false, privateContacts, anniversaryReminders) { eventsFound, eventsAdded ->
                            addContactEvents(false, anniversaryReminders, eventsFound, eventsAdded) {
                                when {
                                    it > 0 -> {
                                        toast(R.string.anniversaries_added)
                                        updateViewPager()
                                        setupQuickFilter()
                                    }

                                    it == -1 -> toast(R.string.no_new_anniversaries)
                                    else -> toast(R.string.no_anniversaries)
                                }
                            }
                        }
                    }
                }
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }

    /*private fun tryAddCustomEvents() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                SetRemindersDialog(this, CONTACT_EVENT) { setReminder ->
                    val privateCursor = getMyContactsCursor(false, false)

                    ensureBackgroundThread {
                        val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
                        adPrivateEvents(true, privateContacts, setReminder) { eventsFound, eventsAdded ->
                            addContactEvents(false, true, setReminder, eventsFound, eventsAdded) { contactsAdded ->
                                when {
                                    contactsAdded > 0 -> {
                                        toast(R.string.custom_events_added)
                                        updateViewPager()
                                        setupQuickFilter()
                                    }

                                    contactsAdded == -1 -> toast(R.string.no_new_custom_events)
                                    else -> toast(R.string.no_custom_contact_events)
                                }
                            }
                        }
                    }
                }
            } else {
                toast(R.string.no_contacts_permission)
            }
        }
    }*/

    private fun addBirthdaysAnniversariesAtStart() {
        if ((!config.addBirthdaysAutomatically && !config.addAnniversariesAutomatically && !config.addCustomEventsAutomatically) || !hasPermission(PERMISSION_READ_CONTACTS)) {
            return
        }

        val privateCursor = getMyContactsCursor(false, false)

        ensureBackgroundThread {
            val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
            if (config.addBirthdaysAutomatically) {
                addPrivateEvents(true, privateContacts, config.birthdayReminders) { eventsFound, eventsAdded ->
                    addContactEvents(true, config.birthdayReminders, eventsFound, eventsAdded) {
                        if (it > 0) {
                            toast(R.string.birthdays_added)
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }

            if (config.addAnniversariesAutomatically) {
                addPrivateEvents(false, privateContacts, config.anniversaryReminders) { eventsFound, eventsAdded ->
                    addContactEvents(false, config.anniversaryReminders, eventsFound, eventsAdded) {
                        if (it > 0) {
                            toast(R.string.anniversaries_added)
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                }
            }
            // TODO: make it support foss contact app
            /*if (config.addCustomEventsAutomatically) {
                //adPrivateEvents(false, privateContacts, config.customEventReminders) { eventsFound, eventsAdded ->
                    addContactEvents(false, true, config.customEventReminders, 0, 0) { // eventsFound, eventsAdded) {
                        if (it > 0) {
                            toast(R.string.anniversaries_added)
                            updateViewPager()
                            setupQuickFilter()
                        }
                    }
                //}
            }*/
        }
    }

    private fun handleParseResult(result: ImportResult) {
        toast(
            when (result) {
                ImportResult.IMPORT_NOTHING_NEW -> R.string.no_new_items
                ImportResult.IMPORT_OK -> R.string.holidays_imported_successfully
                ImportResult.IMPORT_PARTIAL -> R.string.importing_some_holidays_failed
                else -> R.string.importing_holidays_failed
            }, Toast.LENGTH_LONG
        )
    }

    private fun doesEventExist(checkEvents: ArrayList<CheckEvent>, importId: String, contactName: String,
                               timestamp: Long, source: String): Boolean {
        var doesEventExist = false
        // check to make sure locally added events don't get duplicated
        for (checkEvent in checkEvents) {
            val currDate = Formatter.getDateFromTS(checkEvent.startTS)
            val newDate = Formatter.getDateFromTS(timestamp)
            // has to be on the same day
            if (checkEvent.importId == importId) {
                if (checkEvent.startTS != timestamp) {
                    val deleted = eventsDB.deleteBirthdayAnniversary(source, importId)
                    if (deleted == 1) {
                        doesEventExist = false
                        break
                    }
                } else {
                    doesEventExist = true
                    break
                }
            }
            // if the date and name are the same, don't update it
            // locally added events take precedence
            else if (currDate == newDate) {
                if (checkEvent.title == contactName) {
                    doesEventExist = true
                    break
                }
                // if name is not the same check if last name is the same
                else {
                    val currEventNameSplit = checkEvent.title.split(' ')
                    val newEventNameSplit = contactName.split(' ')
                    val currNameLength = currEventNameSplit.size
                    val newNameLength = newEventNameSplit.size
                    if (currNameLength > 1 && newNameLength > 1) {
                        val currName = currEventNameSplit[currNameLength - 1]
                        val newName = newEventNameSplit[newNameLength - 1]
                        if (currName == newName) {
                            doesEventExist = false
                            break
                            // mark it to check for duplicate
                        }
                    }
                    // current name has no last name, so check if first name matches
                    else {
                        val currName = currEventNameSplit[0]
                        val newName = newEventNameSplit[0]
                        doesEventExist = currName == newName
                        break
                    }
                }
            }
        }
        return doesEventExist
    }

    private fun addContactEvents(birthdays: Boolean, reminders: ArrayList<Int>, initEventsFound: Int, initEventsAdded: Int, callback: (Int) -> Unit) {
        var eventsFound = initEventsFound
        var eventsAdded = initEventsAdded
        val uri = Data.CONTENT_URI
        val projection = arrayOf(
            Contacts.DISPLAY_NAME,
            CommonDataKinds.Event.CONTACT_ID,
            CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP,
            CommonDataKinds.Event.START_DATE
        )

        val selection = "${Data.MIMETYPE} = ? AND ${CommonDataKinds.Event.TYPE} = ?"
        val type = if (birthdays) CommonDataKinds.Event.TYPE_BIRTHDAY else CommonDataKinds.Event.TYPE_ANNIVERSARY
        //val type = if (birthdays) CommonDataKinds.Event.TYPE_BIRTHDAY else if (custom) CommonDataKinds.Event.TYPE_CUSTOM else CommonDataKinds.Event.TYPE_ANNIVERSARY
        val selectionArgs = arrayOf(CommonDataKinds.Event.CONTENT_ITEM_TYPE, type.toString())

        val dateFormats = getDateFormats()
        val yearDateFormats = getDateFormatsWithYear()
        val existingEvents = if (birthdays) eventsDB.getBirthdays() else eventsDB.getAnniversaries()
        //val existingEvents = if (birthdays) eventsDB.getBirthdays() else if (custom) eventsDB.getCustom() else eventsDB.getAnniversaries()
        val checkEvents = ArrayList<CheckEvent>()
        //val importIDs = HashMap<String, Long>()
        existingEvents.forEach {
            val event = CheckEvent(it.title, it.startTS, it.importId, it.lastUpdated)
            checkEvents.add(event)
            //importIDs[it.importId] = it.startTS
        }

        val eventTypeId = if (birthdays) eventsHelper.getBirthdaysEventTypeId() else eventsHelper.getAnniversariesEventTypeId()
        //val eventTypeId = if (birthdays) eventsHelper.getLocalBirthdaysEventTypeId() else if (custom) else eventsHelper.getAnniversariesEventTypeId()
        val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY
        //val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else if (custom) SOURCE_CONTACT_CUSTOM else SOURCE_CONTACT_ANNIVERSARY

        queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
            val contactId = cursor.getIntValue(CommonDataKinds.Event.CONTACT_ID).toString()
            val name = cursor.getStringValue(Contacts.DISPLAY_NAME)
            val startDate = cursor.getStringValue(CommonDataKinds.Event.START_DATE)

            for (format in dateFormats) {
                try {
                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    //val newDateTime = Formatter.getDateTimeFromCode()
                    val date = formatter.parse(startDate)
                    val flags = if (format in yearDateFormats) {
                        FLAG_ALL_DAY
                    } else {
                        FLAG_ALL_DAY or FLAG_MISSING_YEAR
                    }

                    val timestamp = date.time / 1000L
                    val lastUpdated = cursor.getLongValue(CommonDataKinds.Event.CONTACT_LAST_UPDATED_TIMESTAMP)
                    val event = Event(
                        null, timestamp, timestamp, name, reminder1Minutes = reminders[0], reminder2Minutes = reminders[1],
                        reminder3Minutes = reminders[2], importId = contactId, timeZone = DateTimeZone.getDefault().id, flags = flags,
                        repeatInterval = YEAR, repeatRule = REPEAT_SAME_DAY, eventType = eventTypeId, source = source, lastUpdated = lastUpdated
                    )

                    val doesEventExist = doesEventExist(checkEvents, event.importId, name, timestamp, source)
                    //val importIDsToDelete = ArrayList<String>()
                    /*for ((key, value) in importIDs) {
                        if (key == contactId && value != timestamp) {
                            val deleted = eventsDB.deleteBirthdayAnniversary(source, key)
                            if (deleted == 1) {
                                importIDsToDelete.add(key)
                            }
                        }
                    }

                    importIDsToDelete.forEach {
                        importIDs.remove(it)
                    }*/

                    eventsFound++
                    if (!doesEventExist) { // !importIDs.containsKey(contactId)) {
                        eventsHelper.insertEvent(event, false, false) {
                            eventsAdded++
                        }
                    }
                    break
                } catch (ignore: Exception) {
                }
            }
        }

        runOnUiThread {
            callback(if (eventsAdded == 0 && eventsFound > 0) -1 else eventsAdded)
        }
    }

    private fun addPrivateEvents(
        birthdays: Boolean,
        contacts: ArrayList<SimpleContact>,
        reminders: ArrayList<Int>,
        callback: (eventsFound: Int, eventsAdded: Int) -> Unit
    ) {
        var eventsAdded = 0
        var eventsFound = 0
        if (contacts.isEmpty()) {
            callback(0, 0)
            return
        }

        try {
            val eventTypeId = if (birthdays) eventsHelper.getBirthdaysEventTypeId() else eventsHelper.getAnniversariesEventTypeId()
            val source = if (birthdays) SOURCE_CONTACT_BIRTHDAY else SOURCE_CONTACT_ANNIVERSARY

            val existingEvents = if (birthdays) eventsDB.getBirthdays() else eventsDB.getAnniversaries()
            val checkEvents = ArrayList<CheckEvent>()
            //val importIDs = HashMap<String, Long>()
            existingEvents.forEach {
                val event = CheckEvent(it.title, it.startTS, it.importId, it.lastUpdated)
                checkEvents.add(event)
                //importIDs[it.importId] = it.startTS
            }

            contacts.forEach { contact ->
                val events = if (birthdays) contact.birthdays else contact.anniversaries
                //var description = ""
                //if (config.showBirthdayAnniversaryDescription) {
                //    description = if (birthdays) getString(R.string.birthday)
                //    else getString(R.string.anniversary)
                //}
                events.forEach { birthdayAnniversary ->
                    // private contacts are created in Simple Contacts Pro, so we can guarantee that they exist only in these 2 formats
                    val format = if (birthdayAnniversary.startsWith("--")) {
                        "--MM-dd"
                    } else {
                        "yyyy-MM-dd"
                    }

                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    val date = formatter.parse(birthdayAnniversary)
                    if (date.year < 70) {
                        date.year = 70
                    }

                    val timestamp = date.time / 1000L
                    val lastUpdated = System.currentTimeMillis()
                    val event = Event(
                        null, timestamp, timestamp, contact.name, reminder1Minutes = reminders[0], reminder2Minutes = reminders[1],
                        reminder3Minutes = reminders[2], importId = contact.contactId.toString(), timeZone = DateTimeZone.getDefault().id, flags = FLAG_ALL_DAY,
                        repeatInterval = YEAR, repeatRule = REPEAT_SAME_DAY, eventType = eventTypeId, source = source, lastUpdated = lastUpdated
                    )

                    val doesEventExist = doesEventExist(checkEvents, event.importId, contact.name, timestamp, source)

                    /*
                    val importIDsToDelete = ArrayList<String>()
                    for ((key, value) in importIDs) {
                        if (key == contact.contactId.toString() && value != timestamp) {
                            val deleted = eventsDB.deleteBirthdayAnniversary(source, key)
                            if (deleted == 1) {
                                importIDsToDelete.add(key)
                            }
                        }
                    }

                    importIDsToDelete.forEach {
                        importIDs.remove(it)
                    }*/

                    eventsFound++
                    if (!doesEventExist) { //     !importIDs.containsKey(contact.contactId.toString())) {
                        eventsHelper.insertEvent(event, false, false) {
                            eventsAdded++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        callback(eventsFound, eventsAdded)
    }

    private fun updateView(view: Int) {
        binding.calendarFab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        val dateCode = getDateCodeToDisplay(view)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager(dateCode)
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun getDateCodeToDisplay(newView: Int): String? {
        val fragment = currentFragments.last()
        val currentView = fragment.viewType
        if (newView == EVENTS_LIST_VIEW || currentView == EVENTS_LIST_VIEW) {
            return null
        }

        val fragmentDate = fragment.getCurrentDate()
        val viewOrder = arrayListOf(DAILY_VIEW, WEEKLY_VIEW, MONTHLY_VIEW, YEARLY_VIEW)
        val currentViewIndex = viewOrder.indexOf(if (currentView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else currentView)
        val newViewIndex = viewOrder.indexOf(if (newView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else newView)

        return if (fragmentDate != null && currentViewIndex <= newViewIndex) {
            getDateCodeFormatForView(newView, fragmentDate)
        } else {
            getDateCodeFormatForView(newView, DateTime())
        }
    }

    private fun getDateCodeFormatForView(view: Int, date: DateTime): String {
        return when (view) {
            WEEKLY_VIEW -> getDatesWeekDateTime(date)
            YEARLY_VIEW -> date.toString()
            else -> Formatter.getDayCodeFromDateTime(date)
        }
    }

    private fun updateViewPager(dayCode: String? = null) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            try {
                supportFragmentManager.beginTransaction().remove(it).commitNow()
            } catch (ignored: Exception) {
                return
            }
        }

        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()
        val fixedDayCode = fixDayCode(dayCode)

        when (config.storedView) {
            DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            WEEKLY_VIEW -> bundle.putString(WEEK_START_DATE_TIME, fixedDayCode ?: getDatesWeekDateTime(DateTime()))
            MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            YEARLY_VIEW -> bundle.putString(YEAR_TO_OPEN, fixedDayCode)
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        binding.mainToolbar.navigationIcon = null
    }

    private fun fixDayCode(dayCode: String? = null): String? = when {
        config.storedView == WEEKLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> getDatesWeekDateTime(Formatter.getDateTimeFromCode(dayCode))
        config.storedView == YEARLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> Formatter.getYearFromDayCode(dayCode)
        else -> dayCode
    }

    private fun showExtendedFab() {
        animateFabIcon(false)
        arrayOf(binding.fabEventLabel, binding.fabExtendedOverlay, binding.fabTaskIcon, binding.fabTaskLabel).forEach {
            it.fadeIn()
        }
    }

    private fun hideExtendedFab() {
        animateFabIcon(true)
        arrayOf(binding.fabEventLabel, binding.fabExtendedOverlay, binding.fabTaskIcon, binding.fabTaskLabel).forEach {
            it.fadeOut()
        }
    }

    private fun animateFabIcon(showPlus: Boolean) {
        val newDrawableId = if (showPlus) {
            R.drawable.ic_plus_vector
        } else {
            R.drawable.ic_today_vector
        }
        val newDrawable = resources.getColoredDrawableWithColor(newDrawableId, getProperPrimaryColor())
        binding.calendarFab.setImageDrawable(newDrawable)
    }

    /** Combined the 2 functions below this one into one **/
    private fun openNewEventOrTask(isTask: Boolean) {
        hideKeyboard()
        hideExtendedFab()
        val lastFragment = currentFragments.last()
        val allowChangingDay = lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
        if (isTask) {
            launchNewTaskIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
        } else {
            launchNewEventIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
        }
    }

    private fun openNewEvent() = openNewEventOrTask(false)
    private fun openNewTask() = openNewEventOrTask(true)

    fun openMonthFromYearly(dateTime: DateTime) {
        if (currentFragments.last() is MonthFragmentsHolder) {
            return
        }

        val fragment = MonthFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        resetActionBarTitle()
        binding.calendarFab.beVisible()
        showBackNavigationArrow()
    }

    fun openDayFromMonthly(dateTime: DateTime) {
        if (currentFragments.last() is DayFragmentsHolder) {
            return
        }

        val fragment = DayFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        try {
            supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
            showBackNavigationArrow()
        } catch (ignore: Exception) {
        }
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> MonthFragmentsHolder()
        MONTHLY_DAILY_VIEW -> MonthDayFragmentsHolder()
        YEARLY_VIEW -> YearFragmentsHolder()
        EVENTS_LIST_VIEW -> EventListFragment()
        else -> WeekFragmentsHolder()
    }

    private fun removeTopFragment() {
        supportFragmentManager.beginTransaction().remove(currentFragments.last()).commit()
        currentFragments.removeAt(currentFragments.size - 1)
        toggleGoToTodayVisibility(currentFragments.last().shouldGoToTodayBeVisible())
        currentFragments.last().apply {
            refreshEvents()
            updateActionBarTitle()
        }
        binding.calendarFab.beGoneIf(currentFragments.size == 1 && config.storedView == YEARLY_VIEW)
        if (currentFragments.size > 1) {
            showBackNavigationArrow()
        } else {
            binding.mainToolbar.navigationIcon = null
        }
    }

    private fun showBackNavigationArrow() {
        binding.mainToolbar.navigationIcon = resources.getColoredDrawableWithColor(R.drawable.ic_arrow_left_vector, getProperBackgroundColor().getContrastColor())
    }

    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun tryImportEvents() {
        if (isQPlus()) {
            handleNotificationPermission { granted ->
                if (granted) {
                    hideKeyboard()
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/calendar"

                        try {
                            startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            showErrorToast(e)
                        }
                    }
                } else {
                    toast(R.string.no_post_notifications_permissions)
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    importEvents()
                }
            }
        }
    }

    private fun importEvents() {
        FilePickerDialog(this) {
            showImportEventsDialog(it)
        }
    }

    private fun tryImportEventsFromFile(uri: Uri) {
        when (uri.scheme) {
            "file" -> showImportEventsDialog(uri.path!!)
            "content" -> {
                val tempFile = getTempFile()
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    showImportEventsDialog(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun showImportEventsDialog(path: String) {
        ImportEventsDialog(this, path) {
            if (it) {
                runOnUiThread {
                    updateViewPager()
                    setupQuickFilter()
                }
            }
        }
    }

    private fun tryExportEvents() {
        if (isQPlus()) {
            ExportEventsDialog(this, config.lastExportPath, true) { file, eventTypes ->
                eventTypesToExport = eventTypes
                hideKeyboard()

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/calendar"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    try {
                        startActivityForResult(this, PICK_EXPORT_FILE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) { hasPermission ->
                if (hasPermission) {
                    ExportEventsDialog(this, config.lastExportPath, false) { file, eventTypes ->
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            exportEventsTo(eventTypes, it)
                        }
                    }
                }
            }
        }
    }

    private fun exportEventsTo(eventTypes: ArrayList<Long>, outputStream: OutputStream?) {
        ensureBackgroundThread {
            val events = eventsHelper.getEventsToExport(eventTypes)
            if (events.isEmpty()) {
                toast(R.string.no_entries_for_exporting)
            } else {
                IcsExporter().exportEvents(this, outputStream, events, true) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> R.string.exporting_successful
                            ExportResult.EXPORT_PARTIAL -> R.string.exporting_some_entries_failed
                            else -> R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_JODA
        val forkedFromUrl = "https://github.com/SimpleMobileTools/Simple-Calendar"
        val sourceCodeUrl = "https://github.com/gold-cal/liturgical-calendar"

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_2_title, R.string.faq_2_text),
            FAQItem(R.string.faq_5_title, R.string.faq_5_text),
            FAQItem(R.string.faq_3_title, R.string.faq_3_text),
            FAQItem(R.string.faq_6_title, R.string.faq_6_text),
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(R.string.faq_1_title_commons, R.string.faq_1_text_commons),
            FAQItem(R.string.faq_4_title_commons, R.string.faq_4_text_commons),
            FAQItem(R.string.faq_4_title, R.string.faq_4_text)
        )

        /*if (!resources.getBoolean(R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))
            faqItems.add(FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons))
            faqItems.add(FAQItem(R.string.faq_7_title_commons, R.string.faq_7_text_commons))

        }*/
        val showItems = AboutItems(true, true, forkedFromUrl, sourceCodeUrl)

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true, showItems)
    }

    private fun searchQueryChanged(text: String) {
        mLatestSearchQuery = text
        binding.searchPlaceholder2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            eventsHelper.getEventsWithSearchQuery(text, this) { searchedText, events ->
                if (searchedText == mLatestSearchQuery) {
                    binding.searchResultsList.beVisibleIf(events.isNotEmpty())
                    binding.searchPlaceholder.beVisibleIf(events.isEmpty())
                    val listItems = getEventListItems(events)
                    val eventsAdapter = EventListAdapter(this, listItems,
                        true, this, binding.searchResultsList) {
                        hideKeyboard()
                        if (it is ListEvent) {
                            Intent(applicationContext, getActivityToOpen(it.isTask)).apply {
                                putExtra(EVENT_ID, it.id)
                                startActivity(this)
                            }
                        }
                    }

                    binding.searchResultsList.adapter = eventsAdapter
                }
            }
        } else {
            binding.searchPlaceholder.beVisible()
            binding.searchResultsList.beGone()
        }
    }

    private fun checkSwipeRefreshAvailability() {
        binding.swipeRefreshLayout.isEnabled = config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!binding.swipeRefreshLayout.isEnabled) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    // only used at active search
    override fun refreshItems() {
        searchQueryChanged(mLatestSearchQuery)
        refreshViewPager()
    }

    private fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS(timestamp / 1000L)
        binding.calendarFab.beVisible()
        config.storedView = DAILY_VIEW
        updateViewPager(dayCode)
    }

    private fun addLiturgicalCalendar() {
        var tlcRefreshListener = config.tlcRefresh
        val refresh = config.isRefresh
        val todayDateTime = Formatter.getDateTimeFromCode(Formatter.getDayCodeFromTS(getNowSeconds())) // no time
        if (tlcRefreshListener == 0L) {
            tlcRefreshListener = todayDateTime.minusDays(1).seconds()
        }
        val today = todayDateTime.seconds()

        if (tlcRefreshListener < today || refresh) {
            ensureBackgroundThread {
                // Update once a week
                if (!refresh) {
                    config.tlcRefresh = todayDateTime.plusDays(7).seconds()
                } else {
                    config.isRefresh = false
                }

                val eventTypeId = eventsHelper.getLiturgicalEventTypeId()
                IcsImporter(this).importEvents(true, "tlc.ics", eventTypeId, 0, false, null)

                runOnUiThread {
                    updateViewPager()
                    setupQuickFilter()
                }
            }
        }
    }

    private fun createEventTypes() {
        if (config.isFirstRun) {
            ensureBackgroundThread {
                val nameB = getString(R.string.birthdays)
                eventsHelper.createPredefinedEventType(nameB, R.color.default_birthdays_color, BIRTHDAY_EVENT)
                val nameA = getString(R.string.anniversaries)
                eventsHelper.createPredefinedEventType(nameA, R.color.default_anniversaries_color, ANNIVERSARY_EVENT)
                runOnUiThread {
                    config.isFirstRun = false
                }
            }
        }
    }

    private fun  checkDeleteOldEvents() {
        // check if this feature has been enabled
        if (config.deleteOldEvents) {
            ensureBackgroundThread {
                val olderThen = config.deleteEventsOlderThen
                eventsHelper.deleteOldEvents(olderThen)
            }
        }
    }

    // events fetched from Thunderbird, https://www.thunderbird.net/en-US/calendar/holidays and
    // https://holidays.kayaposoft.com/public_holidays.php?year=2021
    private fun getHolidayRadioItems(): ArrayList<RadioItem> {
        val items = ArrayList<RadioItem>()

        LinkedHashMap<String, String>().apply {
            put("United States", "unitedstates.ics")
            put("Algeria", "algeria.ics")
            put("Argentina", "argentina.ics")
            put("Australia", "australia.ics")
            put("België", "belgium.ics")
            put("Bolivia", "bolivia.ics")
            put("Brasil", "brazil.ics")
            put("България", "bulgaria.ics")
            put("Canada", "canada.ics")
            put("China", "china.ics")
            put("Colombia", "colombia.ics")
            put("Česká republika", "czech.ics")
            put("Danmark", "denmark.ics")
            put("Deutschland", "germany.ics")
            put("Eesti", "estonia.ics")
            put("España", "spain.ics")
            put("Éire", "ireland.ics")
            put("France", "france.ics")
            put("Fürstentum Liechtenstein", "liechtenstein.ics")
            put("Hellas", "greece.ics")
            put("Hrvatska", "croatia.ics")
            put("India", "india.ics")
            put("Indonesia", "indonesia.ics")
            put("Ísland", "iceland.ics")
            put("Israel", "israel.ics")
            put("Italia", "italy.ics")
            put("Қазақстан Республикасы", "kazakhstan.ics")
            put("المملكة المغربية", "morocco.ics")
            put("Latvija", "latvia.ics")
            put("Lietuva", "lithuania.ics")
            put("Luxemburg", "luxembourg.ics")
            put("Makedonija", "macedonia.ics")
            put("Malaysia", "malaysia.ics")
            put("Magyarország", "hungary.ics")
            put("México", "mexico.ics")
            put("Nederland", "netherlands.ics")
            put("República de Nicaragua", "nicaragua.ics")
            put("日本", "japan.ics")
            put("Nigeria", "nigeria.ics")
            put("Norge", "norway.ics")
            put("Österreich", "austria.ics")
            put("Pākistān", "pakistan.ics")
            put("Polska", "poland.ics")
            put("Portugal", "portugal.ics")
            put("Россия", "russia.ics")
            put("República de Costa Rica", "costarica.ics")
            put("República Oriental del Uruguay", "uruguay.ics")
            put("République d'Haïti", "haiti.ics")
            put("România", "romania.ics")
            put("Schweiz", "switzerland.ics")
            put("Singapore", "singapore.ics")
            put("한국", "southkorea.ics")
            put("Srbija", "serbia.ics")
            put("Slovenija", "slovenia.ics")
            put("Slovensko", "slovakia.ics")
            put("South Africa", "southafrica.ics")
            put("Sri Lanka", "srilanka.ics")
            put("Suomi", "finland.ics")
            put("Sverige", "sweden.ics")
            put("Taiwan", "taiwan.ics")
            put("ราชอาณาจักรไทย", "thailand.ics")
            put("Türkiye Cumhuriyeti", "turkey.ics")
            put("Ukraine", "ukraine.ics")
            put("United Kingdom", "unitedkingdom.ics")

            var i = 0
            for ((country, file) in this) {
                items.add(RadioItem(i++, country, file))
            }
        }

        return items
    }

    /*private fun checkWhatsNewDialog() {
        if (config.showWhatsNewDialog) {
            arrayListOf<Release>().apply {
                //add(Release(8, R.string.release_8))
                //add(Release(9, R.string.release_9))
                //add(Release(10, R.string.release_10))
                //add(Release(11, R.string.release_11))
                //add(Release(12, R.string.release_12))
                //add(Release(14, R.string.release_14))
                //add(Release(15, R.string.release_15))
                //add(Release(16, R.string.release_16))
                //add(Release(17, R.string.release_17))
                //add(Release(18, R.string.release_18))
                //add(Release(19, R.string.release_19))
                add(Release(20, R.string.release_20))
                add(Release(21, R.string.release_21))
                add(Release(22, R.string.release_22))
                add(Release(23, R.string.release_23))

                checkWhatsNew(this, BuildConfig.VERSION_CODE)
            }
        }
    }*/
}
