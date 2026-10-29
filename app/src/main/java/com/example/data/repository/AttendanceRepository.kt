package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.LeaveRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudSyncInfo(
    val isConnected: Boolean = false,
    val lastSyncTime: String = "Menghubungkan...",
    val totalEmployeesSynced: Int = 0,
    val totalAttendanceSynced: Int = 0,
    val syncStatusMessage: String = "Menghubungkan ke Firestore..."
)

class AttendanceRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val attendanceDao = database.attendanceDao()
    private val leaveDao = database.leaveDao()
    private val employeeDao = database.employeeDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncInfo = MutableStateFlow(CloudSyncInfo())
    val syncInfo: StateFlow<CloudSyncInfo> = _syncInfo.asStateFlow()

    // Company Settings from Cloud (Settings collection, company_config document)
    private val _officeConfig = MutableStateFlow(
        OfficeConfig(
            name = "Kantor Pusat Menara Mandiri, Jakarta",
            latitude = -6.2255,
            longitude = 106.8080,
            radiusMeters = 100.0,
            startTime = "08:00",
            endTime = "17:00"
        )
    )
    val officeConfig: StateFlow<OfficeConfig> = _officeConfig.asStateFlow()

    data class OfficeConfig(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
        val startTime: String,
        val endTime: String
    )

    // Firestore instance with custom database ID
    val firestore: FirebaseFirestore by lazy {
        val databaseId = try {
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "ai-studio-de36770d-74ff-4d7d-a0f4-ea856ef139bc"
        } catch (_: Exception) {
            "ai-studio-de36770d-74ff-4d7d-a0f4-ea856ef139bc"
        }
        val app = FirebaseApp.getInstance()
        Log.i("AttendanceRepository", "Initializing Firestore for database ID: $databaseId")
        FirebaseFirestore.getInstance(app, databaseId)
    }

    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()

    init {
        setupFirestoreRealtimeSync()
    }

    fun getEmployee(id: String): Flow<Employee?> = employeeDao.observeEmployee(id)

    suspend fun getEmployeeDirect(id: String): Employee? = employeeDao.getEmployeeById(id)

    fun getAttendanceHistory(employeeId: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForEmployee(employeeId)

    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>> =
        attendanceDao.getAllAttendanceRecords()

    fun observeTodayAttendance(employeeId: String, date: String): Flow<AttendanceRecord?> =
        attendanceDao.observeTodayAttendance(employeeId, date)

    suspend fun getTodayAttendance(employeeId: String, date: String): AttendanceRecord? =
        attendanceDao.getTodayAttendance(employeeId, date)

    fun getLeaveRequests(employeeId: String): Flow<List<LeaveRequest>> =
        leaveDao.getLeaveRequestsForEmployee(employeeId)

    fun getAllLeaveRequests(): Flow<List<LeaveRequest>> =
        leaveDao.getAllLeaveRequests()

    fun getPendingLeaveRequests(): Flow<List<LeaveRequest>> =
        leaveDao.getPendingLeaveRequests()

    suspend fun recordCheckIn(
        employeeId: String,
        photoUri: String?,
        confidence: Float,
        locationName: String = "Kantor Pusat - Jakarta",
        distanceMeters: Double = 12.0
    ): AttendanceRecord {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()

        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val isLate = (hour > 8) || (hour == 8 && minute > 30)
        val status = if (isLate) AttendanceRecord.STATUS_LATE else AttendanceRecord.STATUS_ON_TIME

        val existing = attendanceDao.getTodayAttendance(employeeId, todayStr)
        val record = if (existing != null) {
            existing.copy(
                checkInTime = now,
                checkInPhotoUri = photoUri ?: existing.checkInPhotoUri,
                checkInStatus = status,
                biometricConfidence = confidence,
                distanceMeters = distanceMeters,
                locationName = locationName,
                notes = if (isLate) "Terlambat ${hour - 8} jam ${minute} menit" else "Tepat waktu"
            )
        } else {
            AttendanceRecord(
                employeeId = employeeId,
                date = todayStr,
                checkInTime = now,
                checkInPhotoUri = photoUri,
                checkInStatus = status,
                biometricConfidence = confidence,
                distanceMeters = distanceMeters,
                locationName = locationName,
                notes = if (isLate) "Terlambat ${hour - 8} jam ${minute} menit" else "Tepat waktu"
            )
        }

        if (existing != null) {
            attendanceDao.update(record)
        } else {
            val id = attendanceDao.insert(record)
        }

        // Send to Firestore attendance collection
        pushAttendanceToFirestore(
            employeeId = employeeId,
            date = todayStr,
            type = "CHECK_IN",
            timestamp = now,
            status = status,
            locationName = locationName,
            distanceMeters = distanceMeters,
            confidence = confidence,
            notes = record.notes
        )

        return record
    }

    suspend fun recordCheckOut(
        employeeId: String,
        photoUri: String?,
        confidence: Float
    ): AttendanceRecord {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        val outStatus = when {
            hour < 17 -> AttendanceRecord.STATUS_EARLY_OUT
            hour >= 18 -> AttendanceRecord.STATUS_OVERTIME
            else -> AttendanceRecord.STATUS_NORMAL_OUT
        }

        val existing = attendanceDao.getTodayAttendance(employeeId, todayStr)
        val record = if (existing != null) {
            existing.copy(
                checkOutTime = now,
                checkOutPhotoUri = photoUri ?: existing.checkOutPhotoUri,
                checkOutStatus = outStatus,
                notes = (existing.notes ?: "") + " | Checkout: $outStatus"
            )
        } else {
            AttendanceRecord(
                employeeId = employeeId,
                date = todayStr,
                checkInTime = now - (8 * 3600 * 1000L),
                checkOutTime = now,
                checkOutPhotoUri = photoUri,
                checkInStatus = AttendanceRecord.STATUS_ON_TIME,
                checkOutStatus = outStatus,
                biometricConfidence = confidence
            )
        }

        if (existing != null) {
            attendanceDao.update(record)
        } else {
            attendanceDao.insert(record)
        }

        // Send to Firestore attendance collection
        pushAttendanceToFirestore(
            employeeId = employeeId,
            date = todayStr,
            type = "CHECK_OUT",
            timestamp = now,
            status = outStatus,
            locationName = record.locationName,
            distanceMeters = record.distanceMeters,
            confidence = confidence,
            notes = "Checkout: $outStatus"
        )

        return record
    }

    private fun pushAttendanceToFirestore(
        employeeId: String,
        date: String,
        type: String,
        timestamp: Long,
        status: String,
        locationName: String,
        distanceMeters: Double,
        confidence: Float,
        notes: String?
    ) {
        scope.launch {
            try {
                val emp = employeeDao.getEmployeeById(employeeId)
                val employeeName = emp?.name ?: "Karyawan"
                val department = emp?.department ?: "-"

                // Payload with both English and Indonesian field names for maximum dashboard compatibility
                val payload = hashMapOf(
                    "employeeId" to employeeId,
                    "employeeName" to employeeName,
                    "name" to employeeName,
                    "nama" to employeeName,
                    "nip" to (emp?.nik ?: ""),
                    "department" to department,
                    "divisi" to department,
                    "date" to date,
                    "tanggal" to date,
                    "type" to type,
                    "tipe" to type,
                    "timestamp" to timestamp,
                    "time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
                    "status" to status,
                    "locationName" to locationName,
                    "location" to locationName,
                    "distanceMeters" to distanceMeters,
                    "biometricConfidence" to confidence,
                    "notes" to (notes ?: "-"),
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Write document with timestamp-based ID or let Firestore generate it
                firestore.collection("attendance").add(payload)
                    .addOnSuccessListener { docRef ->
                        Log.i("AttendanceRepository", "Attendance saved to Firestore successfully: ${docRef.id}")
                        _syncInfo.value = _syncInfo.value.copy(
                            isConnected = true,
                            lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                            syncStatusMessage = "Presensi terkirim ke Web Dashboard"
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("AttendanceRepository", "Firestore write failed: ${e.message}", e)
                        _syncInfo.value = _syncInfo.value.copy(
                            syncStatusMessage = "Gagal kirim: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                Log.e("AttendanceRepository", "Error pushing attendance: ${e.message}", e)
            }
        }
    }

    suspend fun submitLeaveRequest(request: LeaveRequest): Long {
        val id = leaveDao.insert(request)

        scope.launch {
            try {
                val payload = hashMapOf(
                    "id" to id.toString(),
                    "employeeId" to request.employeeId,
                    "employeeName" to request.employeeName,
                    "nama" to request.employeeName,
                    "department" to request.employeeDepartment,
                    "leaveType" to request.leaveType,
                    "tipeCuti" to request.leaveType,
                    "startDate" to request.startDate,
                    "endDate" to request.endDate,
                    "durationDays" to request.durationDays,
                    "jumlahHari" to request.durationDays,
                    "reason" to request.reason,
                    "alasan" to request.reason,
                    "emergencyContact" to request.emergencyContact,
                    "status" to LeaveRequest.STATUS_PENDING,
                    "submissionTimestamp" to request.submissionTimestamp,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("leave_requests").document(id.toString()).set(payload)
                    .addOnSuccessListener {
                        Log.i("AttendanceRepository", "Leave request synced to Firestore: $id")
                    }
            } catch (e: Exception) {
                Log.w("AttendanceRepository", "Firestore leave sync warning: ${e.message}")
            }
        }
        return id
    }

    suspend fun reviewLeaveRequest(
        requestId: Long,
        employeeId: String,
        daysCount: Int,
        approve: Boolean,
        notes: String?
    ) {
        val status = if (approve) LeaveRequest.STATUS_APPROVED else LeaveRequest.STATUS_REJECTED
        val now = System.currentTimeMillis()
        leaveDao.updateStatus(requestId, status, notes, now)

        if (approve) {
            employeeDao.incrementUsedLeave(employeeId, daysCount)
        }

        scope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "status" to status,
                    "reviewerNotes" to (notes ?: ""),
                    "reviewedTimestamp" to now
                )
                firestore.collection("leave_requests").document(requestId.toString()).update(updates)
            } catch (e: Exception) {
                Log.w("AttendanceRepository", "Firestore leave update warning: ${e.message}")
            }
        }
    }

    suspend fun updateEmployeeFace(employeeId: String, photoUri: String) {
        val emp = employeeDao.getEmployeeById(employeeId)
        if (emp != null) {
            employeeDao.update(emp.copy(faceEnrolled = true, referenceFacePath = photoUri))
            scope.launch {
                try {
                    firestore.collection("employees").document(employeeId).update(
                        mapOf("faceEnrolled" to true)
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun forceRefresh() {
        setupFirestoreRealtimeSync()
    }

    private fun setupFirestoreRealtimeSync() {
        scope.launch {
            try {
                _syncInfo.value = _syncInfo.value.copy(
                    syncStatusMessage = "Menghubungkan ke database..."
                )

                // 1. REAL-TIME LISTENER FOR EMPLOYEES COLLECTION (and fallback to 'karyawan')
                val listenerHandler: (com.google.firebase.firestore.QuerySnapshot?, com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit = { snapshot, err ->
                    if (err != null) {
                        Log.e("AttendanceRepository", "Error listening to employees: ${err.message}")
                        _syncInfo.value = _syncInfo.value.copy(
                            isConnected = false,
                            syncStatusMessage = "Koneksi terhambat: ${err.message}"
                        )
                    } else if (snapshot != null && !snapshot.isEmpty) {
                        Log.i("AttendanceRepository", "Received ${snapshot.size()} employees from Firestore")
                        val remoteEmployees = snapshot.documents.mapNotNull { doc ->
                            mapDocToEmployee(doc)
                        }

                        if (remoteEmployees.isNotEmpty()) {
                            scope.launch {
                                // Replace dummy employees with real Firestore employees!
                                employeeDao.clearAll()
                                employeeDao.insertAll(remoteEmployees)
                            }
                            _syncInfo.value = _syncInfo.value.copy(
                                isConnected = true,
                                totalEmployeesSynced = remoteEmployees.size,
                                lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                syncStatusMessage = "Terhubung: ${remoteEmployees.size} karyawan dari Web Dashboard"
                            )
                        }
                    }
                }

                firestore.collection("employees").addSnapshotListener(listenerHandler)
                firestore.collection("karyawan").addSnapshotListener(listenerHandler)

                // 2. REAL-TIME LISTENER FOR ATTENDANCE COLLECTION
                firestore.collection("attendance")
                    .limit(100)
                    .addSnapshotListener { snapshot, err ->
                        if (err != null) {
                            Log.w("AttendanceRepository", "Attendance listener warning: ${err.message}")
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            Log.i("AttendanceRepository", "Received ${snapshot.size()} attendance records from Firestore")
                            val remoteAttendance = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val empId = doc.getSafeString("employeeId", "userId", "idKaryawan", "nip", "id")
                                        ?: return@mapNotNull null
                                    val date = doc.getSafeString("date", "tanggal")
                                        ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    val ts = doc.getSafeLong("timestamp", "createdAt", "waktu", "timeMillis", "time")
                                        ?: System.currentTimeMillis()
                                    val type = doc.getSafeString("type", "tipe", "jenisPresensi", "statusPresensi")?.uppercase(Locale.getDefault()) ?: "CHECK_IN"
                                    val status = doc.getSafeString("status", "keterangan") ?: AttendanceRecord.STATUS_ON_TIME
                                    val loc = doc.getSafeString("locationName", "location", "lokasi") ?: "Kantor Pusat"
                                    val dist = doc.getSafeDouble("distanceMeters", "distance", "jarak") ?: 12.0
                                    val conf = (doc.getSafeDouble("biometricConfidence", "confidence", "skorWajah") ?: 98.8).toFloat()
                                    val notes = doc.getSafeString("notes", "catatan")

                                    AttendanceRecord(
                                        employeeId = empId,
                                        date = date,
                                        checkInTime = if (type.contains("IN") || type == "MASUK") ts else null,
                                        checkOutTime = if (type.contains("OUT") || type == "KELUAR" || type == "PULANG") ts else null,
                                        checkInStatus = status,
                                        locationName = loc,
                                        distanceMeters = dist,
                                        biometricConfidence = conf,
                                        notes = notes
                                    )
                                } catch (e: Exception) {
                                    Log.e("AttendanceRepository", "Error parsing attendance doc ${doc.id}: ${e.message}")
                                    null
                                }
                            }

                            if (remoteAttendance.isNotEmpty()) {
                                scope.launch {
                                    remoteAttendance.forEach { attendanceDao.insert(it) }
                                }
                                _syncInfo.value = _syncInfo.value.copy(
                                    isConnected = true,
                                    totalAttendanceSynced = remoteAttendance.size,
                                    lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                )
                            }
                        }
                    }

                // 3. REAL-TIME LISTENER FOR LEAVE_REQUESTS COLLECTION
                firestore.collection("leave_requests").addSnapshotListener { snapshot, err ->
                    if (err != null || snapshot == null) return@addSnapshotListener
                    val remoteLeaves = snapshot.documents.mapNotNull { doc ->
                        try {
                            val empId = doc.getSafeString("employeeId", "userId", "idKaryawan") ?: return@mapNotNull null
                            val empName = doc.getSafeString("employeeName", "nama", "name") ?: "Karyawan"
                            val localId = doc.getSafeLong("id", "requestId") ?: (doc.id.hashCode().toLong().let { if (it < 0) -it else it })
                            LeaveRequest(
                                id = localId,
                                employeeId = empId,
                                employeeName = empName,
                                employeeDepartment = doc.getSafeString("department", "employeeDepartment", "divisi", "departemen") ?: "-",
                                leaveType = doc.getSafeString("leaveType", "tipeCuti", "jenisCuti") ?: LeaveRequest.TYPE_ANNUAL,
                                startDate = doc.getSafeString("startDate", "tanggalMulai", "tglMulai") ?: "",
                                endDate = doc.getSafeString("endDate", "tanggalSelesai", "tglSelesai") ?: "",
                                durationDays = doc.getSafeInt("durationDays", "jumlahHari", "durasi") ?: 1,
                                reason = doc.getSafeString("reason", "alasan", "keterangan") ?: "-",
                                emergencyContact = doc.getSafeString("emergencyContact", "kontakDarurat", "noHpDarurat") ?: "-",
                                status = doc.getSafeString("status") ?: LeaveRequest.STATUS_PENDING,
                                submissionTimestamp = doc.getSafeLong("submissionTimestamp", "createdAt", "timestamp") ?: System.currentTimeMillis(),
                                reviewerNotes = doc.getSafeString("reviewerNotes", "catatanReviewer", "catatanAdmin"),
                                reviewedTimestamp = doc.getSafeLong("reviewedTimestamp", "waktuReview")
                            )
                        } catch (e: Exception) {
                            Log.e("AttendanceRepository", "Error parsing leave doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    if (remoteLeaves.isNotEmpty()) {
                        scope.launch {
                            remoteLeaves.forEach { leaveDao.insert(it) }
                        }
                    }
                }

                // 4. REAL-TIME LISTENER FOR SETTINGS (company_config)
                firestore.collection("settings").document("company_config").addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        try {
                            val name = doc.getSafeString("officeName", "namaKantor", "nama") ?: "Kantor Pusat Menara Mandiri, Jakarta"
                            val lat = doc.getSafeDouble("officeLatitude", "latitude", "lat") ?: -6.2255
                            val lng = doc.getSafeDouble("officeLongitude", "longitude", "lng") ?: 106.8080
                            val radius = doc.getSafeDouble("geofenceRadiusMeters", "radiusMeters", "radius") ?: 100.0
                            val start = doc.getSafeString("workStartTime", "jamMasuk") ?: "08:00"
                            val end = doc.getSafeString("workEndTime", "jamPulang") ?: "17:00"

                            _officeConfig.value = OfficeConfig(
                                name = name,
                                latitude = lat,
                                longitude = lng,
                                radiusMeters = radius,
                                startTime = start,
                                endTime = end
                            )
                        } catch (e: Exception) {
                            Log.e("AttendanceRepository", "Error parsing company_config: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AttendanceRepository", "Fatal sync error: ${e.message}", e)
                _syncInfo.value = _syncInfo.value.copy(
                    isConnected = false,
                    syncStatusMessage = "Gagal inisialisasi: ${e.message}"
                )
            }
        }
    }

    private fun seedInitialEmployeesToFirestore() {
        scope.launch {
            try {
                val initialStaff = listOf(
                    hashMapOf(
                        "id" to "EMP-001",
                        "employeeId" to "EMP-001",
                        "name" to "Dony Setyawan",
                        "nip" to "199408122020121001",
                        "nik" to "199408122020121001",
                        "position" to "Senior Android Specialist",
                        "department" to "Technology & Core Engineering",
                        "email" to "dony.setyawan@perusahaan.co.id",
                        "phoneNumber" to "+62 812-3456-7890",
                        "annualLeaveQuota" to 12,
                        "usedLeaveQuota" to 2,
                        "faceEnrolled" to true,
                        "role" to Employee.ROLE_EMPLOYEE
                    ),
                    hashMapOf(
                        "id" to "EMP-002",
                        "employeeId" to "EMP-002",
                        "name" to "Siti Rahmawati, S.Psi",
                        "nip" to "199203152018032002",
                        "nik" to "199203152018032002",
                        "position" to "Head of People & HR Operations",
                        "department" to "Human Resources & Talent",
                        "email" to "siti.rahma@perusahaan.co.id",
                        "phoneNumber" to "+62 811-9876-5432",
                        "annualLeaveQuota" to 14,
                        "usedLeaveQuota" to 1,
                        "faceEnrolled" to true,
                        "role" to Employee.ROLE_HR_MANAGER
                    ),
                    hashMapOf(
                        "id" to "EMP-003",
                        "employeeId" to "EMP-003",
                        "name" to "Dimas Prasetyo",
                        "nip" to "199611202021081003",
                        "nik" to "199611202021081003",
                        "position" to "UI/UX Product Designer",
                        "department" to "Product & Design",
                        "email" to "dimas.prasetyo@perusahaan.co.id",
                        "phoneNumber" to "+62 856-7788-9900",
                        "annualLeaveQuota" to 12,
                        "usedLeaveQuota" to 4,
                        "faceEnrolled" to true,
                        "role" to Employee.ROLE_EMPLOYEE
                    )
                )

                initialStaff.forEach { emp ->
                    val id = emp["id"] as String
                    firestore.collection("employees").document(id).set(emp)
                }

                // Seed initial company_config
                val config = hashMapOf(
                    "officeName" to "Kantor Pusat Menara Mandiri, Jakarta",
                    "officeLatitude" to -6.2255,
                    "officeLongitude" to 106.8080,
                    "geofenceRadiusMeters" to 100.0,
                    "workStartTime" to "08:00",
                    "workEndTime" to "17:00",
                    "lateToleranceMinutes" to 30
                )
                firestore.collection("settings").document("company_config").set(config)

                Log.i("AttendanceRepository", "Initial staff and settings seeded to Firestore database")
            } catch (e: Exception) {
                Log.w("AttendanceRepository", "Seeding skipped: ${e.message}")
            }
        }
    }

    private fun mapDocToEmployee(doc: DocumentSnapshot): Employee? {
        return try {
            val docId = doc.id
            val id = doc.getSafeString("id", "employeeId", "nip", "idKaryawan") ?: docId
            val name = doc.getSafeString(
                "name",
                "employeeName",
                "nama",
                "fullName",
                "namaLengkap",
                "userName"
            ) ?: return null

            val nip = doc.getSafeString("nip", "nik", "nomorInduk", "idNumber") ?: id
            val position = doc.getSafeString("position", "jabatan", "posisi", "roleTitle") ?: "Staf"
            val department = doc.getSafeString("department", "departemen", "divisi", "division") ?: "Operasional"
            val email = doc.getSafeString("email", "surel") ?: ""
            val phone = doc.getSafeString("phoneNumber", "telepon", "phone", "noHp") ?: ""

            val annualLeave = doc.getSafeInt("annualLeaveQuota", "kuotaCuti", "leaveQuota", "cutiTahunan") ?: 12
            val usedLeave = doc.getSafeInt("usedLeaveQuota", "cutiTerpakai", "usedQuota") ?: 0
            val role = doc.getSafeString("role") ?: Employee.ROLE_EMPLOYEE
            val faceEnrolled = doc.getSafeBoolean("faceEnrolled", "wajahTerdaftar", "biometricEnrolled", default = true)

            Employee(
                id = id,
                name = name,
                nik = nip,
                position = position,
                department = department,
                email = email,
                phoneNumber = phone,
                annualLeaveQuota = annualLeave,
                usedLeaveQuota = usedLeave,
                faceEnrolled = faceEnrolled,
                role = role
            )
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error mapping employee doc ${doc.id}: ${e.message}")
            null
        }
    }
}

// -------------------------------------------------------------------------------------
// SAFE DOCUMENT SNAPSHOT EXTENSIONS TO PREVENT CLASS-CAST / RUNTIME EXCEPTIONS
// -------------------------------------------------------------------------------------

private fun DocumentSnapshot.getSafeRaw(field: String): Any? {
    return try {
        if (contains(field)) get(field) else null
    } catch (_: Exception) {
        null
    }
}

private fun DocumentSnapshot.getSafeString(vararg fields: String): String? {
    for (f in fields) {
        val raw = getSafeRaw(f) ?: continue
        when (raw) {
            is String -> if (raw.isNotBlank()) return raw
            is Number -> return raw.toString()
            is Boolean -> return raw.toString()
            is com.google.firebase.Timestamp -> {
                return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(raw.toDate())
            }
            is Date -> {
                return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(raw)
            }
        }
    }
    return null
}

private fun DocumentSnapshot.getSafeLong(vararg fields: String): Long? {
    for (f in fields) {
        val raw = getSafeRaw(f) ?: continue
        when (raw) {
            is com.google.firebase.Timestamp -> return raw.toDate().time
            is Number -> return raw.toLong()
            is Date -> return raw.time
            is String -> {
                val asLong = raw.toLongOrNull()
                if (asLong != null) return asLong
                val asDouble = raw.toDoubleOrNull()
                if (asDouble != null) return asDouble.toLong()

                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
                )
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                        val parsed = sdf.parse(raw)
                        if (parsed != null) return parsed.time
                    } catch (_: Exception) {}
                }
            }
        }
    }
    return null
}

private fun DocumentSnapshot.getSafeDouble(vararg fields: String): Double? {
    for (f in fields) {
        val raw = getSafeRaw(f) ?: continue
        when (raw) {
            is Number -> return raw.toDouble()
            is String -> {
                val d = raw.toDoubleOrNull()
                if (d != null) return d
            }
        }
    }
    return null
}

private fun DocumentSnapshot.getSafeInt(vararg fields: String): Int? {
    return getSafeLong(*fields)?.toInt()
}

private fun DocumentSnapshot.getSafeBoolean(vararg fields: String, default: Boolean = false): Boolean {
    for (f in fields) {
        val raw = getSafeRaw(f) ?: continue
        when (raw) {
            is Boolean -> return raw
            is Number -> return raw.toInt() != 0
            is String -> {
                if (raw.equals("true", ignoreCase = true) || raw == "1" || raw.equals("aktif", ignoreCase = true)) return true
                if (raw.equals("false", ignoreCase = true) || raw == "0") return false
            }
        }
    }
    return default
}

