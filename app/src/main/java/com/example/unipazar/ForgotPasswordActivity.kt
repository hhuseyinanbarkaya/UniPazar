package com.example.unipazar

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etResetEmail = findViewById<TextInputEditText>(R.id.etResetEmail)
        val btnSendResetLink = findViewById<MaterialButton>(R.id.btnSendResetLink)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnBack.setOnClickListener {
            finish()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        btnSendResetLink.setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            if (email.isEmpty()) {
                etResetEmail.error = "Lütfen e-posta adresinizi girin."
                return@setOnClickListener
            }

            btnSendResetLink.isEnabled = false
            btnSendResetLink.text = "Gönderiliyor..."

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sıfırlama bağlantısı e-postanıza gönderildi.", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSendResetLink.isEnabled = true
                    btnSendResetLink.text = "Sıfırlama Bağlantısı Gönder"
                    Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
