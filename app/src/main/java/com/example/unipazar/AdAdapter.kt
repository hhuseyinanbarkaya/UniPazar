package com.example.unipazar

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class AdAdapter(private var ads: List<Ad>) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvUniversity: TextView = view.findViewById(R.id.tvUniversity)
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val ivAdImage: ImageView = view.findViewById(R.id.ivAdImage)
        
        val tvSellerName: TextView = view.findViewById(R.id.tvSellerName)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val ivSellerAvatar: ImageView = view.findViewById(R.id.ivSellerAvatar)
    }

    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1
    }

    var isGridView: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_GRID) R.layout.item_ad_grid else R.layout.item_ad
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return AdViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        val ad = ads[position]
        holder.tvTitle.text = ad.title
        holder.tvDescription.text = ad.description
        holder.tvPrice.text = ad.price
        holder.tvUniversity.text = ad.university
        
        holder.tvSellerName.text = ad.sellerName
        holder.tvTimestamp.text = TimeUtils.getTimeAgo(ad.timestamp)

        if (ad.type == "SALE") {
            holder.tvBadge.text = "Satılık"
            holder.tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))
        } else {
            holder.tvBadge.text = "Aranıyor"
            holder.tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
        }
        
        // Show first image if list is not empty, else fallback to old imageUrl, else hide/placeholder
        val firstImage = if (ad.imageUrls.isNotEmpty()) ad.imageUrls[0] else ad.imageUrl
        if (firstImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(firstImage)
                .transform(CenterCrop(), RoundedCorners(24))
                .into(holder.ivAdImage)
        } else {
            // Load a default placeholder
            Glide.with(holder.itemView.context)
                .load(R.drawable.placeholder_image) // We'll create this or use a color
                .transform(CenterCrop(), RoundedCorners(24))
                .into(holder.ivAdImage)
        }
        
        if (ad.sellerAvatarUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(ad.sellerAvatarUrl)
                .transform(CenterCrop(), RoundedCorners(100))
                .into(holder.ivSellerAvatar)
        }

        // Open Ad Detail Screen
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, AdDetailActivity::class.java)
            intent.putExtra("AD_ITEM", ad)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = ads.size

    fun updateData(newAds: List<Ad>) {
        ads = newAds
        notifyDataSetChanged()
    }
}
