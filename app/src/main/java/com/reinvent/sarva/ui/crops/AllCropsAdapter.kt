package com.reinvent.sarva.ui.crops

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reinvent.sarva.R

class AllCropsAdapter(
    private val cropsList: List<String>,
    private val myCrops: Set<String>,  // ✅ Keep track of selected crops
    private val onCropSelected: (String) -> Unit
) : RecyclerView.Adapter<AllCropsAdapter.AllCropsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllCropsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_crop, parent, false)
        return AllCropsViewHolder(view)
    }

    override fun onBindViewHolder(holder: AllCropsViewHolder, position: Int) {
        val cropName = cropsList[position]
        holder.bind(cropName, myCrops.contains(cropName), onCropSelected)
    }

    override fun getItemCount(): Int = cropsList.size

    class AllCropsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropTextView: TextView = itemView.findViewById(R.id.crop_name)

        fun bind(cropName: String, isAdded: Boolean, onCropSelected: (String) -> Unit) {
            cropTextView.text = cropName
            cropTextView.setBackgroundColor(if (isAdded) Color.GREEN else Color.WHITE) // ✅ Turns green ONLY if added

            itemView.setOnClickListener {
                onCropSelected(cropName)
            }
        }
    }
}


