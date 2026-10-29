package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY date DESC, id DESC")
    fun getAttendanceForEmployee(employeeId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND date = :date LIMIT 1")
    suspend fun getTodayAttendance(employeeId: String, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND date = :date LIMIT 1")
    fun observeTodayAttendance(employeeId: String, date: String): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, id DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecord): Long

    @Update
    suspend fun update(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}
