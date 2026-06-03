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
        val ivVerifiedBadge: ImageView? = view.findViewById(R.id.ivVerifiedBadge)
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
        holder.tvPrice.text = "₺" + PriceFormatter.format(ad.price.replace("₺", "").trim())
        holder.tvUniversity.text = ad.university
        
        holder.tvSellerName.text = ad.sellerName
        holder.tvTimestamp.text = TimeUtils.getTimeAgo(ad.timestamp)
        holder.ivVerifiedBadge?.visibility = if (ad.isSellerVerified) View.VISIBLE else View.GONE

        if (ad.type == "SALE") {
            holder.tvBadge.text = "Satılık"
            holder.tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))
        } else {
            holder.tvBadge.text = "Aranıyor"
            holder.tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
        }
        
        val fallbackUrl = when (ad.category) {
            "Kitap" -> "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=800&auto=format&fit=crop"
            "Elektronik" -> "https://images.unsplash.com/photo-1498049794561-7780e7231661?q=80&w=800&auto=format&fit=crop"
            "Ev Eşyası" -> "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?q=80&w=800&auto=format&fit=crop"
            "Özel Ders" -> "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?q=80&w=800&auto=format&fit=crop"
            "Diğer" -> "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=800&auto=format&fit=crop"
            else -> "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=800&auto=format&fit=crop"
        }

        val firstImage = if (ad.imageUrls.isNotEmpty()) ad.imageUrls[0] else ad.imageUrl
        val targetImage = if (firstImage.isNotEmpty()) firstImage else fallbackUrl
        if (targetImage.startsWith("data:image")) {
            val bytes = android.util.Base64.decode(targetImage.substringAfter("base64,"), android.util.Base64.DEFAULT)
            Glide.with(holder.itemView.context).load(bytes).transform(CenterCrop(), RoundedCorners(24)).into(holder.ivAdImage)
        } else {
            Glide.with(holder.itemView.context).load(targetImage).error(Glide.with(holder.itemView.context).load(fallbackUrl)).transform(CenterCrop(), RoundedCorners(24)).into(holder.ivAdImage)
        }
        
        val defaultAvatar = "https://ui-avatars.com/api/?name=${ad.sellerName.replace(" ", "+")}&background=random"
        val targetAvatar = if (ad.sellerAvatarUrl.isNotEmpty()) ad.sellerAvatarUrl else defaultAvatar
        if (targetAvatar.startsWith("data:image")) {
            val bytes = android.util.Base64.decode(targetAvatar.substringAfter("base64,"), android.util.Base64.DEFAULT)
            Glide.with(holder.itemView.context).load(bytes).transform(CenterCrop(), RoundedCorners(100)).into(holder.ivSellerAvatar)
        } else {
            Glide.with(holder.itemView.context).load(targetAvatar).error(Glide.with(holder.itemView.context).load(defaultAvatar)).transform(CenterCrop(), RoundedCorners(100)).into(holder.ivSellerAvatar)
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
