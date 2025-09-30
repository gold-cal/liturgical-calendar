package com.liturgical.calendar.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import com.liturgical.calendar.R
import com.liturgical.calendar.adapters.WidgetConfigAdapter
import com.liturgical.calendar.databinding.WidgetConfigListBinding
import com.liturgical.calendar.dialogs.CustomPeriodPickerDialog
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.helpers.EVENT_PERIOD_CUSTOM
import com.liturgical.calendar.helpers.EVENT_PERIOD_TODAY
import com.liturgical.calendar.helpers.Formatter
import com.liturgical.calendar.helpers.MyWidgetListProvider
import com.liturgical.calendar.models.ListEvent
import com.liturgical.calendar.models.ListItem
import com.liturgical.calendar.models.ListSectionDay
import com.liturgical.calendar.models.Widget
import com.secure.commons.dialogs.ColorPickerDialog
import com.secure.commons.dialogs.RadioGroupDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.*
import com.secure.commons.models.RadioItem
import org.joda.time.DateTime
import java.util.*

class WidgetListConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    //private var mDayAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mDayColorWithoutTransparency = 0
    private var mDayColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0
    private var selectedPeriodOption = 0
    private val binding by viewBinding(WidgetConfigListBinding::inflate)

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(binding.root)
        initVariables()

        val isCustomizingColors = true //intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        WidgetConfigAdapter(this, getListItems(), false, null, binding.configEventsList) {}.apply {
            updateTextColor(mTextColor)
            binding.configEventsList.adapter = this
        }

        binding.periodPickerHolder.background.applyColorFilter(getProperBackgroundColor())
        binding.periodPickerValue.setOnClickListener { showPeriodSelector() }

        binding.configSave.setOnClickListener { saveConfig() }
        binding.configBgColor.setOnClickListener { pickBackgroundColor() }
        binding.configDayColor.setOnClickListener { pickDayColor() }
        binding.configTextColor.setOnClickListener { pickTextColor() }

        binding.periodPickerHolder.beGoneIf(isCustomizingColors)

        val primaryColor = getProperPrimaryColor()
        binding.configBgSeekbar.setColors(mTextColor, primaryColor, primaryColor)

        updateSelectedPeriod(config.lastUsedEventSpan)
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(binding.configListHolder)
        setupToolbar(binding.configToolbar)
    }

    private fun initVariables() {
        mTextColorWithoutTransparency = config.widgetTextColor
        updateColors()

        mBgColor = config.widgetBgColor
        mDayColor = config.widgetDayColor
        mBgAlpha = Color.alpha(mBgColor) / 255.toFloat()
        //mDayAlpha = Color.alpha(mDayColor) / 255.toFloat()

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        mDayColorWithoutTransparency = Color.rgb(Color.red(mDayColor), Color.green(mDayColor), Color.blue(mDayColor))
        binding.configBgSeekbar.setOnSeekBarChangeListener(bgSeekbarChangeListener)
        //config_day_seekbar.setOnSeekBarChangeListener(daySeekbarChangeListener)
        binding.configBgSeekbar.progress = (mBgAlpha * 100).toInt()
        //config_day_seekbar.progress = (mDayAlpha * 100).toInt()

        updateBgColor()
        updateDayColor()
    }

    private fun saveConfig() {
        val widget = Widget(null, mWidgetId, selectedPeriodOption)
        ensureBackgroundThread {
            widgetsDB.insertOrUpdate(widget)
        }

        storeWidgetColors()
        requestWidgetUpdate()

        config.lastUsedEventSpan = selectedPeriodOption
        if (!config.widgetUpdate) {
            scheduleListWidgetRefresh(true)
        }

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun showPeriodSelector() {
        hideKeyboard()
        val seconds = TreeSet<Int>()
        seconds.apply {
            add(EVENT_PERIOD_TODAY)
            add(WEEK_SECONDS)
            add(MONTH_SECONDS)
            add(YEAR_SECONDS)
            add(selectedPeriodOption)
        }

        val items = ArrayList<RadioItem>(seconds.size)
        seconds.mapIndexedTo(items) { index, value ->
            RadioItem(index, getFormattedSeconds(value), value)
        }

        var selectedIndex = 0
        seconds.forEachIndexed { index, value ->
            if (value == selectedPeriodOption) {
                selectedIndex = index
            }
        }

        items.add(RadioItem(EVENT_PERIOD_CUSTOM, getString(R.string.within_the_next)))

        RadioGroupDialog(this, items, selectedIndex, showOKButton = true, cancelCallback = null) { any ->
            val option = any as Int
            if (option == EVENT_PERIOD_CUSTOM) {
                CustomPeriodPickerDialog(this) {
                    updateSelectedPeriod(it)
                }
            } else {
                updateSelectedPeriod(option)
            }
        }
    }

    private fun updateSelectedPeriod(selectedPeriod: Int) {
        selectedPeriodOption = selectedPeriod
        when (selectedPeriod) {
            0 -> {
                selectedPeriodOption = YEAR_SECONDS
                binding.periodPickerValue.setText(R.string.within_the_next_one_year)
            }
            EVENT_PERIOD_TODAY -> binding.periodPickerValue.setText(R.string.today_only)
            else -> binding.periodPickerValue.text = getFormattedSeconds(selectedPeriodOption)
        }
    }

    private fun getFormattedSeconds(seconds: Int): String = if (seconds == EVENT_PERIOD_TODAY) {
        getString(R.string.today_only)
    } else {
        when {
            seconds == YEAR_SECONDS -> getString(R.string.within_the_next_one_year)
            seconds % MONTH_SECONDS == 0 -> resources.getQuantityString(R.plurals.within_the_next_months, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            seconds % WEEK_SECONDS == 0 -> resources.getQuantityString(R.plurals.within_the_next_weeks, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            else -> resources.getQuantityString(R.plurals.within_the_next_days, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
        }
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetDayColor = mDayColor
            widgetTextColor = mTextColorWithoutTransparency
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBgColor()
            }
        }
    }

    private fun pickDayColor() {
        ColorPickerDialog(this, mDayColorWithoutTransparency) { wasPositivePressed, color ->
            if(wasPositivePressed) {
                mDayColorWithoutTransparency = color
                updateDayColor()
            }
        }
    }

    private fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mTextColorWithoutTransparency = color
                updateColors()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetListProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateColors() {
        mTextColor = mTextColorWithoutTransparency
        (binding.configEventsList.adapter as? WidgetConfigAdapter)?.updateTextColor(mTextColor)
        binding.configTextColor.setFillWithStroke(mTextColor, mTextColor)
        binding.configSave.setTextColor(getProperPrimaryColor().getContrastColor())
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        binding.configEventsList.background.applyColorFilter(mBgColor)
        binding.configBgColor.setFillWithStroke(mBgColor, mBgColor)
        binding.configSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
    }

    private fun updateDayColor() {
        mDayColor = mDayColorWithoutTransparency.adjustAlpha(mBgAlpha)
        (binding.configEventsList.adapter as? WidgetConfigAdapter)?.updateDaybg(mDayColor)
        binding.configDayColor.setFillWithStroke(mDayColor, mDayColor)
    }

    private fun getListItems(): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>(10)
        var dateTime = DateTime.now().withTime(0, 0, 0, 0).plusDays(1)
        var code = Formatter.getDayCodeFromTS(dateTime.seconds())
        var day = Formatter.getDateDayTitle(code)
        listItems.add(ListSectionDay(day, code, mDayColor, false, false))

        var time = dateTime.withHourOfDay(7)
        listItems.add(
            ListEvent(
                1,
                time.seconds(),
                time.plusMinutes(30).seconds(),
                getString(R.string.sample_title_1),
                getString(R.string.sample_description_1),
                false,
                getProperAccentColor(),
                "",
                false,
                false,
                false,
                false,
                false,
                "",
                ""
            )
        )
        time = dateTime.withHourOfDay(8)
        listItems.add(
            ListEvent(
                2,
                time.seconds(),
                time.plusHours(1).seconds(),
                getString(R.string.sample_title_2),
                getString(R.string.sample_description_2),
                false,
                getProperAccentColor(),
                "",
                false,
                false,
                false,
                false,
                false,
                "",
                ""
            )
        )

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDateDayTitle(code)
        listItems.add(ListSectionDay(day, code, mDayColor, false, false))

        time = dateTime.withHourOfDay(8)
        listItems.add(
            ListEvent(
                3,
                time.seconds(),
                time.plusHours(1).seconds(),
                getString(R.string.sample_title_3),
                "",
                false,
                getProperAccentColor(),
                "",
                false,
                false,
                false,
                false,
                false,
                "",
                ""
            )
        )
        time = dateTime.withHourOfDay(13)
        listItems.add(
            ListEvent(
                4,
                time.seconds(),
                time.plusHours(1).seconds(),
                getString(R.string.sample_title_4),
                getString(R.string.sample_description_4),
                false,
                getProperAccentColor(),
                "",
                false,
                false,
                false,
                false,
                false,
                "",
                ""
            )
        )
        time = dateTime.withHourOfDay(18)
        listItems.add(
            ListEvent(
                5,
                time.seconds(),
                time.plusMinutes(10).seconds(),
                getString(R.string.sample_title_5),
                "",
                false,
                getProperAccentColor(),
                "",
                false,
                false,
                false,
                false,
                false,
                "",
                ""
            )
        )

        return listItems
    }

    private val bgSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mBgAlpha = progress.toFloat() / 100.toFloat()
            updateBgColor()
            updateDayColor()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    /*private val daySeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mDayAlpha = progress.toFloat() / 100.toFloat()
            updateDayColor()
        }
        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }*/
}
