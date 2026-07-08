package com.meow.lnctattendance.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OfflineCacheHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "offline_cache.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ATTENDANCE = "attendance"
        private const val TABLE_TIMETABLE = "timetable"

        private const val COL_ID = "id"
        // Attendance columns
        private const val COL_PRESENT = "present"
        private const val COL_ABSENT = "absent"
        private const val COL_TOTAL = "total"
        private const val COL_PERCENTAGE = "percentage"
        private const val COL_LAST_FETCH = "last_fetch"

        // Timetable columns
        private const val COL_PERIODS_JSON = "periods_json"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_ATTENDANCE ($COL_ID INTEGER PRIMARY KEY, $COL_PRESENT INTEGER, $COL_ABSENT INTEGER, $COL_TOTAL INTEGER, $COL_PERCENTAGE REAL, $COL_LAST_FETCH INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_TIMETABLE ($COL_ID INTEGER PRIMARY KEY, $COL_PERIODS_JSON TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTENDANCE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TIMETABLE")
        onCreate(db)
    }

    fun saveAttendance(present: Int, absent: Int, total: Int, percentage: Double, lastFetch: Long) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_ID, 1)
                put(COL_PRESENT, present)
                put(COL_ABSENT, absent)
                put(COL_TOTAL, total)
                put(COL_PERCENTAGE, percentage)
                put(COL_LAST_FETCH, lastFetch)
            }
            db.replace(TABLE_ATTENDANCE, null, values)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun getAttendance(): AttendanceData? {
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_ATTENDANCE, null, "$COL_ID = 1", null, null, null, null)
            return cursor.use {
                if (it.moveToFirst()) {
                    AttendanceData(
                        present = it.getInt(it.getColumnIndexOrThrow(COL_PRESENT)),
                        absent = it.getInt(it.getColumnIndexOrThrow(COL_ABSENT)),
                        total = it.getInt(it.getColumnIndexOrThrow(COL_TOTAL)),
                        percentage = it.getDouble(it.getColumnIndexOrThrow(COL_PERCENTAGE)),
                        lastFetch = it.getLong(it.getColumnIndexOrThrow(COL_LAST_FETCH))
                    )
                } else null
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun saveTimetable(periodsJson: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_ID, 1)
                put(COL_PERIODS_JSON, periodsJson)
            }
            db.replace(TABLE_TIMETABLE, null, values)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun getTimetableJson(): String {
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_TIMETABLE, null, "$COL_ID = 1", null, null, null, null)
            return cursor.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(COL_PERIODS_JSON)) ?: "[]"
                } else "[]"
            }
        } catch (e: Exception) {
            return "[]"
        }
    }
}

data class AttendanceData(
    val present: Int,
    val absent: Int,
    val total: Int,
    val percentage: Double,
    val lastFetch: Long
)
