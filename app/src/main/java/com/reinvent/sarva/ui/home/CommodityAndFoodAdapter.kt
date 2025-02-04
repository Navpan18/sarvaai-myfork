package com.reinvent.sarva.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.reinvent.sarva.R

class CommodityAndFoodAdapter(private val context : Context) :
    RecyclerView.Adapter<CommodityAndFoodAdapter.CommodityAndFoodItemViewHolder>()
{
    override fun onCreateViewHolder(
        parent : ViewGroup ,
        viewType : Int
    ) : CommodityAndFoodItemViewHolder
    {
        val itemView =
            LayoutInflater.from(context).inflate(R.layout.commodity_or_food_item , parent , false)
        return CommodityAndFoodItemViewHolder(itemView)
    }

    override fun getItemCount() : Int
    {
        return 10
    }

    override fun onBindViewHolder(holder : CommodityAndFoodItemViewHolder , position : Int)
    {

    }

    class CommodityAndFoodItemViewHolder(itemView : View) : ViewHolder(itemView)

}