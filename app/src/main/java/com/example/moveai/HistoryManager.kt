package com.example.moveai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkoutHistory(
    val id: String,
    val type: String,
    val distanceKm: Float,
    val time: String,
    val pace: String,
    val calories: Int,
    val date: String,
    val mapImagePath: String
)

object HistoryManager {
    private const val PREFS_NAME = "MoveAI_History"
    private const val KEY_WORKOUTS = "saved_workouts"

    fun saveWorkout(
        context: Context,
        type: String,
        distanceKm: Float,
        time: String,
        pace: String,
        calories: Int,
        mapImagePath: String
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            val jsonOld = prefs.getString(KEY_WORKOUTS, "[]")
            val typeToken = object : TypeToken<MutableList<WorkoutHistory>>() {}.type

            val historyList: MutableList<WorkoutHistory> = gson.fromJson(jsonOld, typeToken) ?: mutableListOf()

            val currentDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            val newWorkout = WorkoutHistory(
                id = System.currentTimeMillis().toString(),
                type = type, distanceKm = distanceKm, time = time,
                pace = pace, calories = calories, date = currentDate,
                mapImagePath = mapImagePath
            )

            historyList.add(0, newWorkout)
            prefs.edit().putString(KEY_WORKOUTS, gson.toJson(historyList)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistory(context: Context): List<WorkoutHistory> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_WORKOUTS, "[]")
            val typeToken = object : TypeToken<List<WorkoutHistory>>() {}.type

            Gson().fromJson(json, typeToken) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // This new function clears all saved workout records
    fun clearHistory(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_WORKOUTS).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}