package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.util.*

class TechnicalDashboardActivity : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var userRoleText: TextView

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
        setContentView(R.layout.activity_technical_dashboard)

        val db = AppDatabase.getDatabase(this)
        userViewModel = UserViewModel(UserRepository(db.userDao()))

        val languageSwitcher = findViewById<TextView>(R.id.languageSelector)
        userRoleText = findViewById(R.id.userRole)

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        val userId = SessionManager.getLoggedUserId(this)
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val user = userViewModel.getUserById(userId)
            if (user != null) {
                userRoleText.text = getString(R.string.user_role_prefix_technical) + " " + user.name
            }
        }

        setupBottomNavBar()
        val urgentCard = findViewById<CardView>(R.id.cardHighUrgency)
        val mediumCard = findViewById<CardView>(R.id.cardMediumUrgency)
        val minorCard = findViewById<CardView>(R.id.cardLowUrgency)

        urgentCard.setOnClickListener {
            openIssueActivity(getString(R.string.urgency_high))
        }

        mediumCard.setOnClickListener {
            openIssueActivity(getString(R.string.urgency_medium))
        }

        minorCard.setOnClickListener {
            openIssueActivity(getString(R.string.urgency_low))
        }
    }

    private fun openIssueActivity(urgency: String) {
        val intent = Intent(this, TechnicalIssueActivity::class.java)
        intent.putExtra("urgency_level", urgency)
        startActivity(intent)
    }
}
