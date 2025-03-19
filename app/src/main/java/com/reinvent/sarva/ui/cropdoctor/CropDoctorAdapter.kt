package com.reinvent.sarva.ui.cropdoctor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reinvent.sarva.R

class CropDoctorAdapter(
    private val cropsList: List<String>,
    private val onCropSelected: (String) -> Unit
) : RecyclerView.Adapter<CropDoctorAdapter.CropDoctorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropDoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crop, parent, false)
        return CropDoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: CropDoctorViewHolder, position: Int) {
        val cropName = cropsList[position]
        holder.bind(cropName, onCropSelected)
    }

    override fun getItemCount(): Int = cropsList.size

    class CropDoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropTextView: TextView = itemView.findViewById(R.id.crop_name)

        fun bind(cropName: String, onCropSelected: (String) -> Unit) {
            cropTextView.text = cropName
            itemView.setOnClickListener { onCropSelected(cropName) }
        }
    }
}
