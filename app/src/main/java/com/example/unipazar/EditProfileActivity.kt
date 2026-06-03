package com.example.unipazar

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var ivProfileAvatar: ImageView
    private lateinit var btnChangeAvatar: TextView
    private lateinit var etProfileName: TextInputEditText
    private lateinit var etProfilePhone: TextInputEditText
    private lateinit var etProfileUniversity: TextInputEditText
    private lateinit var etProfileBio: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var profileProgressBar: ProgressBar

    private var avatarUri: Uri? = null
    private var currentAvatarUrl: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            Glide.with(this).load(it).circleCrop().into(ivProfileAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarProfile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        etProfileName = findViewById(R.id.etProfileName)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        etProfileUniversity = findViewById(R.id.etProfileUniversity)
        etProfileBio = findViewById(R.id.etProfileBio)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        profileProgressBar = findViewById(R.id.profileProgressBar)

        loadUserProfile(currentUser.uid)

        btnChangeAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnSaveProfile.setOnClickListener {
            saveUserProfile(currentUser.uid, currentUser.email ?: "")
        }
    }

    private fun loadUserProfile(uid: String) {
        profileProgressBar.visibility = View.VISIBLE
        btnSaveProfile.isEnabled = false

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                profileProgressBar.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                if (document != null && document.exists()) {
                    etProfileName.setText(document.getString("name") ?: "")
                    etProfilePhone.setText(document.getString("phone") ?: "")
                    etProfileUniversity.setText(document.getString("university") ?: "")
                    etProfileBio.setText(document.getString("bio") ?: "")
                    
                    currentAvatarUrl = document.getString("avatarUrl") ?: ""
                    if (currentAvatarUrl.isNotEmpty()) {
                        Glide.with(this).load(currentAvatarUrl).circleCrop().into(ivProfileAvatar)
                    }
                }
            }
            .addOnFailureListener { e ->
                profileProgressBar.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                Toast.makeText(this, "Profil yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserProfile(uid: String, email: String) {
        val name = etProfileName.text.toString().trim()
        val phone = etProfilePhone.text.toString().trim()
        val university = etProfileUniversity.text.toString().trim()
        val bio = etProfileBio.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || university.isEmpty()) {
            Toast.makeText(this, "Lütfen gerekli alanları doldurun (*)", Toast.LENGTH_SHORT).show()
            return
        }

        profileProgressBar.visibility = View.VISIBLE
        btnSaveProfile.isEnabled = false

        if (avatarUri != null) {
            val ref = storage.reference.child("avatars/$uid.jpg")
            ref.putFile(avatarUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        saveToFirestore(uid, email, name, phone, university, bio, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    profileProgressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(this, "Fotoğraf yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveToFirestore(uid, email, name, phone, university, bio, currentAvatarUrl)
        }
    }

    private fun saveToFirestore(uid: String, email: String, name: String, phone: String, university: String, bio: String, avatarUrl: String) {
        val userProfile = hashMapOf(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "phone" to phone,
            "bio" to bio,
            "university" to university,
            "avatarUrl" to avatarUrl
        )

        db.collection("users").document(uid).set(userProfile, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                profileProgressBar.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                Toast.makeText(this, "Profiliniz başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                profileProgressBar.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                Toast.makeText(this, "Profil güncellenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
