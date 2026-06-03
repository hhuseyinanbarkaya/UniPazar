package com.example.unipazar

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etRegisterName = findViewById<TextInputEditText>(R.id.etRegisterName)
        val etRegisterEmail = findViewById<TextInputEditText>(R.id.etRegisterEmail)
        val etRegisterPassword = findViewById<TextInputEditText>(R.id.etRegisterPassword)
        val etRegisterConfirmPassword = findViewById<TextInputEditText>(R.id.etRegisterConfirmPassword)
        val btnRegisterSubmit = findViewById<MaterialButton>(R.id.btnRegisterSubmit)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegisterSubmit.setOnClickListener {
            val name = etRegisterName.text.toString().trim()
            val email = etRegisterEmail.text.toString().trim()
            val password = etRegisterPassword.text.toString().trim()
            val confirmPassword = etRegisterConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Ad Soyad alanı boş bırakılamaz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!email.endsWith(".edu.tr")) {
                Toast.makeText(this, "Sadece .edu.tr uzantılı okul e-postaları kayıt olabilir.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Şifre en az 6 karakter olmalıdır.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Şifreler uyuşmuyor. Lütfen kontrol edin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegisterSubmit.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            saveUserProfile(user.uid, email, name)
                        } else {
                            Toast.makeText(this, "Kayıt Başarılı! Hoşgeldiniz.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finishAffinity()
                        }
                    } else {
                        btnRegisterSubmit.isEnabled = true
                        Toast.makeText(this, "Kayıt Hatası: ${translateAuthError(task.exception)}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun saveUserProfile(uid: String, email: String, name: String) {
        // Try parsing university name from domain, e.g. std.okan.edu.tr -> okan
        val domain = email.split("@").getOrNull(1) ?: ""
        var university = domain.substringBefore(".edu.tr")
        if (university.startsWith("std.")) {
            university = university.substringAfter("std.")
        }
        university = university.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val userProfile = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "phone" to "",
            "bio" to "",
            "university" to university
        )

        db.collection("users").document(uid).set(userProfile)
            .addOnCompleteListener {
                Toast.makeText(this, "Kayıt Başarılı! Hoşgeldiniz.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
    }

    private fun translateAuthError(exception: java.lang.Exception?): String {
        if (exception == null) return "Bilinmeyen bir hata oluştu."
        return when (exception) {
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> 
                "Bu e-posta adresi zaten kullanımda. Lütfen giriş yapmayı deneyin."
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
}
