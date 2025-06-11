package com.ipvc.fixit

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.FaultRepository
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.FaultViewModel
import com.ipvc.fixit.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IssueInfoActivity : AppCompatActivity() {

    private lateinit var faultViewModel: FaultViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var userRoleText: TextView
    private lateinit var languageSwitcher: TextView

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
        setContentView(R.layout.activity_issue_info)

        // Views
        userRoleText = findViewById(R.id.userRole)
        languageSwitcher = findViewById(R.id.languageSelector)
        val backButton = findViewById<Button>(R.id.backButton)

        val equipmentText = findViewById<TextView>(R.id.equipmentText)
        val locationText = findViewById<TextView>(R.id.locationText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        val statusText = findViewById<TextView>(R.id.statusText)
        val reportedAtText = findViewById<TextView>(R.id.reportedAtText)
        val resolvedText = findViewById<TextView>(R.id.resolvedText)
        val imageView = findViewById<ImageView>(R.id.issueImage)

        // DB e ViewModels
        val db = AppDatabase.getDatabase(this)
        faultViewModel = FaultViewModel(FaultRepository(db.faultDao()))
        userViewModel = UserViewModel(UserRepository(db.userDao()))

        // Linguagem
        languageSwitcher.setOnClickListener {
            val currentLang = resources.configuration.locales[0].language
            val newLang = if (currentLang == "pt") "en" else "pt"
            setLocale(newLang)
        }

        // Botão voltar
        backButton.setOnClickListener {
            finish()
        }

        // Nome do utilizador
        val userId = SessionManager.getLoggedUserId(this)
        if (userId != null) {
            lifecycleScope.launch {
                val user = userViewModel.getUserById(userId)
                user?.let {
                    val prefix = when (it.role.lowercase()) {
                        "operator" -> getString(R.string.user_role_prefix)
                        "technical" -> getString(R.string.user_role_prefix_technical)
                        "manager" -> "Manager"
                        else -> ""
                    }
                    userRoleText.text = "$prefix ${it.name}"
                }
            }
        }

        // Obter ID da fault
        val faultId = intent.getIntExtra("fault_id", -1)
        if (faultId == -1) {
            Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mostrar dados da fault
        lifecycleScope.launch {
            val fault = faultViewModel.getById(faultId)
            fault?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                equipmentText.text = "${getString(R.string.equipment)}: #${it.equipmentId}"
                locationText.text = "${getString(R.string.location)}: ${it.location}"
                descriptionText.text = "${getString(R.string.description)}: ${it.description}"
                statusText.text = "${getString(R.string.status)}: ${it.status.capitalize()}"
                reportedAtText.text = "${getString(R.string.date)}: ${sdf.format(Date(it.reportedAt))}"

                if (it.resolvedAt != null) {
                    resolvedText.text = "${getString(R.string.resolved)}: ${sdf.format(Date(it.resolvedAt))}"
                    resolvedText.visibility = TextView.VISIBLE
                }

                if (!it.photo.isNullOrBlank()) {
                    imageView.visibility = ImageView.VISIBLE
                    Glide.with(this@IssueInfoActivity)
                        .load(it.photo)
                        .placeholder(R.drawable.default_photo)
                        .error(R.drawable.default_photo)
                        .into(imageView)
                }
            }
        }

        setupBottomNavBar()
    }
}
