package com.liturgical.calendar.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liturgical.calendar.R
import com.liturgical.calendar.extensions.config
import com.liturgical.calendar.helpers.Converters
import com.liturgical.calendar.helpers.LITURGICAL_EVENT
import com.liturgical.calendar.helpers.LITURGICAL_EVENT_TYPE_ID
import com.liturgical.calendar.helpers.REGULAR_EVENT_TYPE_ID
import com.liturgical.calendar.interfaces.EventTypesDao
import com.liturgical.calendar.interfaces.EventsDao
import com.liturgical.calendar.interfaces.TasksDao
import com.liturgical.calendar.interfaces.WidgetsDao
import com.liturgical.calendar.models.Event
import com.liturgical.calendar.models.EventType
import com.liturgical.calendar.models.Task
import com.liturgical.calendar.models.Widget
import com.secure.commons.extensions.getProperPrimaryColor
import java.util.concurrent.Executors

@Database(entities = [Event::class, EventType::class, Widget::class, Task::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EventsDatabase : RoomDatabase() {

    abstract fun EventsDao(): EventsDao

    abstract fun EventTypesDao(): EventTypesDao

    abstract fun WidgetsDao(): WidgetsDao

    abstract fun TasksDao(): TasksDao

    companion object {
        private var db: EventsDatabase? = null

        fun getInstance(context: Context): EventsDatabase {
            if (db == null) {
                synchronized(EventsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, EventsDatabase::class.java, "events.db")
                            .addCallback(object : Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    insertRegularEventType(context)
                                    insertLiturgicalEventType(context)
                                }
                            })
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .addMigrations(MIGRATION_3_4)
                            .addMigrations(MIGRATION_4_5)
                            .addMigrations(MIGRATION_5_6)
                            .addMigrations(MIGRATION_6_7)
                            .addMigrations(MIGRATION_7_8)
                            .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        private fun insertRegularEventType(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val regularEvent = context.resources.getString(R.string.regular_event)
                val eventType = EventType(REGULAR_EVENT_TYPE_ID, regularEvent, context.getProperPrimaryColor())
                db!!.EventTypesDao().insertOrUpdate(eventType)
                context.config.addDisplayEventType(REGULAR_EVENT_TYPE_ID.toString())
            }
        }

        private fun insertLiturgicalEventType(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val liturgicalEvent = context.resources.getString(R.string.liturgical_event)
                val eventType = EventType(LITURGICAL_EVENT_TYPE_ID,
                    liturgicalEvent,
                    context.resources.getColor(R.color.default_liturgical_color),
                    caldavCalendarId = 0,
                    type = LITURGICAL_EVENT)
                db!!.EventTypesDao().insertOrUpdate(eventType)
                context.config.addDisplayEventType(LITURGICAL_EVENT_TYPE_ID.toString())
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN reminder_1_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN reminder_2_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN reminder_3_type INTEGER NOT NULL DEFAULT 0")
                    execSQL("ALTER TABLE events ADD COLUMN attendees TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN time_zone TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN availability INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("CREATE TABLE IF NOT EXISTS `widgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `widget_id` INTEGER NOT NULL, `period` INTEGER NOT NULL)")
                    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_widgets_widget_id` ON `widgets` (`widget_id`)")
                }
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE events ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `task_id` INTEGER NOT NULL, `start_ts` INTEGER NOT NULL, `flags` INTEGER NOT NULL)")
                    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_id_task_id` ON `tasks` (`id`, `task_id`)")
                }
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE event_types ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
                }
            }
        }
    }
}