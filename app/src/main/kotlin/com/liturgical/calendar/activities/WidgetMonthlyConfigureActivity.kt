package com.liturgical.calendar.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.liturgical.calendar.databinding.DayMonthlyNumberViewBinding
import com.liturgical.calendar.databinding.WidgetConfigMonthlyBinding
import com.liturgical.calendar.extensions.addDayEvents
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.helpers.MonthlyCalendarImpl
import com.liturgical.calendar.helpers.MyWidgetMonthlyProvider
import com.liturgical.calendar.helpers.isWeekend
import com.liturgical.calendar.interfaces.MonthlyCalendar
import com.liturgical.calendar.models.DayMonthly
import com.secure.commons.dialogs.ColorPickerDialog
import com.secure.commons.extensions.*
import com.secure.commons.helpers.LOWER_ALPHA
import org.joda.time.DateTime

class WidgetMonthlyConfigureActivity : SimpleActivity(), MonthlyCalendar {
    private var mDays: List<DayMonthly>? = null
    private var dayLabelHeight = 0
    private val binding by viewBinding(WidgetConfigMonthlyBinding::inflate)

    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0
    private var mWeakTextColor = 0
    private var mPrimaryColor = 0
    private var mWeekendsTextColor = 0
    private var mHighlightWeekends = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(binding.root)
        initVariables()

        val extras = intent.extras
        if (extras != null)
            mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish()

        binding.configSave.setOnClickListener { saveConfig() }
        binding.configBgColor.setOnClickListener { pickBackgroundColor() }
        binding.configTextColor.setOnClickListener { pickTextColor() }
        binding.configBgSeekbar.setColors(mTextColor, mPrimaryColor, mPrimaryColor)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.configToolbar)
    }

    private fun initVariables() {
        mTextColorWithoutTransparency = config.widgetTextColor
        updateColors()

        mBgColor = config.widgetBgColor
        mBgAlpha = Color.alpha(mBgColor) / 255.toFloat()

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        binding.configBgSeekbar.setOnSeekBarChangeListener(bgSeekbarChangeListener)
        binding.configBgSeekbar.progress = (mBgAlpha * 100).toInt()
        updateBgColor()

        MonthlyCalendarImpl(this, applicationContext).updateMonthlyCalendar(DateTime().withDayOfMonth(1))
    }

    private fun saveConfig() {
        storeWidgetColors()
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
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

    private fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mTextColorWithoutTransparency = color
                updateColors()
                updateDays()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this,
            MyWidgetMonthlyProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateColors() {
        mTextColor = mTextColorWithoutTransparency
        mWeakTextColor = mTextColorWithoutTransparency.adjustAlpha(LOWER_ALPHA)
        mPrimaryColor = getProperPrimaryColor()
        mWeekendsTextColor = config.highlightWeekendsColor
        mHighlightWeekends = config.highlightWeekends

        binding.configCalendar.topNav.topLeftArrow.applyColorFilter(mTextColor)
        binding.configCalendar.topNav.topRightArrow.applyColorFilter(mTextColor)
        binding.configCalendar.topNav.topValue.setTextColor(mTextColor)
        binding.configTextColor.setFillWithStroke(mTextColor, mTextColor)
        updateLabels()
        binding.configSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
        binding.configSave.setTextColor(getProperPrimaryColor().getContrastColor())
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        //config_calendar.background.applyColorFilter(mBgColor)
        binding.configCalendar.monthCalendarHolder.background.applyColorFilter(mBgColor)
        binding.configBgColor.setFillWithStroke(mBgColor, mBgColor)
    }

    private fun updateDays() {
        val len = mDays!!.size

        if (applicationContext.config.showWeekNumbers) {
            binding.configCalendar.firstRow.weekNum.setTextColor(mTextColor)
            binding.configCalendar.firstRow.weekNum.beVisible()

            // TODO: need to change this function for viewBinding
            for (i in 0..5) {
                findViewById<TextView>(resources.getIdentifier("week_num_$i", "id", packageName)).apply {
                    text = "${mDays!![i * 7 + 3].weekOfYear}:"
                    setTextColor(mTextColor)
                    beVisible()
                }
            }
        }

        val dividerMargin = resources.displayMetrics.density.toInt()
        // TODO: need to convert to viewBinding
        for (i in 0 until len) {
            findViewById<LinearLayout>(resources.getIdentifier("day_$i", "id", packageName)).apply {
                val day = mDays!![i]
                removeAllViews()

                val dayTextColor = if (config.highlightWeekends && day.isWeekend) {
                    config.highlightWeekendsColor
                } else {
                    mTextColor
                }

                addDayNumber(dayTextColor, day, this, dayLabelHeight) { dayLabelHeight = it }
                context.addDayEvents(day, this, resources, dividerMargin)
            }
        }
    }

    private fun addDayNumber(rawTextColor: Int, day: DayMonthly, linearLayout: LinearLayout,
                             dayLabelHeight: Int, callback: (Int) -> Unit) {
        var textColor = rawTextColor
        if (!day.isThisMonth) {
            textColor = textColor.adjustAlpha(LOWER_ALPHA)
        }

        //(View.inflate(applicationContext, R.layout.day_monthly_number_view, null) as RelativeLayout).apply
        DayMonthlyNumberViewBinding.inflate(layoutInflater).apply {
            root.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            linearLayout.addView(this.root)

            dayMonthlyNumberBackground.beVisibleIf(day.isToday)
            dayMonthlyNumberId.apply {
                setTextColor(textColor)
                text = day.value.toString()
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }

            if (day.isToday) {
                dayMonthlyNumberBackground.setColorFilter(getProperPrimaryColor())
                dayMonthlyNumberId.setTextColor(getProperPrimaryColor().getContrastColor())
            }
        }
    }

    private val bgSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mBgAlpha = progress.toFloat() / 100.toFloat()
            updateBgColor()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    override fun updateMonthlyCalendar(context: Context, month: String, days: ArrayList<DayMonthly>,
                                       checkedEvents: Boolean, currTargetDate: DateTime) {
        runOnUiThread {
            mDays = days
                binding.configCalendar.topNav.topValue.text = month
            updateDays()
        }
    }

    private fun updateLabels() {
        for (i in 0..6) {
            findViewById<TextView>(resources.getIdentifier("label_$i", "id", packageName)).apply {
                val textColor = if (config.highlightWeekends && isWeekend(i, config.isSundayFirst)) {
                    mWeekendsTextColor
                } else {
                    mTextColor
                }

                setTextColor(textColor)
            }
        }
    }
}
