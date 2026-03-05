package com.meow.lnctattendance.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val BASE_URL = "https://lnctu.vercel.app"

object ApiService {

    // ── Internal HTTP helper ──────────────────────────────────────────────

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL$path").openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 20_000
        conn.readTimeout = 25_000
        conn.setRequestProperty("Accept", "application/json")
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream.bufferedReader().use(BufferedReader::readText)
        conn.disconnect()
        if (code !in 200..299) throw ApiException("HTTP $code: ${body.take(500)}")
        JSONObject(body)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    // ── Public API calls ──────────────────────────────────────────────────

    suspend fun fetchAttendance(username: String, password: String): AttendanceData {
        val json = get("/attendance?username=${enc(username)}&password=${enc(password)}")
        return parseAttendance(json)
    }

    suspend fun fetchAnalysis(username: String, password: String): AnalysisData {
        val json = get("/analysis?username=${enc(username)}&password=${enc(password)}")
        return parseAnalysis(json)
    }

    suspend fun fetchRiskEngine(username: String, password: String): RiskEngineData {
        val json = get("/risk-engine?username=${enc(username)}&password=${enc(password)}")
        return parseRiskEngine(json)
    }

    suspend fun fetchLeaveSimulator(username: String, password: String, day: String): LeaveSimulatorData {
        val json = get("/leave-simulator?username=${enc(username)}&password=${enc(password)}&day=${enc(day)}")
        return parseLeaveSimulator(json)
    }

    suspend fun fetchWeekSimulator(username: String, password: String): WeekSimulatorData {
        val json = get("/leave-simulator-week?username=${enc(username)}&password=${enc(password)}")
        return parseWeekSimulator(json)
    }

    suspend fun fetchTimetable(): TimetableData {
        val json = get("/timetable")
        return parseTimetable(json)
    }

    // ── Parsers ───────────────────────────────────────────────────────────

    private fun parseAttendance(root: JSONObject): AttendanceData {
        assertSuccess(root)
        val d = root.getJSONObject("data")
        return AttendanceData(
            studentName     = d.optString("student_name", "").takeIf { it.isNotBlank() },
            totalClasses    = d.optInt("total_classes"),
            present         = d.optInt("present"),
            absent          = d.optInt("absent"),
            percentage      = d.optDouble("percentage"),
            overallPercentage = d.optDouble("overall_percentage", d.optDouble("percentage")),
            attendedClasses = d.optInt("attended_classes", d.optInt("present")),
            subjects        = parseSubjectArray(d.optJSONArray("subjects")),
            datewise        = parseDatewiseArray(d.optJSONArray("datewise")),
        )
    }

    private fun parseAnalysis(root: JSONObject): AnalysisData {
        assertSuccess(root)
        val d = root.getJSONObject("data")
        val sumJson = d.getJSONObject("summary")
        return AnalysisData(
            summary = AnalysisSummary(
                totalSubjects      = sumJson.optInt("total_subjects"),
                atRiskCount        = sumJson.optInt("at_risk_count"),
                safeCount          = sumJson.optInt("safe_count"),
                overallPercentage  = sumJson.optDouble("overall_percentage"),
                overallStatus      = sumJson.optString("overall_status"),
                overallMessage     = sumJson.optString("overall_message"),
            ),
            atRiskSubjects = parseSubjectArray(d.optJSONArray("at_risk_subjects")),
            safeSubjects   = parseSubjectArray(d.optJSONArray("safe_subjects")),
            dayAnalysis    = parseDayAnalysis(d.optJSONObject("day_analysis")),
            predictions    = parsePredictions(d.optJSONArray("predictions")),
        )
    }

    private fun parseRiskEngine(root: JSONObject): RiskEngineData {
        assertSuccess(root)
        val d = root.getJSONObject("data")
        return RiskEngineData(
            threshold            = d.optDouble("threshold", 75.0),
            overallRiskStatus    = d.optString("overall_risk_status"),
            atRiskSubjectsCount  = d.optInt("at_risk_subjects_count"),
            criticalAlert        = d.optBoolean("critical_alert"),
            subjectRisks         = parseSubjectRisks(d.optJSONArray("subject_risks")),
        )
    }

    private fun parseLeaveSimulator(root: JSONObject): LeaveSimulatorData {
        assertSuccess(root)
        val d = root.getJSONObject("data")
        val oa = d.optJSONObject("overall_attendance")
        return LeaveSimulatorData(
            simulatedDay           = d.optString("simulated_day"),
            totalClassesOnDay      = d.optInt("total_classes_on_day"),
            affectedSubjectsCount  = d.optInt("affected_subjects_count"),
            recommendation         = d.optString("recommendation"),
            advice                 = d.optString("advice"),
            totalImpactScore       = d.optInt("total_impact_score"),
            subjectSimulations     = parseSubjectSimulations(d.optJSONArray("subject_simulations")),
            overallAttendance      = OverallAttendanceChange(
                current   = oa?.optDouble("current") ?: 0.0,
                projected = oa?.optDouble("projected") ?: 0.0,
                drop      = oa?.optDouble("drop") ?: 0.0,
            ),
        )
    }

