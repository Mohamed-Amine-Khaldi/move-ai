package com.example.moveai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class MealResponse(val meals: List<Meal>?)

data class Meal(
    val idMeal: String,
    val strMeal: String,
    var strCategory: String?,
    var strArea: String?,
    val strMealThumb: String?,

    // Ingredients and measurements (10 are sufficient for the presentation)
    val strIngredient1: String?, val strMeasure1: String?,
    val strIngredient2: String?, val strMeasure2: String?,
    val strIngredient3: String?, val strMeasure3: String?,
    val strIngredient4: String?, val strMeasure4: String?,
    val strIngredient5: String?, val strMeasure5: String?,
    val strIngredient6: String?, val strMeasure6: String?,
    val strIngredient7: String?, val strMeasure7: String?,
    val strIngredient8: String?, val strMeasure8: String?,
    val strIngredient9: String?, val strMeasure9: String?,
    val strIngredient10: String?, val strMeasure10: String?,

    // State flag to control the UI expansion animation
    var isExpanded: Boolean = false
) {
    // This function aggregates the non-null ingredients into a clean formatted list
    fun getIngredientsList(): String {
        val list = mutableListOf<String>()
        if (!strIngredient1.isNullOrBlank()) list.add("• $strIngredient1 ($strMeasure1)")
        if (!strIngredient2.isNullOrBlank()) list.add("• $strIngredient2 ($strMeasure2)")
        if (!strIngredient3.isNullOrBlank()) list.add("• $strIngredient3 ($strMeasure3)")
        if (!strIngredient4.isNullOrBlank()) list.add("• $strIngredient4 ($strMeasure4)")
        if (!strIngredient5.isNullOrBlank()) list.add("• $strIngredient5 ($strMeasure5)")
        if (!strIngredient6.isNullOrBlank()) list.add("• $strIngredient6 ($strMeasure6)")
        if (!strIngredient7.isNullOrBlank()) list.add("• $strIngredient7 ($strMeasure7)")
        if (!strIngredient8.isNullOrBlank()) list.add("• $strIngredient8 ($strMeasure8)")
        if (!strIngredient9.isNullOrBlank()) list.add("• $strIngredient9 ($strMeasure9)")
        if (!strIngredient10.isNullOrBlank()) list.add("• $strIngredient10 ($strMeasure10)")
        return list.joinToString("\n")
    }
}

interface MealApi {
    // Switched to 'search' endpoint so the API consistently returns ingredient details
    @GET("search.php")
    suspend fun getMealsBySearch(@Query("s") query: String): Response<MealResponse>
}

class FoodFragment : Fragment() {

    private lateinit var rvDietTips: RecyclerView
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var btnRefresh: FloatingActionButton
    private lateinit var scrollViewFood: NestedScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_food, container, false)

        rvDietTips = view.findViewById(R.id.rvDietTips)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        scrollViewFood = view.findViewById(R.id.scrollViewFood)

        rvDietTips.layoutManager = LinearLayoutManager(requireContext())
        foodAdapter = FoodAdapter(emptyList())
        rvDietTips.adapter = foodAdapter

        btnRefresh.setOnClickListener {
            Toast.makeText(requireContext(), "Loading fresh meals...", Toast.LENGTH_SHORT).show()
            fetchFoodData()
        }

        fetchFoodData()

        return view
    }

    private fun fetchFoodData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.themealdb.com/api/json/v1/1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(MealApi::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Queried specific keywords to ensure valid recipes and prevent null responses
                val req1 = async { api.getMealsBySearch("chicken") }
                val req2 = async { api.getMealsBySearch("beef") }
                val req3 = async { api.getMealsBySearch("salad") }

                val meals1 = req1.await().body()?.meals ?: emptyList()
                val meals2 = req2.await().body()?.meals ?: emptyList()
                val meals3 = req3.await().body()?.meals ?: emptyList()

                // Limited the result to 15 items to keep app performance smooth
                val combinedList = (meals1 + meals2 + meals3).shuffled().take(15)

                withContext(Dispatchers.Main) {
                    if (isAdded && context != null) {
                        foodAdapter.updateData(combinedList)
                        scrollViewFood.smoothScrollTo(0, 0)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Check your internet!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}