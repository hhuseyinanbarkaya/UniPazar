package com.example.unipazar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categories: List<String>,
    private var selectedCategory: String = "Tümü",
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llCategoryRoot: LinearLayout = itemView.findViewById(R.id.llCategoryRoot)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category

        if (category == selectedCategory) {
            holder.llCategoryRoot.setBackgroundResource(R.drawable.bg_category_selected)
            holder.tvCategoryName.setTextColor(Color.WHITE)
        } else {
            holder.llCategoryRoot.setBackgroundResource(R.drawable.bg_category_unselected)
            holder.tvCategoryName.setTextColor(Color.parseColor("#6B7280"))
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedCategory
            selectedCategory = category
            
            notifyItemChanged(categories.indexOf(previousSelected))
            notifyItemChanged(position)
            
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}
