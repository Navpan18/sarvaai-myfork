package com.reinvent.sarva.ui.crops

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reinvent.sarva.R

class CropsAdapter(
    private val allCropsList: List<String>,
    private val myCropsList: MutableSet<String>,
    private val onCropSelected: (String) -> Unit
) : RecyclerView.Adapter<CropsAdapter.CropsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crop, parent, false)
        return CropsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CropsViewHolder, position: Int) {
        val cropName = allCropsList[position]
        holder.bind(cropName, myCropsList.contains(cropName))

        holder.itemView.setOnClickListener {
            onCropSelected(cropName)
        }
    }

    override fun getItemCount(): Int = allCropsList.size

    class CropsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropTextView: TextView = itemView.findViewById(R.id.crop_name)

        fun bind(cropName: String, isAdded: Boolean) {
            cropTextView.text = cropName
            if (isAdded) {
                cropTextView.setBackgroundColor(Color.GREEN) // Highlight selected crops
            } else {
                cropTextView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}
