package com.meow.lnctattendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meow.lnctattendance.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────
// UI state helpers
// ──────────────────────────────────────────────

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// ──────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────

class AttendanceViewModel : ViewModel() {

    // Attendance (home tab)
    private val _attendanceState = MutableStateFlow<UiState<AttendanceData>>(UiState.Idle)
    val attendanceState: StateFlow<UiState<AttendanceData>> = _attendanceState.asStateFlow()

    // Analysis tab
    private val _analysisState = MutableStateFlow<UiState<AnalysisData>>(UiState.Idle)
    val analysisState: StateFlow<UiState<AnalysisData>> = _analysisState.asStateFlow()

    // Risk engine tab
    private val _riskState = MutableStateFlow<UiState<RiskEngineData>>(UiState.Idle)
    val riskState: StateFlow<UiState<RiskEngineData>> = _riskState.asStateFlow()

    // Leave simulator tab – single day
    private val _leaveState = MutableStateFlow<UiState<LeaveSimulatorData>>(UiState.Idle)
    val leaveState: StateFlow<UiState<LeaveSimulatorData>> = _leaveState.asStateFlow()

    // Leave simulator tab – whole week
    private val _weekState = MutableStateFlow<UiState<WeekSimulatorData>>(UiState.Idle)
    val weekState: StateFlow<UiState<WeekSimulatorData>> = _weekState.asStateFlow()

    // Timetable tab
    private val _timetableState = MutableStateFlow<UiState<TimetableData>>(UiState.Idle)
    val timetableState: StateFlow<UiState<TimetableData>> = _timetableState.asStateFlow()

    // Credential holder (set once on login)
    private val _credentials = MutableStateFlow<Pair<String, String>?>(null)
    val credentials: StateFlow<Pair<String, String>?> = _credentials.asStateFlow()

    // Used to fire a "save to DataStore" side-effect
    private val _lastLoginEvent = MutableStateFlow<Triple<String, String, String>?>(null)
    val lastLoginEvent: StateFlow<Triple<String, String, String>?> = _lastLoginEvent.asStateFlow()

    // ── Auth ─────────────────────────────────────────────────────────────

    fun login(username: String, password: String) {
        _credentials.value = Pair(username, password)
        fetchAttendance(username, password)
    }

    fun logout() {
        _credentials.value = null
        _attendanceState.value = UiState.Idle
        _analysisState.value = UiState.Idle
        _riskState.value = UiState.Idle
        _leaveState.value = UiState.Idle
        _weekState.value = UiState.Idle
        _timetableState.value = UiState.Idle
    }

    // ── Data fetching ─────────────────────────────────────────────────────

    fun fetchAttendance(username: String, password: String) {
        _attendanceState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ApiService.fetchAttendance(username, password) }
                .onSuccess {
                    _attendanceState.value = UiState.Success(it)
                    _credentials.value = Pair(username, password)
                    _lastLoginEvent.value = Triple("", username, password)
                }
                .onFailure { _attendanceState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun fetchAnalysis() {
        val (u, p) = _credentials.value ?: return
        _analysisState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ApiService.fetchAnalysis(u, p) }
                .onSuccess { _analysisState.value = UiState.Success(it) }
                .onFailure { _analysisState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun fetchRiskEngine() {
        val (u, p) = _credentials.value ?: return
        _riskState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ApiService.fetchRiskEngine(u, p) }
                .onSuccess { _riskState.value = UiState.Success(it) }
                .onFailure { _riskState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun fetchLeaveSimulator(day: String) {
        val (u, p) = _credentials.value ?: return
        _leaveState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ApiService.fetchLeaveSimulator(u, p, day) }
                .onSuccess { _leaveState.value = UiState.Success(it) }
                .onFailure { _leaveState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun fetchWeekSimulator() {
        val (u, p) = _credentials.value ?: return
        _weekState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ApiService.fetchWeekSimulator(u, p) }
                .onSuccess { _weekState.value = UiState.Success(it) }
                .onFailure { _weekState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun fetchTimetable() {
        if (_timetableState.value is UiState.Success) return // cache it
        _timetableState.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ApiService.fetchTimetable() }
                .onSuccess { _timetableState.value = UiState.Success(it) }
                .onFailure { _timetableState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }

    // Legacy compat (used in MainActivity)
    val uiState: StateFlow<UiState<AttendanceData>> = attendanceState

    // Refresh helper used from UI
    fun refresh() {
        val (u, p) = _credentials.value ?: return
        fetchAttendance(u, p)
    }
}
