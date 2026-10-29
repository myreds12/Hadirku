package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Employee
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FaceScanScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HrApprovalScreen
import com.example.ui.screens.LeaveRequestScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.HadirKuTheme
import com.example.ui.viewmodel.AttendanceViewModel

enum class AppDestination {
    SPLASH,
    LOGIN,
    DASHBOARD,
    FACE_SCAN,
    LEAVE_REQUEST,
    HISTORY,
    HR_APPROVAL,
    PROFILE
}

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HadirKuTheme {
                HadirKuApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HadirKuApp(viewModel: AttendanceViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }
    var isCheckInScan by remember { mutableStateOf(true) }
    var isFaceEnrollmentScan by remember { mutableStateOf(false) }

    val employee by viewModel.currentEmployee.collectAsStateWithLifecycle()
    val allEmployees by viewModel.employees.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val attendanceHistory by viewModel.attendanceHistory.collectAsStateWithLifecycle()
    val leaveRequests by viewModel.leaveRequests.collectAsStateWithLifecycle()
    val allLeaves by viewModel.allLeaveRequests.collectAsStateWithLifecycle()
    val pendingLeaves by viewModel.pendingLeaves.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val currentDateFull by viewModel.currentDateFull.collectAsStateWithLifecycle()
    val officeDistance by viewModel.officeDistanceMeters.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val syncInfo by viewModel.syncInfo.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    val isTopLevelScreen = currentDestination == AppDestination.DASHBOARD ||
            currentDestination == AppDestination.LEAVE_REQUEST ||
            currentDestination == AppDestination.HISTORY ||
            currentDestination == AppDestination.PROFILE ||
            currentDestination == AppDestination.HR_APPROVAL

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.DASHBOARD,
                        onClick = { currentDestination = AppDestination.DASHBOARD },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda", modifier = Modifier.size(22.dp)) },
                        label = { Text("Beranda", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.LEAVE_REQUEST,
                        onClick = { currentDestination = AppDestination.LEAVE_REQUEST },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Cuti", modifier = Modifier.size(22.dp)) },
                        label = { Text("Cuti", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_leave")
                    )
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.HISTORY,
                        onClick = { currentDestination = AppDestination.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = "Riwayat", modifier = Modifier.size(22.dp)) },
                        label = { Text("Riwayat", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_history")
                    )
                    if (employee?.role == Employee.ROLE_HR_MANAGER) {
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.HR_APPROVAL,
                            onClick = { currentDestination = AppDestination.HR_APPROVAL },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (pendingLeaves.isNotEmpty()) {
                                            Badge { Text(pendingLeaves.size.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Approval", modifier = Modifier.size(22.dp))
                                }
                            },
                            label = { Text("Approval", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_approval")
                        )
                    }
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.PROFILE,
                        onClick = { currentDestination = AppDestination.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil", modifier = Modifier.size(22.dp)) },
                        label = { Text("Profil", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = currentDestination,
                label = "destination_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { destination ->
                when (destination) {
                    AppDestination.SPLASH -> {
                        SplashScreen(
                            onSplashFinished = {
                                if (isLoggedIn) {
                                    currentDestination = AppDestination.DASHBOARD
                                } else {
                                    currentDestination = AppDestination.LOGIN
                                }
                            }
                        )
                    }

                    AppDestination.LOGIN -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val activity = context as? android.app.Activity
                        LoginScreen(
                            employees = allEmployees,
                            onLoginWithEmployeeId = { empId ->
                                viewModel.loginWithEmployeeId(empId)
                                currentDestination = AppDestination.DASHBOARD
                            },
                            onLoginWithCredentials = { idOrEmail, pin, onSuccess, onError ->
                                viewModel.loginWithCredentials(
                                    identifier = idOrEmail,
                                    pin = pin,
                                    onSuccess = { name ->
                                        onSuccess(name)
                                        currentDestination = AppDestination.DASHBOARD
                                    },
                                    onError = onError
                                )
                            },
                            onGoogleSignInClicked = { act, onSuccess, onError ->
                                viewModel.signInWithGoogle(
                                    activity = act,
                                    onSuccess = {
                                        val googleUser = viewModel.currentUser.value
                                        val matched = allEmployees.find { it.email.equals(googleUser?.email, ignoreCase = true) }
                                            ?: allEmployees.firstOrNull()
                                        if (matched != null) {
                                            viewModel.loginWithEmployeeId(matched.id)
                                        }
                                        onSuccess()
                                        currentDestination = AppDestination.DASHBOARD
                                    },
                                    onError = onError
                                )
                            }
                        )
                    }
                    AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            employee = employee,
                            allEmployees = allEmployees,
                            todayAttendance = todayAttendance,
                            currentTime = currentTime,
                            currentDateFull = currentDateFull,
                            officeName = viewModel.officeName,
                            officeDistance = officeDistance,
                            isWithinOffice = viewModel.isWithinOfficeRadius,
                            pendingLeaves = pendingLeaves,
                            syncInfo = syncInfo,
                            onRefreshSync = { viewModel.refreshCloudSync() },
                            onSelectEmployee = { id -> viewModel.selectEmployee(id) },
                            onNavigateToFaceScan = { isCheckIn ->
                                isCheckInScan = isCheckIn
                                isFaceEnrollmentScan = false
                                currentDestination = AppDestination.FACE_SCAN
                            },
                            onNavigateToLeaveRequest = { currentDestination = AppDestination.LEAVE_REQUEST },
                            onNavigateToHistory = { currentDestination = AppDestination.HISTORY },
                            onNavigateToHrApproval = { currentDestination = AppDestination.HR_APPROVAL },
                            onNavigateToProfile = { currentDestination = AppDestination.PROFILE },
                            onLogout = {
                                viewModel.logout {
                                    currentDestination = AppDestination.LOGIN
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    AppDestination.FACE_SCAN -> {
                        FaceScanScreen(
                            isCheckIn = isCheckInScan,
                            isEnrollment = isFaceEnrollmentScan,
                            employee = employee,
                            scanState = scanState,
                            currentTime = currentTime,
                            officeName = viewModel.officeName,
                            officeDistance = officeDistance,
                            isWithinOffice = viewModel.isWithinOfficeRadius,
                            onStartScan = { viewModel.startFaceScan(isCheckInScan, isFaceEnrollmentScan) },
                            onConfirmAttendance = { photoPath ->
                                viewModel.confirmAttendanceWithPhoto(photoPath) {
                                    currentDestination = AppDestination.DASHBOARD
                                }
                            },
                            onEnrollFace = { photoPath ->
                                viewModel.enrollEmployeeFace(photoPath) {
                                    currentDestination = AppDestination.PROFILE
                                }
                            },
                            onCancelScan = { viewModel.cancelScan() },
                            onNavigateBack = {
                                if (isFaceEnrollmentScan) {
                                    currentDestination = AppDestination.PROFILE
                                } else {
                                    currentDestination = AppDestination.DASHBOARD
                                }
                            }
                        )
                    }

                    AppDestination.LEAVE_REQUEST -> {
                        LeaveRequestScreen(
                            employee = employee,
                            onSubmitLeave = { type, start, end, days, reason, emergency, onSuccess ->
                                viewModel.submitLeaveRequest(type, start, end, days, reason, emergency) {
                                    onSuccess()
                                    currentDestination = AppDestination.HISTORY
                                }
                            },
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    AppDestination.HISTORY -> {
                        HistoryScreen(
                            attendanceHistory = attendanceHistory,
                            leaveRequests = leaveRequests,
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    AppDestination.HR_APPROVAL -> {
                        HrApprovalScreen(
                            pendingLeaves = pendingLeaves,
                            allLeaves = allLeaves,
                            onReviewLeave = { reqId, empId, days, approve, notes ->
                                viewModel.reviewLeaveRequest(reqId, empId, days, approve, notes)
                            },
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    AppDestination.PROFILE -> {
                        ProfileScreen(
                            employee = employee,
                            allEmployees = allEmployees,
                            onSelectEmployee = { id -> viewModel.selectEmployee(id) },
                            onRefreshSync = { viewModel.refreshCloudSync() },
                            onNavigateToFaceEnrollment = {
                                isCheckInScan = true
                                isFaceEnrollmentScan = true
                                currentDestination = AppDestination.FACE_SCAN
                            },
                            onNavigateBack = { currentDestination = AppDestination.DASHBOARD },
                            onLogout = {
                                viewModel.logout {
                                    currentDestination = AppDestination.LOGIN
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
