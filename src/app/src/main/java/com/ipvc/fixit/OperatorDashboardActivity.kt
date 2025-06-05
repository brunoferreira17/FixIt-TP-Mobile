package com.ipvc.fixit

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.repository.FaultRepository
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.viewmodel.FaultViewModel
import com.ipvc.fixit.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OperatorDashboardActivity : ComponentActivity() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var faultViewModel: FaultViewModel
    private lateinit var userRoleText: TextView
    private lateinit var issuesContainer: LinearLayout

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
        setContentView(R.layout.activity_operator_dashboard)

        val languageSwitcher = findViewById<TextView>(R.id.languageSelector)
        val reportIssueButton = findViewById<Button>(R.id.reportIssueButton)
        userRoleText = findViewById(R.id.userRole)
        issuesContainer = findViewById(R.id.issuesContainer)

        val db = AppDatabase.getDatabase(this)
        userViewModel = UserViewModel(UserRepository(db.userDao()))
        faultViewModel = FaultViewModel(FaultRepository(db.faultDao()))

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        reportIssueButton.setOnClickListener {
            startActivity(Intent(this, NewIssueActivity::class.java))
        }

        val userId = SessionManager.getLoggedUserId(this)
        if (userId == null) {
            Toast.makeText(this, "Utilizador não autenticado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val user = userViewModel.getUserById(userId)
            if (user == null) {
                Toast.makeText(this@OperatorDashboardActivity, "Utilizador não encontrado", Toast.LENGTH_SHORT).show()
                return@launch
            }

            userRoleText.text = getString(R.string.user_role_prefix) + " " + user.name

            // Carrega as avarias deste utilizador
            faultViewModel.loadByReporter(user.userId.toIntOrNull() ?: -1)

            // Observa o fluxo de dados (StateFlow)
            faultViewModel.faults.collect { faults ->
                issuesContainer.removeAllViews()

                faults.sortedByDescending { it.reportedAt }.forEach { fault ->
                    val view = LayoutInflater.from(this@OperatorDashboardActivity)
                        .inflate(R.layout.component_issue_item, issuesContainer, false)

                    val translatedStatus = translateStatus(fault.status)
                    val title = "Equipamento #${fault.equipmentId} - $translatedStatus"
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(fault.reportedAt))

                    view.findViewById<TextView>(R.id.issueTitleAndStatus).text = title
                    view.findViewById<TextView>(R.id.issueDate).text = date

                    issuesContainer.addView(view)
                }

                syncWithSupabase(faults.filter { !it.syncStatus }, user.userId)
            }
        }
    }

    private fun syncWithSupabase(unsynced: List<Fault>, userId: String) {
        if (!SupabaseClientInstance.isConnectedToInternet(this)) return

        lifecycleScope.launch(Dispatchers.IO) {
            unsynced.forEach { fault ->
                val success = SupabaseClientInstance.syncFault(fault, userId)
                if (success) {
                    faultViewModel.markAsSynced(fault.faultId)
                }
            }
        }
    }

    private fun translateStatus(status: String): String {
        return when (status) {
            "pending" -> getString(R.string.status_pending)
            "in_progress" -> getString(R.string.status_in_progress)
            "resolved" -> getString(R.string.status_resolved)
            else -> status
        }
    }
}
