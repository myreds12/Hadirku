package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_requests")
data class LeaveRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val employeeName: String,
    val employeeDepartment: String,
    val leaveType: String, // CUTI_TAHUNAN, CUTI_SAKIT, IZIN_KHUSUS, CUTI_MELAHIRKAN, CUTI_MENIKAH
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String, // "YYYY-MM-DD"
    val durationDays: Int,
    val reason: String,
    val emergencyContact: String,
    val attachmentUri: String? = null,
    val submissionTimestamp: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING, // PENDING, APPROVED, REJECTED
    val reviewerNotes: String? = null,
    val reviewedTimestamp: Long? = null
) {
    companion object {
        const val TYPE_ANNUAL = "Cuti Tahunan"
        const val TYPE_SICK = "Cuti Sakit"
        const val TYPE_SPECIAL_PERMIT = "Izin Khusus"
        const val TYPE_MATERNITY = "Cuti Melahirkan"
        const val TYPE_MARRIAGE = "Cuti Menikah"

        const val STATUS_PENDING = "MENUNGGU"
        const val STATUS_APPROVED = "DISETUJUI"
        const val STATUS_REJECTED = "DITOLAK"
    }
}
