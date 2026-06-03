package com.example.unipazar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampusSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val etCampus = findViewById<TextInputEditText>(R.id.etCampus)
        val btnSaveCampus = findViewById<MaterialButton>(R.id.btnSaveCampus)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch current
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etCampus.setText(doc.getString("university") ?: "")
                }
            }

        btnSaveCampus.setOnClickListener {
            val newCampus = etCampus.text.toString().trim()
            if (newCampus.isEmpty()) {
                etCampus.error = "Kampüs adı boş olamaz"
                return@setOnClickListener
            }

            db.collection("users").document(currentUser.uid)
                .update("university", newCampus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Kampüs güncellendi!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
