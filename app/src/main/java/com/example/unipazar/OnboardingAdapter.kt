package com.example.unipazar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

data class OnboardingSlide(
    val title: String,
    val description: String,
    val imageResId: Int,
    val backgroundColor: Int
)

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIllustration: ImageView = itemView.findViewById(R.id.ivIllustration)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(slide: OnboardingSlide) {
            // Set the illustration background color to blend with vector assets
            ivIllustration.setBackgroundColor(slide.backgroundColor)
            ivIllustration.setImageResource(slide.imageResId)
            
            // Set HTML text for colored titles
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                tvTitle.text = android.text.Html.fromHtml(slide.title, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                tvTitle.text = android.text.Html.fromHtml(slide.title)
            }
            
            tvDescription.text = slide.description
        }
    }
}
