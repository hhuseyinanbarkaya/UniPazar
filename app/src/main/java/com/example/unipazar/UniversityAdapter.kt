package com.example.unipazar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UniversityAdapter(
    private var universityList: List<String>,
    private val onUniversityClick: (String) -> Unit
) : RecyclerView.Adapter<UniversityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUniversityName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_university, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uni = universityList[position]
        holder.tvName.text = uni
        
        if (uni == "Tüm Üniversiteler") {
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.tvName.setTextColor(android.graphics.Color.parseColor("#FF5400"))
        } else {
            holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.tvName.setTextColor(android.graphics.Color.parseColor("#111827"))
        }

        holder.itemView.setOnClickListener {
            onUniversityClick(uni)
        }
    }

    override fun getItemCount() = universityList.size

    fun updateList(newList: List<String>) {
        universityList = newList
        notifyDataSetChanged()
    }
}
