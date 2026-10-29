package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey
    val id: String,
    val name: String,
    val nik: String,
    val position: String,
    val department: String,
    val email: String,
    val phoneNumber: String,
    val annualLeaveQuota: Int = 12,
    val usedLeaveQuota: Int = 0,
    val faceEnrolled: Boolean = true,
    val referenceFacePath: String? = null,
    val role: String = ROLE_EMPLOYEE // "KARYAWAN" or "HR_MANAGER"
) {
    val remainingLeave: Int
        get() = (annualLeaveQuota - usedLeaveQuota).coerceAtLeast(0)

    companion object {
        const val ROLE_EMPLOYEE = "KARYAWAN"
        const val ROLE_HR_MANAGER = "HR_MANAGER"
    }
}
