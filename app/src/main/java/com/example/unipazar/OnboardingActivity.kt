package com.example.unipazar

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var llDots: LinearLayout
    private lateinit var btnSkip: TextView
    private lateinit var btnNext: MaterialButton
    private lateinit var tvGoToLogin: TextView

    private lateinit var slides: List<OnboardingSlide>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if onboarding is already completed
        val sharedPrefs = getSharedPreferences("UniPazarPrefs", Context.MODE_PRIVATE)
        val isCompleted = sharedPrefs.getBoolean("onboarding_completed", false)

        if (isCompleted) {
            proceedToLoginOrMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPagerOnboarding)
        llDots = findViewById(R.id.llDots)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        // Define our 3 colorful slides with HTML formatting for brand orange highlights
        slides = listOf(
            OnboardingSlide(
                "Kampüsündeki Pazar Yerine <font color='#FF5400'>Hoş Geldin</font>",
                "Kendi üniversitendeki arkadaşlarınla güvenle eşya al ve sat. Kitaplardan teknolojiye her şey burada!",
                R.drawable.welcome_intro,
                Color.parseColor("#4F46E5") // Indigo image bg
            ),
            OnboardingSlide(
                "Sadece <font color='#FF5400'>Üniversiteliler</font>",
                "Sadece .edu.tr uzantılı e-postaya sahip onaylı üniversite öğrencileri ilan verebilir ve alışveriş yapabilir.",
                R.drawable.university_intro,
                Color.parseColor("#10B981") // Emerald Green image bg
            ),
            OnboardingSlide(
                "<font color='#FF5400'>Güvenli</font> ve Hızlı İletişim",
                "Satıcılarla doğrudan WhatsApp veya telefon üzerinden anında iletişime geçin. İlanları kolayca inceleyin.",
                R.drawable.safety_intro,
                Color.parseColor("#F57C00") // Deep Orange image bg
            )
        )

        val adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter

        setupDots(slides.size)
        updateDots(0)

        // Initial button text
        btnNext.text = "İleri →"

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)

                if (position == slides.size - 1) {
                    btnNext.text = "Başla →"
                    btnSkip.visibility = View.INVISIBLE
                } else {
                    btnNext.text = "İleri →"
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < slides.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                completeOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            completeOnboarding()
        }

        tvGoToLogin.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun setupDots(count: Int) {
        llDots.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            dot.layoutParams = params
            dot.setImageResource(R.drawable.dot_inactive)
            llDots.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        for (i in 0 until llDots.childCount) {
            val dot = llDots.getChildAt(i) as ImageView
            if (i == position) {
                dot.setImageResource(R.drawable.dot_active)
            } else {
                dot.setImageResource(R.drawable.dot_inactive)
            }
        }
    }

    private fun completeOnboarding() {
        val sharedPrefs = getSharedPreferences("UniPazarPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
        proceedToLoginOrMain()
    }

    private fun proceedToLoginOrMain() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
