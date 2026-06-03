package com.example.unipazar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Eğer kullanıcı zaten giriş yapmışsa direkt MainActivity'e geç
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!isValidEduEmail(email)) {
                Toast.makeText(this, "Lütfen geçerli bir .edu.tr uzantılı okul e-postası girin.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Şifre boş olamaz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    btnLogin.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Giriş Başarılı!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Giriş Hatası: ${translateAuthError(task.exception)}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }



        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun translateAuthError(exception: java.lang.Exception?): String {
        if (exception == null) return "Bilinmeyen bir hata oluştu."
        return when (exception) {
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> 
                "Bu e-posta adresi zaten kullanımda. Lütfen giriş yapmayı deneyin."
            is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> 
                "Hatalı e-posta veya şifre."
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> 
                "Şifre çok zayıf. En az 6 karakter olmalıdır."
            else -> {
                val msg = exception.message ?: ""
                if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true)) {
                    "İnternet bağlantısı hatası. Lütfen internetinizi kontrol edin."
                } else {
                    exception.localizedMessage ?: "Bir hata oluştu."
                }
            }
        }
    }

    private fun isValidEduEmail(email: String): Boolean {
        return email.isNotEmpty() && email.endsWith(".edu.tr")
    }
}
