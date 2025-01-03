package com.liturgical.calendar.activities

import android.annotation.SuppressLint
import android.content.Intent
import com.liturgical.calendar.extensions.getNewEventTimestampFromCode
import com.liturgical.calendar.helpers.*
import com.secure.commons.activities.BaseSplashActivity
import org.joda.time.DateTime

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        when {
            intent.extras?.containsKey(DAY_CODE) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(DAY_CODE, intent.getStringExtra(DAY_CODE))
                putExtra(VIEW_TO_OPEN, intent.getIntExtra(VIEW_TO_OPEN, LAST_VIEW))
                startActivity(this)
            }
            intent.extras?.containsKey(EVENT_ID) == true -> Intent(this, MainActivity::class.java).apply {
                putExtra(IS_TASK, intent.getBooleanExtra(IS_TASK, false))
                putExtra(EVENT_ID, intent.getLongExtra(EVENT_ID, 0L))
                putExtra(EVENT_OCCURRENCE_TS, intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L))
                startActivity(this)
            }
            intent.action == SHORTCUT_NEW_EVENT -> {
                val dayCode = Formatter.getDayCodeFromDateTime(DateTime())
                Intent(this, EventActivity::class.java).apply {
                    putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
                    startActivity(this)
                }
            }
            intent.action == SHORTCUT_NEW_TASK -> {
                val dayCode = Formatter.getDayCodeFromDateTime(DateTime())
                Intent(this, TaskActivity::class.java).apply {
                    putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode))
                    startActivity(this)
                }
            }
            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
