package com.reinvent.sarva.ui.crops

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reinvent.sarva.R

class MyCropsAdapter(
    private val myCropsList: MutableList<String>,
    private val onRequestRemoval: (String) -> Unit
) : RecyclerView.Adapter<MyCropsAdapter.MyCropsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCropsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crop, parent, false)
        return MyCropsViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyCropsViewHolder, position: Int) {
        val cropName = myCropsList[position]
        holder.bind(cropName, onRequestRemoval)
    }

    override fun getItemCount(): Int = myCropsList.size

    class MyCropsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cropTextView: TextView = itemView.findViewById(R.id.crop_name)

        fun bind(cropName: String, onRequestRemoval: (String) -> Unit) {
            cropTextView.text = cropName

            // Handle Click for Removal Confirmation
            itemView.setOnClickListener {
                onRequestRemoval(cropName)  // 🔥 This triggers the dialog in MyCropsActivity
            }
        }
    }
}
