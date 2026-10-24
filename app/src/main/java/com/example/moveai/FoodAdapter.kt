package com.example.moveai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

class FoodAdapter(private var foodList: List<Meal>) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvDetails: TextView = itemView.findViewById(R.id.tvFoodDetails)
        val ivImage: ImageView = itemView.findViewById(R.id.ivFoodImage)

        // Ingredient UI components
        val btnExpand: ImageView = itemView.findViewById(R.id.btnExpand)
        val layoutIngredients: MaterialCardView = itemView.findViewById(R.id.layoutIngredients)
        val tvIngredientsList: TextView = itemView.findViewById(R.id.tvIngredientsList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = foodList[position]

        holder.tvName.text = item.strMeal
        holder.tvDetails.text = "${item.strCategory ?: "Healthy"} • ${item.strArea ?: "Global"}"

        Glide.with(holder.itemView.context)
            .load(item.strMealThumb)
            .placeholder(android.R.color.darker_gray)
            .centerCrop()
            .into(holder.ivImage)

        // Populate the ingredients text
        holder.tvIngredientsList.text = item.getIngredientsList()

        // Logic to rotate the arrow icon and show/hide ingredients
        val isExpanded = item.isExpanded
        holder.layoutIngredients.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.btnExpand.rotation = if (isExpanded) 180f else 0f

        holder.btnExpand.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = foodList.size

    fun updateData(newData: List<Meal>) {
        foodList = newData
        notifyDataSetChanged()
    }
}