package com.ipvc.fixit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.UserViewModel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var roleText: TextView
    private lateinit var profilePhoto: ImageView
    private lateinit var removePhotoButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var editButton: Button
    private lateinit var logoutButton: Button
    private lateinit var userViewModel: UserViewModel

    private var currentUser: User? = null
    private var selectedPhotoUri: Uri? = null

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedPhotoUri = it
                profilePhoto.setImageURI(it)
                removePhotoButton.visibility = ImageButton.VISIBLE
            }
        }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val db = AppDatabase.getDatabase(this)
        userViewModel = UserViewModel(UserRepository(db.userDao()))

        nameEdit = findViewById(R.id.nameEdit)
        emailEdit = findViewById(R.id.emailEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        phoneEdit = findViewById(R.id.phoneEdit)
        roleText = findViewById(R.id.roleText)
        profilePhoto = findViewById(R.id.profilePhoto)
        removePhotoButton = findViewById(R.id.removePhotoButton)
        saveButton = findViewById(R.id.saveButton)
        editButton = findViewById(R.id.editProfileButton)
        logoutButton = findViewById(R.id.logoutButton)

        findViewById<TextView>(R.id.languageSelector).setOnClickListener {
            val currentLang = resources.configuration.locales[0].language
            val newLang = if (currentLang == "pt") "en" else "pt"
            setLocale(newLang)
        }

        profilePhoto.setOnClickListener {
            if (nameEdit.isEnabled) {
                galleryLauncher.launch("image/*")
            }
        }

        setupBottomNavBar()

        removePhotoButton.setOnClickListener {
            profilePhoto.setImageResource(R.drawable.default_photo)
            selectedPhotoUri = null
            removePhotoButton.visibility = ImageButton.GONE
        }

        editButton.setOnClickListener {
            toggleEditing(true)
        }

        saveButton.setOnClickListener {
            saveProfileChanges()
        }

        logoutButton.setOnClickListener {
            SessionManager.clearSession(this)

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val userId = SessionManager.getLoggedUserId(this)
        if (userId == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            loadUserData(userId)
        }
    }

    private fun toggleEditing(enabled: Boolean) {
        nameEdit.isEnabled = enabled
        emailEdit.isEnabled = enabled
        passwordEdit.isEnabled = enabled
        phoneEdit.isEnabled = enabled
        saveButton.visibility = if (enabled) Button.VISIBLE else Button.GONE
        removePhotoButton.visibility = if (enabled && currentUser?.profilePhoto != null) ImageButton.VISIBLE else ImageButton.GONE
    }

    private fun loadUserData(userId: String) {
        lifecycleScope.launch {
            val user = userViewModel.getUserById(userId)
            if (user != null) {
                currentUser = user
                nameEdit.setText(user.name)
                emailEdit.setText(user.email)
                passwordEdit.setText(user.password)
                phoneEdit.setText(user.phone ?: "")
                roleText.text = getString(R.string.role_operator) + user.role

                Log.d("ProfileDebug", "Foto de perfil: ${user.profilePhoto}")

                if (user.profilePhoto.isNullOrEmpty()) {
                    profilePhoto.setImageResource(R.drawable.default_photo)
                } else {
                    Glide.with(this@ProfileActivity)
                        .load(user.profilePhoto)
                        .placeholder(R.drawable.default_photo)
                        .error(R.drawable.default_photo)
                        .into(profilePhoto)
                }
            }
        }
    }

    private fun saveProfileChanges() {
        val user = currentUser ?: return

        val updatedUser = user.copy(
            name = nameEdit.text.toString().trim(),
            email = emailEdit.text.toString().trim(),
            password = passwordEdit.text.toString().trim(),
            phone = phoneEdit.text.toString().trim(),
        )

        lifecycleScope.launch {
            val finalUser = if (selectedPhotoUri != null) {
                val photoUrl = uploadPhotoToSupabase(selectedPhotoUri!!, user.userId)
                updatedUser.copy(profilePhoto = photoUrl)
            } else {
                updatedUser.copy(profilePhoto = if (profilePhoto.drawable.constantState ==
                    getDrawable(R.drawable.default_photo)?.constantState
                ) null else user.profilePhoto)
            }

            val success = userViewModel.updateUser(finalUser)
            val remoteSuccess = SupabaseClientInstance.updateUserInSupabase(finalUser)

            if (success && remoteSuccess) {
                Toast.makeText(this@ProfileActivity, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                toggleEditing(false)
                loadUserData(user.userId)
            } else {
                Toast.makeText(this@ProfileActivity, getString(R.string.error_updating_profile), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadPhotoToSupabase(uri: Uri, userId: String): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileBytes = inputStream.readBytes()
            withContext(Dispatchers.IO) {
                inputStream.close()
            }

            val fileName = "profile_photos/user_$userId.jpg"
            val bucket = SupabaseClientInstance.client.storage.from("fixit")

            bucket.upload(
                path = fileName,
                data = fileBytes
            ) {
                upsert = true
            }

            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.image_upload_fail), Toast.LENGTH_SHORT).show()
            null
        }
    }
}
