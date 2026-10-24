package com.example.moveai

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.util.Locale

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var adapter: HistoryAdapter
    private var historyList = mutableListOf<WorkoutHistory>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewHistory)
        val btnClear = view.findViewById<View>(R.id.btnClearHistory)

        rv.layoutManager = LinearLayoutManager(requireContext())

        // Fetch the history and convert it to a MutableList so it can be cleared instantly
        historyList = HistoryManager.getHistory(requireContext()).toMutableList()
        adapter = HistoryAdapter(historyList)
        rv.adapter = adapter

        // Logic for the clear history button
        btnClear.setOnClickListener {
            if (historyList.isNotEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to delete all workouts?")
                    .setPositiveButton("Yes") { _, _ ->
                        HistoryManager.clearHistory(requireContext())
                        historyList.clear()
                        adapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "History Cleared!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "History is already empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class HistoryAdapter(private val items: MutableList<WorkoutHistory>) : RecyclerView.Adapter<HistoryAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgMapSnapshot)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
    )

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = items[pos]

        holder.tvTitle.text = "${item.type ?: "Unknown"} • ${item.date ?: "No date"}"

        val formattedDistance = String.format(Locale.getDefault(), "%.2f", item.distanceKm ?: 0f)
        holder.tvDetails.text = "Dist: ${formattedDistance}km | Time: ${item.time ?: "00:00"} | Pace: ${item.pace ?: "0:00"} | ${item.calories ?: 0} kcal"

        val imagePath = item.mapImagePath
        if (!imagePath.isNullOrEmpty()) {
            val file = File(imagePath)
            if (file.exists()) {
                Glide.with(holder.itemView.context)
                    .load(Uri.fromFile(file))
                    .override(800, 400)
                    .centerCrop()
                    .into(holder.img)
            } else {
                holder.img.setImageDrawable(null)
            }
        } else {
            holder.img.setImageDrawable(null)
        }
    }

    override fun getItemCount() = items.size
}