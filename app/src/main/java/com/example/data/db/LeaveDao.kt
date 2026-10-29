package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LeaveRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_requests WHERE employeeId = :employeeId ORDER BY submissionTimestamp DESC")
    fun getLeaveRequestsForEmployee(employeeId: String): Flow<List<LeaveRequest>>

    @Query("SELECT * FROM leave_requests ORDER BY submissionTimestamp DESC")
    fun getAllLeaveRequests(): Flow<List<LeaveRequest>>

    @Query("SELECT * FROM leave_requests WHERE status = 'MENUNGGU' ORDER BY submissionTimestamp DESC")
    fun getPendingLeaveRequests(): Flow<List<LeaveRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: LeaveRequest): Long

    @Update
    suspend fun update(request: LeaveRequest)

    @Query("UPDATE leave_requests SET status = :status, reviewerNotes = :notes, reviewedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, notes: String?, timestamp: Long)
}
