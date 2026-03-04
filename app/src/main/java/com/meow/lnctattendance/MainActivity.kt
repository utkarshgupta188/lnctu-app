package com.meow.lnctattendance

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meow.lnctattendance.data.AttendanceData
import com.meow.lnctattendance.data.AnalysisData
import com.meow.lnctattendance.data.RiskEngineData
import com.meow.lnctattendance.data.LeaveSimulatorData
import com.meow.lnctattendance.data.WeekSimulatorData
import com.meow.lnctattendance.data.TimetableData
import com.meow.lnctattendance.prefs.AuthState
import com.meow.lnctattendance.prefs.PreferencesManager
import com.meow.lnctattendance.ui.components.ErrorScreen
import com.meow.lnctattendance.ui.components.LoadingScreen
import com.meow.lnctattendance.ui.screens.*
import com.meow.lnctattendance.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            val systemDark = isSystemInDarkTheme()
            val darkModePref by prefs.darkMode.collectAsStateWithLifecycle(initialValue = null)
            val isDark = darkModePref ?: systemDark

            LNCTAttendanceTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AttendanceApp(prefs = prefs, isDark = isDark)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Root app
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttendanceApp(
    vm: AttendanceViewModel = viewModel(),
    prefs: PreferencesManager,
    isDark: Boolean,
) {
    val scope = rememberCoroutineScope()

    val authState       by prefs.authState.collectAsStateWithLifecycle(initialValue = AuthState.Loading)
    val credentials     by vm.credentials.collectAsStateWithLifecycle()
    val attendanceState by vm.attendanceState.collectAsStateWithLifecycle()
    val lastLoginEvent  by vm.lastLoginEvent.collectAsStateWithLifecycle()

    var showLogin            by remember { mutableStateOf(false) }
    var initialLoadAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (!initialLoadAttempted && authState is AuthState.Authenticated) {
            val login = (authState as AuthState.Authenticated).login
            vm.login(login.username, login.password)
            initialLoadAttempted = true
        } else if (authState is AuthState.None) {
            showLogin = true
            initialLoadAttempted = true
        }
    }

    LaunchedEffect(lastLoginEvent) {
        lastLoginEvent?.let { (_, user, pass) ->
            scope.launch { prefs.saveLastLogin("", user, pass, System.currentTimeMillis()) }
        }
    }

    LaunchedEffect(attendanceState) {
        if (initialLoadAttempted && attendanceState is UiState.Idle && credentials == null) {
            showLogin = true
        }
    }

    val onToggleDark: () -> Unit = {
        scope.launch { prefs.setDarkMode(!isDark) }
    }

    if (authState is AuthState.Loading && !initialLoadAttempted) return

    if (showLogin) {
        LoginScreen(
            savedUsername = (authState as? AuthState.Authenticated)?.login?.username ?: "",
            isDark        = isDark,
            onToggleDark  = onToggleDark,
            onLogin       = { user, pass ->
                showLogin = false
                vm.login(user, pass)
            },
        )
    } else {
        MainNavigation(
            vm           = vm,
            username     = credentials?.first
                ?: (authState as? AuthState.Authenticated)?.login?.username ?: "",
            isDark       = isDark,
            onToggleDark = onToggleDark,
            onLogout     = {
                scope.launch {
                    prefs.clear()
                    vm.logout()
                    showLogin = true
                }
            },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Login Screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(
    savedUsername: String,
    isDark: Boolean,
    onToggleDark: () -> Unit,
    onLogin: (String, String) -> Unit,
) {
    var username by remember { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    // Load the round mipmap as a Bitmap via ResourcesCompat (works for any drawable/mipmap type)
    val context = LocalContext.current
    val iconPainter = remember {
        val drawable = ResourcesCompat.getDrawable(
            context.resources,
            R.mipmap.ic_launcher_round,
            context.theme,
        )
        if (drawable is BitmapDrawable) {
            BitmapPainter(drawable.bitmap.asImageBitmap())
        } else {
            // Fallback: render adaptive drawable to bitmap at 192×192 px
            val bmp = android.graphics.Bitmap.createBitmap(192, 192, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable?.setBounds(0, 0, 192, 192)
            drawable?.draw(canvas)
            BitmapPainter(bmp.asImageBitmap())
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ── Dark-mode toggle (top-right) ──
        IconButton(
            onClick  = onToggleDark,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 12.dp),
        ) {
            Icon(
                imageVector        = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Scrollable login form ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(80.dp))

            // ── App icon — round mipmap loaded as Bitmap, clipped to circle ──
            Image(
                painter            = iconPainter,
                contentDescription = "App Icon",
                modifier           = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
            )

            Spacer(Modifier.height(22.dp))
            Text(
                text       = "LNCT Attendance",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "Sign in to view your attendance",
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))

            OutlinedTextField(
                value           = username,
                onValueChange   = { username = it },
                label           = { Text("Student Username") },
                leadingIcon     = { Icon(Icons.Default.Person, null, tint = Primary) },
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = { Text("Password") },
                leadingIcon          = { Icon(Icons.Default.Lock, null, tint = Primary) },
                trailingIcon         = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPass) "Hide" else "Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                singleLine           = true,
                modifier             = Modifier.fillMaxWidth(),
                shape                = RoundedCornerShape(14.dp),
                visualTransformation = if (showPass) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick  = {
                    if (username.isNotBlank() && password.isNotBlank()) onLogin(username, password)
                },
                enabled  = username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text      = "Credentials are securely stored locally on your device",
                fontSize  = 12.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── Made by credit ──
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Code,
                    contentDescription = null,
                    tint               = Primary.copy(alpha = 0.80f),
                    modifier           = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text     = "Made by ",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text       = "Utkarsh Gupta",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Primary,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Bottom nav destinations
// ──────────────────────────────────────────────────────────────────────────────

private enum class NavDest(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    Home("Home",           Icons.Outlined.Home,          Icons.Filled.Home),
    Analysis("Analysis",   Icons.Outlined.BarChart,      Icons.Filled.BarChart),
    Risk("Risk",           Icons.Outlined.Security,      Icons.Filled.Security),
    Leave("Leave",         Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Timetable("Timetable", Icons.Outlined.ViewModule,    Icons.Filled.ViewModule),
}

// ──────────────────────────────────────────────────────────────────────────────
// Main navigation shell
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainNavigation(
    vm: AttendanceViewModel,
    username: String,
    isDark: Boolean,
    onToggleDark: () -> Unit,
    onLogout: () -> Unit,
) {
    var currentDest by remember { mutableStateOf(NavDest.Home) }

    val attendanceState by vm.attendanceState.collectAsStateWithLifecycle()
    val analysisState   by vm.analysisState.collectAsStateWithLifecycle()
    val riskState       by vm.riskState.collectAsStateWithLifecycle()
    val leaveState      by vm.leaveState.collectAsStateWithLifecycle()
    val weekState       by vm.weekState.collectAsStateWithLifecycle()
    val timetableState  by vm.timetableState.collectAsStateWithLifecycle()

    LaunchedEffect(currentDest) {
        when (currentDest) {
            NavDest.Analysis  -> if (analysisState is UiState.Idle) vm.fetchAnalysis()
            NavDest.Risk      -> if (riskState is UiState.Idle) vm.fetchRiskEngine()
            NavDest.Timetable -> vm.fetchTimetable()
            else              -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(currentDest.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = onToggleDark) {
                        Icon(
                            imageVector        = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                            tint               = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint               = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                NavDest.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDest == dest,
                        onClick  = { currentDest = dest },
                        icon     = {
                            Icon(
                                if (currentDest == dest) dest.iconSelected else dest.icon,
                                contentDescription = dest.label,
                            )
                        },
                        label  = { Text(dest.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor    = Primary.copy(alpha = 0.14f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState    = currentDest,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label          = "nav_anim",
            ) { dest ->
                when (dest) {
                    NavDest.Home -> TabContent(
                        state          = attendanceState,
                        loadingMessage = "Fetching attendance…",
                        onRetry        = { vm.refresh() },
                    ) { data: AttendanceData ->
                        HomeScreen(
                            data        = data,
                            username    = username,
                            studentName = data.studentName,
                            onRefresh   = { vm.refresh() },
                        )
                    }

                    NavDest.Analysis -> TabContent(
                        state          = analysisState,
                        loadingMessage = "Analyzing attendance…",
                        onRetry        = { vm.fetchAnalysis() },
                    ) { data: AnalysisData ->
                        AnalysisScreen(data = data, onRefresh = { vm.fetchAnalysis() })
                    }

                    NavDest.Risk -> TabContent(
                        state          = riskState,
                        loadingMessage = "Calculating risk…",
                        onRetry        = { vm.fetchRiskEngine() },
                    ) { data: RiskEngineData ->
                        RiskScreen(data = data, onRefresh = { vm.fetchRiskEngine() })
                    }

                    NavDest.Leave -> LeaveTabContent(
                        weekState  = weekState,
                        leaveState = leaveState,
                        vm         = vm,
                    )

                    NavDest.Timetable -> TabContent(
                        state          = timetableState,
                        loadingMessage = "Loading timetable…",
                        onRetry        = { vm.fetchTimetable() },
                    ) { data: TimetableData ->
                        TimetableScreen(data = data)
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Generic tab content wrapper
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private inline fun <reified T> TabContent(
    state: UiState<T>,
    loadingMessage: String,
    crossinline onRetry: () -> Unit,
    crossinline content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Idle    -> LoadingScreen(loadingMessage)
        is UiState.Loading -> LoadingScreen(loadingMessage)
        is UiState.Error   -> ErrorScreen(state.message) { onRetry() }
        is UiState.Success -> content(state.data)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Leave tab – combines week + day simulator state
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LeaveTabContent(
    weekState:  UiState<WeekSimulatorData>,
    leaveState: UiState<LeaveSimulatorData>,
    vm: AttendanceViewModel,
) {
    LeaveScreen(
        weekState     = weekState,
        dayState      = leaveState,
        onSimulateDay = { day -> vm.fetchLeaveSimulator(day) },
        onLoadWeek    = { vm.fetchWeekSimulator() },
    )
}
