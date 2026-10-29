package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.LeaveRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Employee::class, AttendanceRecord::class, LeaveRequest::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun leaveDao(): LeaveDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hadirku_attendance.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val initialEmployees = listOf(
                Employee(
                    id = "EMP-001",
                    name = "Dony Setyawan",
                    nik = "199408122020121001",
                    position = "Senior Android Specialist",
                    department = "Technology & Core Engineering",
                    email = "dony.setyawan@perusahaan.co.id",
                    phoneNumber = "+62 812-3456-7890",
                    annualLeaveQuota = 12,
                    usedLeaveQuota = 2,
                    faceEnrolled = true,
                    role = Employee.ROLE_EMPLOYEE
                ),
                Employee(
                    id = "EMP-002",
                    name = "Siti Rahmawati, S.Psi",
                    nik = "199203152018032002",
                    position = "Head of People & HR Operations",
                    department = "Human Resources & Talent",
                    email = "siti.rahma@perusahaan.co.id",
                    phoneNumber = "+62 811-9876-5432",
                    annualLeaveQuota = 14,
                    usedLeaveQuota = 1,
                    faceEnrolled = true,
                    role = Employee.ROLE_HR_MANAGER
                ),
                Employee(
                    id = "EMP-003",
                    name = "Dimas Prasetyo",
                    nik = "199611202021081003",
                    position = "UI/UX Product Designer",
                    department = "Product & Design",
                    email = "dimas.prasetyo@perusahaan.co.id",
                    phoneNumber = "+62 856-7788-9900",
                    annualLeaveQuota = 12,
                    usedLeaveQuota = 4,
                    faceEnrolled = true,
                    role = Employee.ROLE_EMPLOYEE
                )
            )
            db.employeeDao().insertAll(initialEmployees)

            // Seed historical attendances
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L

            val pastAttendances = listOf(
                AttendanceRecord(
                    employeeId = "EMP-001",
                    date = "2026-10-24",
                    checkInTime = now - (3 * oneDayMillis) + (8 * 3600 * 1000L),
                    checkOutTime = now - (3 * oneDayMillis) + (17 * 3600 * 1000L) + (10 * 60 * 1000L),
                    checkInStatus = AttendanceRecord.STATUS_ON_TIME,
                    checkOutStatus = AttendanceRecord.STATUS_NORMAL_OUT,
                    locationName = "Kantor Pusat - Jakarta",
                    distanceMeters = 8.5,
                    biometricConfidence = 99.1f,
                    notes = "Verifikasi Face ID Berhasil"
                ),
                AttendanceRecord(
                    employeeId = "EMP-001",
                    date = "2026-10-25",
                    checkInTime = now - (2 * oneDayMillis) + (8 * 3600 * 1000L) + (12 * 60 * 1000L),
                    checkOutTime = now - (2 * oneDayMillis) + (17 * 3600 * 1000L),
                    checkInStatus = AttendanceRecord.STATUS_ON_TIME,
                    checkOutStatus = AttendanceRecord.STATUS_NORMAL_OUT,
                    locationName = "Kantor Pusat - Jakarta",
                    distanceMeters = 14.2,
                    biometricConfidence = 98.6f,
                    notes = "Verifikasi Face ID Berhasil"
                ),
                AttendanceRecord(
                    employeeId = "EMP-001",
                    date = "2026-10-26",
                    checkInTime = now - oneDayMillis + (8 * 3600 * 1000L) + (4 * 60 * 1000L),
                    checkOutTime = now - oneDayMillis + (17 * 3600 * 1000L) + (30 * 60 * 1000L),
                    checkInStatus = AttendanceRecord.STATUS_ON_TIME,
                    checkOutStatus = AttendanceRecord.STATUS_OVERTIME,
                    locationName = "Kantor Pusat - Jakarta",
                    distanceMeters = 11.0,
                    biometricConfidence = 99.4f,
                    notes = "Lembur 30 menit disetujui"
                )
            )
            pastAttendances.forEach { db.attendanceDao().insert(it) }

            // Seed initial leave request
            val sampleLeave = LeaveRequest(
                employeeId = "EMP-001",
                employeeName = "Dony Setyawan",
                employeeDepartment = "Technology & Core Engineering",
                leaveType = LeaveRequest.TYPE_ANNUAL,
                startDate = "2026-11-10",
                endDate = "2026-11-11",
                durationDays = 2,
                reason = "Acara keluarga dan keperluan pribadi di luar kota",
                emergencyContact = "+62 812-9988-7766 (Istri)",
                submissionTimestamp = now - (48 * 3600 * 1000L),
                status = LeaveRequest.STATUS_APPROVED,
                reviewerNotes = "Disetujui. Selamat berlibur!",
                reviewedTimestamp = now - (24 * 3600 * 1000L)
            )
            db.leaveDao().insert(sampleLeave)

            val pendingLeave = LeaveRequest(
                employeeId = "EMP-003",
                employeeName = "Dimas Prasetyo",
                employeeDepartment = "Product & Design",
                leaveType = LeaveRequest.TYPE_SICK,
                startDate = "2026-10-28",
                endDate = "2026-10-29",
                durationDays = 2,
                reason = "Pemeriksaan kesehatan rutin dan istirahat dokter",
                emergencyContact = "+62 856-1122-3344",
                submissionTimestamp = now - (2 * 3600 * 1000L),
                status = LeaveRequest.STATUS_PENDING,
                reviewerNotes = null,
                reviewedTimestamp = null
            )
            db.leaveDao().insert(pendingLeave)
        }
    }
}
