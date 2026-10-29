package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val date: String, // "YYYY-MM-DD"
    val checkInTime: Long? = null, // epoch millis
    val checkOutTime: Long? = null,
    val checkInPhotoUri: String? = null,
    val checkOutPhotoUri: String? = null,
    val checkInStatus: String = STATUS_ON_TIME, // TEPAT_WAKTU, TERLAMBAT
    val checkOutStatus: String? = null, // NORMAL, PULANG_AWAL, LEMBUR
    val locationName: String = "Kantor Pusat - Jakarta",
    val distanceMeters: Double = 12.0,
    val biometricConfidence: Float = 98.5f,
    val notes: String? = null
) {
    companion object {
        const val STATUS_ON_TIME = "TEPAT_WAKTU"
        const val STATUS_LATE = "TERLAMBAT"
        const val STATUS_EARLY_OUT = "PULANG_AWAL"
        const val STATUS_NORMAL_OUT = "NORMAL"
        const val STATUS_OVERTIME = "LEMBUR"
    }
}
