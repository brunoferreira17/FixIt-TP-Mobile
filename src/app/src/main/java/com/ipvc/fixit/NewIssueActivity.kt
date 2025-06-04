package com.ipvc.fixit

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.Equipment
import com.ipvc.fixit.repository.EquipmentRepository
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.viewmodel.EquipmentViewModel
import com.ipvc.fixit.viewmodel.UserViewModel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.Locale

class NewIssueActivity : ComponentActivity() {

    private lateinit var languageSelector: TextView
    private lateinit var userRoleText: TextView
    private lateinit var equipmentSpinner: Spinner
    private lateinit var urgencySpinner: Spinner
    private lateinit var descriptionField: EditText
    private lateinit var photoButton: Button
    private lateinit var submitButton: Button
    private lateinit var backButton: Button

    private lateinit var userViewModel: UserViewModel
    private lateinit var equipmentViewModel: EquipmentViewModel

    private var selectedEquipment: Equipment? = null
    private var selectedUrgency: String? = null
    private var selectedPhotoUri: Uri? = null
    private var selectedPhotoUrl: String? = null

    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_issue_dashboard)

        languageSelector = findViewById(R.id.languageSelector)
        userRoleText = findViewById(R.id.userRole)
        equipmentSpinner = findViewById(R.id.equipmentSpinner)
        urgencySpinner = findViewById(R.id.urgencySpinner)
        descriptionField = findViewById(R.id.descriptionField)
        photoButton = findViewById(R.id.photoButton)
        submitButton = findViewById(R.id.submitButton)
        backButton = findViewById(R.id.backButton)

        val database = AppDatabase.getDatabase(this)
        userViewModel = UserViewModel(UserRepository(database.userDao()))
        equipmentViewModel = EquipmentViewModel(EquipmentRepository(database.equipmentDao()))

        languageSelector.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        backButton.setOnClickListener { finish() }

        val loggedUserId = SessionManager.getLoggedUserId(this)
        if (loggedUserId != null) {
            lifecycleScope.launch {
                val user = userViewModel.getUserById(loggedUserId)
                user?.let {
                    userRoleText.text = getString(R.string.user_role_prefix) + " " + it.name
                }
            }
        }

        lifecycleScope.launch {
            val equipmentList = equipmentViewModel.getAllEquipments()
            val names = equipmentList.map { it.name }
            val adapter = ArrayAdapter(this@NewIssueActivity, android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            equipmentSpinner.adapter = adapter
            equipmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedEquipment = equipmentList[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedEquipment = null
                }
            }
        }

        val urgencies = listOf(
            getString(R.string.urgency_low),
            getString(R.string.urgency_medium),
            getString(R.string.urgency_high)
        )
        val urgencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, urgencies)
        urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        urgencySpinner.adapter = urgencyAdapter
        urgencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedUrgency = when (urgencies[position]) {
                    getString(R.string.urgency_low) -> "low"
                    getString(R.string.urgency_medium) -> "medium"
                    getString(R.string.urgency_high) -> "high"
                    else -> "low"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedUrgency = null
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedPhotoUri = uri
            Toast.makeText(this, getString(R.string.photo_uploaded), Toast.LENGTH_SHORT).show()
        }

        photoButton.setOnClickListener {
            openGallery()
        }

        submitButton.setOnClickListener {
            val description = descriptionField.text.toString().trim()
            if (selectedEquipment == null || description.isBlank() || selectedUrgency == null) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loggedUserId2 = SessionManager.getLoggedUserId(this)
            if (loggedUserId2 == null) {
                Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (selectedPhotoUri != null) {
                    selectedPhotoUrl = uploadPhotoToSupabase(selectedPhotoUri!!)
                }

                val user = userViewModel.getUserById(loggedUserId2)
                if (user == null) {
                    Toast.makeText(this@NewIssueActivity, "Utilizador não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val fault = com.ipvc.fixit.entities.Fault(
                    equipmentId = selectedEquipment!!.equipmentId,
                    reportedBy = (user.userId.toIntOrNull() ?: -1).toString(),
                    assignedTo = null,
                    description = description,
                    photo = selectedPhotoUrl,
                    urgency = selectedUrgency!!,
                    location = selectedEquipment!!.location,
                    status = "pending",
                    reportedAt = System.currentTimeMillis(),
                    resolvedAt = null,
                    syncStatus = SupabaseClientInstance.isConnectedToInternet(this@NewIssueActivity)
                )

                database.faultDao().insertFault(fault)

                if (fault.syncStatus) {
                    val success = SupabaseClientInstance.syncFault(fault, user.userId)
                    if (!success) {
                        Toast.makeText(this@NewIssueActivity, "Erro ao sincronizar com Supabase", Toast.LENGTH_SHORT).show()
                    }
                }

                Toast.makeText(this@NewIssueActivity, getString(R.string.issue_reported_successfully), Toast.LENGTH_SHORT).show()
                descriptionField.text.clear()
                equipmentSpinner.setSelection(0)
                urgencySpinner.setSelection(0)
                selectedEquipment = null
                selectedUrgency = null
                selectedPhotoUrl = null
                selectedPhotoUri = null

                startActivity(Intent(this@NewIssueActivity, OperatorDashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private suspend fun uploadPhotoToSupabase(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileBytes = inputStream.readBytes()
            inputStream.close()

            val fileName = "faults/foto_${System.currentTimeMillis()}.jpg"
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
            null
        }
    }
}