    private fun parseWeekSimulator(root: JSONObject): WeekSimulatorData {
        assertSuccess(root)
        val d = root.getJSONObject("data")
        val ww = d.optJSONObject("whole_week_leave")
        return WeekSimulatorData(
            currentOverallPercentage = d.optDouble("current_overall_percentage"),
            weekSimulation           = parseDaySimulations(d.optJSONArray("week_simulation")),
            wholeWeekLeave           = WholeWeekLeave(
                totalAbsences                = ww?.optInt("total_absences") ?: 0,
                projectedOverallPercentage   = ww?.optDouble("projected_overall_percentage") ?: 0.0,
                overallDrop                  = ww?.optDouble("overall_drop") ?: 0.0,
            ),
        )
    }

    private fun parseTimetable(root: JSONObject): TimetableData {
        assertSuccess(root)
        val d = root.getJSONObject("data")
        val days = mutableMapOf<String, List<TimetablePeriod>>()
        for (day in d.keys()) {
            val arr = d.getJSONArray(day)
            val periods = (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                TimetablePeriod(time = p.optString("time"), subject = p.optString("subject"))
            }
            days[day] = periods
        }
        return TimetableData(days = days)
    }

    // ── Helper parsers ────────────────────────────────────────────────────

    private fun assertSuccess(root: JSONObject) {
        if (!root.optBoolean("success", false)) {
            throw ApiException(root.optString("message", root.optString("detail", "Request failed")))
        }
    }

    private fun parseSubjectArray(arr: JSONArray?): List<Subject> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Subject(
                name       = o.optString("name"),
                total      = o.optInt("total"),
                present    = o.optInt("present"),
                absent     = o.optInt("absent"),
                percentage = o.optDouble("percentage"),
            )
        }
    }

    private fun parseDatewiseArray(arr: JSONArray?): List<DatewiseRecord> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            DatewiseRecord(
                date    = o.optString("date"),
                lecture = o.optString("lecture"),
                subject = o.optString("subject"),
                status  = o.optString("status"),
            )
        }
    }

    private fun parseDayAnalysis(obj: JSONObject?): Map<String, DayAnalysis> {
        obj ?: return emptyMap()
        val map = mutableMapOf<String, DayAnalysis>()
        for (day in obj.keys()) {
            val d = obj.getJSONObject(day)
            val subjectsArr = d.optJSONArray("subjects")
            val subjects = if (subjectsArr != null)
                (0 until subjectsArr.length()).map { subjectsArr.getString(it) }
            else emptyList()
            map[day] = DayAnalysis(
                subjects             = subjects,
                atRiskCount          = d.optInt("at_risk_count"),
                safeCount            = d.optInt("safe_count"),
                totalClasses         = d.optInt("total_classes"),
                leaveRecommendation  = d.optString("leave_recommendation"),
            )
        }
        return map
    }

    private fun parsePredictions(arr: JSONArray?): List<SubjectPrediction> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SubjectPrediction(
                subject            = o.optString("subject"),
                currentPercentage  = o.optDouble("current_percentage"),
                status             = o.optString("status"),
                canMiss            = if (o.has("can_miss")) o.optInt("can_miss") else null,
                classesNeeded      = if (o.has("classes_needed")) o.optInt("classes_needed") else null,
                daysToRecover      = if (o.has("days_to_recover")) o.optInt("days_to_recover") else null,
                message            = o.optString("message"),
            )
        }
    }

    private fun parseSubjectRisks(arr: JSONArray?): List<SubjectRisk> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SubjectRisk(
                subject                        = o.optString("subject"),
                total                          = o.optInt("total"),
                present                        = o.optInt("present"),
                absent                         = o.optInt("absent"),
                percentage                     = o.optDouble("percentage"),
                riskLevel                      = o.optString("risk_level"),
                absentsAllowedBeforeThreshold  = o.optInt("absents_allowed_before_threshold"),
                alreadyBelowThreshold          = o.optBoolean("already_below_threshold"),
                consecutivePresentsNeeded      = o.optInt("consecutive_presents_needed"),
                estimatedDaysToRecover         = o.optInt("estimated_days_to_recover"),
                projectedPercentageIfMissOne   = o.optDouble("projected_percentage_if_miss_one"),
            )
        }
    }

    private fun parseSubjectSimulations(arr: JSONArray?): List<SubjectSimulation> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SubjectSimulation(
                subject            = o.optString("subject"),
                currentPercentage  = o.optDouble("current_percentage"),
                classesOnThisDay   = o.optInt("classes_on_this_day"),
                projectedPercentage = o.optDouble("projected_percentage"),
                percentageDrop     = o.optDouble("percentage_drop"),
                impactLevel        = o.optString("impact_level"),
                willFallBelow75    = o.optBoolean("will_fall_below_75"),
                statusAfterAbsence = o.optString("status_after_absence"),
            )
        }
    }

    private fun parseDaySimulations(arr: JSONArray?): List<DaySimulation> {
        arr ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            DaySimulation(
                day                          = o.optString("day"),
                totalClassUnits              = o.optInt("total_class_units"),
                affectedSubjectsCount        = o.optInt("affected_subjects_count"),
                recommendation               = o.optString("recommendation"),
                advice                       = o.optString("advice"),
                totalImpactScore             = o.optInt("total_impact_score"),
                projectedOverallPercentage   = o.optDouble("projected_overall_percentage"),
                overallDrop                  = o.optDouble("overall_drop"),
                subjectSimulations           = parseSubjectSimulations(o.optJSONArray("subject_simulations")),
            )
        }
    }
}

class ApiException(message: String) : Exception(message)
