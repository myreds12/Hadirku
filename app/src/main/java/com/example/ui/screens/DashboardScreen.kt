package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.LeaveRequest
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.LateOrange
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SuccessGreenOnContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    employee: Employee?,
    allEmployees: List<Employee>,
    todayAttendance: AttendanceRecord?,
    currentTime: String,
    currentDateFull: String,
    officeName: String,
    officeDistance: Double,
    isWithinOffice: Boolean,
    pendingLeaves: List<LeaveRequest>,
    syncInfo: com.example.data.repository.CloudSyncInfo,
    onRefreshSync: () -> Unit,
    onSelectEmployee: (String) -> Unit,
    onNavigateToFaceScan: (isCheckIn: Boolean) -> Unit,
    onNavigateToLeaveRequest: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHrApproval: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEmployeeMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val hasCheckedIn = todayAttendance?.checkInTime != null
    val hasCheckedOut = todayAttendance?.checkOutTime != null

    val checkInFormatted = todayAttendance?.checkInTime?.let {
        SimpleDateFormat("HH:mm WIB", Locale.getDefault()).format(Date(it))
    } ?: "--:--"

    val checkOutFormatted = todayAttendance?.checkOutTime?.let {
        SimpleDateFormat("HH:mm WIB", Locale.getDefault()).format(Date(it))
    } ?: "--:--"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Employee Header & Profile Switcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = employee?.name?.take(2)?.uppercase() ?: "US",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = employee?.name ?: "Karyawan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${employee?.nik} • ${employee?.position}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        // Employee Switcher Button
                        Box {
                            OutlinedButton(
                                onClick = { showEmployeeMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("switch_employee_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Ganti Profil",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (employee?.role == Employee.ROLE_HR_MANAGER) "HRD" else "Staf",
                                    fontSize = 12.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showEmployeeMenu,
                                onDismissRequest = { showEmployeeMenu = false }
                            ) {
                                allEmployees.forEach { emp ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = emp.name,
                                                    fontWeight = if (emp.id == employee?.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "${emp.position} (${emp.role})",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectEmployee(emp.id)
                                            showEmployeeMenu = false
                                        }
                                    )
                                }
                                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Keluar (Logout)",
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    },
                                    onClick = {
                                        showEmployeeMenu = false
                                        showLogoutDialog = true
                                    },
                                    modifier = Modifier.testTag("dashboard_menu_logout_item")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Office Geofence Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isWithinOffice) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isWithinOffice) Color(0xFFBBF7D0) else Color(0xFFFECACA),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Lokasi",
                            tint = if (isWithinOffice) SuccessGreen else Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWithinOffice) {
                                "Di Area Kantor (${officeDistance.toInt()}m) • $officeName"
                            } else {
                                "Di Luar Radius Kantor (${officeDistance.toInt()}m)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isWithinOffice) SuccessGreenOnContainer else Color(0xFF991B1B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Cloud Sync Status Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (syncInfo.isConnected) Color(0xFFEFF6FF) else Color(0xFFFEF3C7))
                            .border(
                                width = 1.dp,
                                color = if (syncInfo.isConnected) Color(0xFFBFDBFE) else Color(0xFFFDE68A),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (syncInfo.isConnected) Icons.Default.CheckCircle else Icons.Default.History,
                            contentDescription = "Cloud Sync",
                            tint = if (syncInfo.isConnected) Color(0xFF2563EB) else Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (syncInfo.isConnected) {
                                    "Firestore Aktif (${syncInfo.totalEmployeesSynced} Karyawan tersinkron)"
                                } else {
                                    "Menghubungkan ke Web Dashboard..."
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncInfo.isConnected) Color(0xFF1E40AF) else Color(0xFF92400E)
                            )
                            Text(
                                text = syncInfo.syncStatusMessage,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(
                            onClick = onRefreshSync,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Sinkronkan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Real-Time Clock & Attendance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentDateFull,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Jam Kerja Reguler: 08:00 - 17:00 WIB",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Daily Check-in & Check-out Status Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Check-in status
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = "Jam Masuk",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Jam Masuk",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = checkInFormatted,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        !hasCheckedIn -> "Belum Absen"
                                        todayAttendance?.checkInStatus == AttendanceRecord.STATUS_LATE -> "Terlambat"
                                        else -> "Tepat Waktu"
                                    },
                                    fontSize = 11.sp,
                                    color = when {
                                        !hasCheckedIn -> Color.White.copy(alpha = 0.7f)
                                        todayAttendance?.checkInStatus == AttendanceRecord.STATUS_LATE -> Color(0xFFFDBA74)
                                        else -> Color(0xFF86EFAC)
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Check-out status
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Jam Pulang",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Jam Pulang",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = checkOutFormatted,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hasCheckedOut) "Tercatat Pulang" else "Belum Pulang",
                                    fontSize = 11.sp,
                                    color = if (hasCheckedOut) Color(0xFF86EFAC) else Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Big Attendance CTA Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToFaceScan(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("check_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!hasCheckedIn) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (hasCheckedIn) "Absen Ulang" else "Absen Masuk", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigateToFaceScan(false) },
                            enabled = hasCheckedIn && !hasCheckedOut,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("check_out_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                disabledContainerColor = Color.White.copy(alpha = 0.15f),
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Absen Pulang", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Quick Action Grid (Pengajuan Cuti, Riwayat, Approval HRD)
        item {
            Text(
                text = "Layanan Mandiri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action 1: Ajukan Cuti
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLeaveRequest() }
                        .testTag("apply_leave_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Ajukan Cuti",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ajukan Cuti",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tahunan / Sakit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action 2: Riwayat Kehadiran & Cuti
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHistory() }
                        .testTag("attendance_history_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Riwayat",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Riwayat Presensi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Log & Foto Selfie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action 3: HR Approval Panel (Special Access for HR / Supervisor)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHrApproval() }
                    .testTag("hr_approval_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (employee?.role == Employee.ROLE_HR_MANAGER) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgedBox(
                        badge = {
                            if (pendingLeaves.isNotEmpty()) {
                                Badge {
                                    Text(text = pendingLeaves.size.toString())
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmberTertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = "Approval HRD",
                                tint = AmberTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persetujuan Cuti (HRD & Manajer)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pendingLeaves.isNotEmpty()) {
                                "${pendingLeaves.size} pengajuan cuti menunggu persetujuan Anda"
                            } else {
                                "Semua pengajuan cuti karyawan telah diproses"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = AmberTertiary
                    )
                }
            }
        }

        // 4. Leave Quota & Attendance Summary Metrics
        item {
            Text(
                text = "Ringkasan Kehadiran & Cuti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Sisa Cuti
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Sisa Cuti",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${employee?.remainingLeave ?: 0} Hari",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "dari ${employee?.annualLeaveQuota ?: 12} hari/thn",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Metric 2: Kehadiran Bulan Ini
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Hadir Bulan Ini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "19 Hari",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Target 22 hari kerja",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Metric 3: Terlambat
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Terlambat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "0x",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LateOrange
                        )
                        Text(
                            text = "Disiplin kerja 100%",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Konfirmasi Keluar",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin keluar dari akun '${employee?.name ?: "Karyawan"}'? Sesi Anda akan diakhiri dan dialihkan ke layar Login."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("dashboard_confirm_logout_button")
                ) {
                    Text("Keluar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showLogoutDialog = false },
                    modifier = Modifier.testTag("dashboard_cancel_logout_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
