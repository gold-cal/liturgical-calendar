package com.liturgical.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.liturgical.calendar.R
import com.liturgical.calendar.activities.SplashActivity
import com.liturgical.calendar.extensions.*
import com.liturgical.calendar.services.WidgetService
import com.liturgical.calendar.services.WidgetServiceEmpty
import com.secure.commons.extensions.*
import com.secure.commons.helpers.ensureBackgroundThread
import org.joda.time.DateTime

class MyWidgetListProvider : AppWidgetProvider() {
    //private val NEW_EVENT = "new_event"
    private val UPDATE_CAL = "update_cal"
    private val LAUNCH_CAL = "launch_cal"
    //private val GO_TO_TODAY = "go_to_today"
    private val SCROLL_TO_TODAY = "scroll_to_today"

    val ACTION_AUTO_UPDATE: String = "com.liturgical.calendar.AUTO_UPDATE"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        val fontSize = context.getWidgetFontSize()
        val textColor = context.config.widgetTextColor

        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        ensureBackgroundThread {
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                val widget = context.widgetsDB.getWidgetWithWidgetId(it)
                val views = RemoteViews(context.packageName, R.layout.widget_event_list).apply {
                    applyColorFilter(R.id.widget_event_list_background, context.config.widgetBgColor)
                    setTextColor(R.id.widget_event_list_empty, textColor)
                    setTextSize(R.id.widget_event_list_empty, fontSize)

                    setTextColor(R.id.widget_event_list_today, textColor)
                    setTextSize(R.id.widget_event_list_today, fontSize)
                }

                views.setImageViewBitmap(R.id.widget_event_update_event, context.resources.getColoredBitmap(R.drawable.ic_update_vector, textColor))
                setupIntent(context, views, UPDATE_CAL, R.id.widget_event_update_event)
                setupIntent(context, views, LAUNCH_CAL, R.id.widget_event_list_today)

                views.setImageViewBitmap(R.id.widget_event_go_to_today, context.resources.getColoredBitmap(R.drawable.ic_today_vector, textColor))
                setupIntent(context, views, SCROLL_TO_TODAY, R.id.widget_event_go_to_today)

                Intent(context, WidgetService::class.java).apply {
                    putExtra(EVENT_LIST_PERIOD, widget?.period)
                    data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                    views.setRemoteAdapter(R.id.widget_event_list, this)
                }

                val startActivityIntent = context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)
                val startActivityPendingIntent =
                    PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                views.setPendingIntentTemplate(R.id.widget_event_list, startActivityPendingIntent)
                views.setEmptyView(R.id.widget_event_list, R.id.widget_event_list_empty)

                appWidgetManager.updateAppWidget(it, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_event_list)
            }
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, MyWidgetListProvider::class.java)

    private fun setupIntent(context: Context, views: RemoteViews, action: String, id: Int) {
        Intent(context, MyWidgetListProvider::class.java).apply {
            this.action = action
            val pendingIntent = PendingIntent.getBroadcast(context, 0, this, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_AUTO_UPDATE -> performUpdate(context)//context.updateListWidget()
            UPDATE_CAL -> performUpdate(context) //updateCalendar(context) //context.launchNewEventOrTaskActivity()
            LAUNCH_CAL -> launchCalenderInDefaultView(context)
            SCROLL_TO_TODAY -> scrollToToday(context) //goToToday(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        context.scheduleListWidgetRefresh(true)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        ensureBackgroundThread {
            appWidgetIds?.forEach {
                context?.widgetsDB?.deleteWidgetId(it)
                context?.scheduleListWidgetRefresh(false)
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        context?.scheduleListWidgetRefresh(false)
    }

    private fun launchCalenderInDefaultView(context: Context) {
        (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
            putExtra(DAY_CODE, Formatter.getDayCodeFromDateTime(DateTime()))
            putExtra(VIEW_TO_OPEN, context.config.listWidgetViewToOpen)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(this)
        }
    }

    /*private fun updateCalendar(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            val views = RemoteViews(context.packageName, R.layout.widget_event_list)
            Intent(context, WidgetServiceEmpty::class.java).apply {
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                views.setRemoteAdapter(R.id.widget_event_list, this)
            }

            appWidgetManager.updateAppWidget(it, views)
        }

        performUpdate(context)
    }*/

    private fun scrollToToday(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            val view = RemoteViews(context.packageName, R.layout.widget_event_list)
            if (context.config.listWidgetDayPosition == 0) {
                view.setScrollPosition(R.id.widget_event_list, context.config.currentScrollPosition)
            } else if (context.config.viewPosition <= context.config.listWidgetDayPosition) {
                view.setScrollPosition(R.id.widget_event_list, context.config.listWidgetDayPosition + 10)
            } else {
                view.setScrollPosition(R.id.widget_event_list, context.config.listWidgetDayPosition)
            }
            appWidgetManager.updateAppWidget(it, view)
            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_event_list)
        }
    }
}
