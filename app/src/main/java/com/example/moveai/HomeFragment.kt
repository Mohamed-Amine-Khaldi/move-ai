package com.example.moveai

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalCalories: TextView

    // These are the new cards added in the design
    private lateinit var tvStepsCount: TextView
    private lateinit var tvActivityTime: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind elements to the new design
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance)
        tvTotalCalories = view.findViewById(R.id.tvTotalCalories)

        // (We need to add these IDs in the XML now)
        tvStepsCount = view.findViewById(R.id.tvStepsCount)
        tvActivityTime = view.findViewById(R.id.tvActivityTime)

        val prefsAuth = requireActivity().getSharedPreferences("MoveAI_Auth", Context.MODE_PRIVATE)
        val userName = prefsAuth.getString("userName", "Jack")
        tvGreeting.text = "Hello, $userName \uD83D\uDC4B"
    }

    override fun onResume() {
        super.onResume()
        updateRealStats()
    }

    private fun updateRealStats() {
        try {
            val historyList = HistoryManager.getHistory(requireContext())
            var totalKm = 0f
            var totalCal = 0

            // Your custom calculation logic
            for (workout in historyList) {
                totalKm += workout.distanceKm
                totalCal += workout.calories
            }

            // You can also calculate time and steps later if saved in History
            val mockSteps = (totalKm * 1300).toInt() // Calculated steps approximately based on distance
            val mockTime = (totalKm * 12).toInt() // Calculated time approximately (12 mins per km)

            // Now populating the calculations into the new design cards
            tvTotalDistance.text = String.format(java.util.Locale.US, "%.1f km", totalKm)
            tvTotalCalories.text = "$totalCal kcal"
            tvStepsCount.text = "$mockSteps"
            tvActivityTime.text = "$mockTime min"

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}