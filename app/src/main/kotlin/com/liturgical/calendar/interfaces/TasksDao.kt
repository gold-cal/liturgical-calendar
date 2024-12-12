package com.liturgical.calendar.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.liturgical.calendar.models.Task

@Dao
interface TasksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(task: Task): Long

    @Query("SELECT * FROM tasks WHERE task_id = :id AND start_ts = :startTs")
    fun getTaskWithIdAndTs(id: Long, startTs: Long): Task?

    @Query("SELECT task_id FROM tasks WHERE task_id = :id")
    fun getTaskIdWithId(id: Long): Long?

    @Query("DELETE FROM tasks WHERE task_id = :id AND start_ts = :startTs")
    fun deleteTaskWithIdAndTs(id: Long, startTs: Long)
}
