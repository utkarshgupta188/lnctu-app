package com.meow.lnctattendance.data

// ──────────────────────────────────────────────
// Core attendance models
// ──────────────────────────────────────────────

data class AttendanceData(
    val studentName: String?,
    val totalClasses: Int,
    val present: Int,
    val absent: Int,
    val percentage: Double,
    val overallPercentage: Double,
    val attendedClasses: Int,
    val subjects: List<Subject> = emptyList(),
)

data class Subject(
    val name: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val percentage: Double,
)

// ──────────────────────────────────────────────
// Analysis models
// ──────────────────────────────────────────────

data class AnalysisData(
    val summary: AnalysisSummary,
    val atRiskSubjects: List<Subject>,
    val safeSubjects: List<Subject>,
    val dayAnalysis: Map<String, DayAnalysis>,
    val predictions: List<SubjectPrediction>,
)

data class AnalysisSummary(
    val totalSubjects: Int,
    val atRiskCount: Int,
    val safeCount: Int,
    val overallPercentage: Double,
    val overallStatus: String,
    val overallMessage: String,
)

data class DayAnalysis(
    val subjects: List<String>,
    val atRiskCount: Int,
    val safeCount: Int,
    val totalClasses: Int,
    val leaveRecommendation: String,
)

data class SubjectPrediction(
    val subject: String,
    val currentPercentage: Double,
    val status: String,
    val canMiss: Int?,
    val classesNeeded: Int?,
    val daysToRecover: Int?,
    val message: String,
)

// ──────────────────────────────────────────────
// Risk engine models
// ──────────────────────────────────────────────

data class RiskEngineData(
    val threshold: Double,
    val overallRiskStatus: String,
    val atRiskSubjectsCount: Int,
    val criticalAlert: Boolean,
    val subjectRisks: List<SubjectRisk>,
)

data class SubjectRisk(
    val subject: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val percentage: Double,
    val riskLevel: String,
    val absentsAllowedBeforeThreshold: Int,
    val alreadyBelowThreshold: Boolean,
    val consecutivePresentsNeeded: Int,
    val estimatedDaysToRecover: Int,
    val projectedPercentageIfMissOne: Double,
)

// ──────────────────────────────────────────────
// Leave simulator models
// ──────────────────────────────────────────────

data class LeaveSimulatorData(
    val simulatedDay: String,
    val totalClassesOnDay: Int,
    val affectedSubjectsCount: Int,
    val recommendation: String,
    val advice: String,
    val totalImpactScore: Int,
    val subjectSimulations: List<SubjectSimulation>,
    val overallAttendance: OverallAttendanceChange,
)

data class SubjectSimulation(
    val subject: String,
    val currentPercentage: Double,
    val classesOnThisDay: Int,
    val projectedPercentage: Double,
    val percentageDrop: Double,
    val impactLevel: String,
    val willFallBelow75: Boolean,
    val statusAfterAbsence: String,
)

data class OverallAttendanceChange(
    val current: Double,
    val projected: Double,
    val drop: Double,
)

// ──────────────────────────────────────────────
// Weekly leave simulator models
// ──────────────────────────────────────────────

data class WeekSimulatorData(
    val currentOverallPercentage: Double,
    val weekSimulation: List<DaySimulation>,
    val wholeWeekLeave: WholeWeekLeave,
)

data class DaySimulation(
    val day: String,
    val totalClassUnits: Int,
    val affectedSubjectsCount: Int,
    val recommendation: String,
    val advice: String,
    val totalImpactScore: Int,
    val projectedOverallPercentage: Double,
    val overallDrop: Double,
    val subjectSimulations: List<SubjectSimulation>,
)

data class WholeWeekLeave(
    val totalAbsences: Int,
    val projectedOverallPercentage: Double,
    val overallDrop: Double,
)

// ──────────────────────────────────────────────
// Timetable models
// ──────────────────────────────────────────────

data class TimetableData(
    val days: Map<String, List<TimetablePeriod>>,
)

data class TimetablePeriod(
    val time: String,
    val subject: String,
)
