package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ManagerDashboardActivity : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var userRoleText: TextView

    private lateinit var totalIssues: TextView
    private lateinit var pendingIssues: TextView
    private lateinit var inProgressIssues: TextView
    private lateinit var solvedIssues: TextView
    private lateinit var mostProblematic: TextView

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
        setContentView(R.layout.activity_manager_dashboard)

        val db = AppDatabase.getDatabase(this)
        val faultDao = db.faultDao()
        val equipmentDao = db.equipmentDao()
        userViewModel = UserViewModel(UserRepository(db.userDao()))

        userRoleText = findViewById(R.id.userRole)
        val languageSwitcher = findViewById<TextView>(R.id.languageSwitcher)

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
                userRoleText.text = getString(R.string.user_role_prefix_manager) + " " + user.name
            }

            // Carregar estatísticas reais da base de dados
            withContext(Dispatchers.IO) {
                val allFaults = faultDao.getAllFaults()
                val total = allFaults.size
                val pending = allFaults.count { it.status.trim().lowercase() == "pending" }
                val inProgress = allFaults.count { it.status.trim().lowercase() == "in_progress" }
                val solved = allFaults.count { it.status.trim().lowercase() == "solved" }

                // Contar avarias por equipamento
                val faultCountByEquipment = allFaults.groupingBy { it.equipmentId }.eachCount()
                val mostProblematicId = faultCountByEquipment.maxByOrNull { it.value }?.key
                val problematicEquipmentName = mostProblematicId?.let {
                    equipmentDao.getEquipmentById(it)?.name ?: getString(R.string.unknown)
                } ?: getString(R.string.none)

                withContext(Dispatchers.Main) {
                    updateOverviewStats(total, pending, inProgress, solved, problematicEquipmentName)
                }
            }
        }

        setupBottomNavBar()

        // Botões
        findViewById<CardView>(R.id.btnViewIssues).setOnClickListener {
            startActivity(Intent(this, ManagerIssueDetailsActivity::class.java))
        }

        findViewById<CardView>(R.id.btnAssignTechnicians).setOnClickListener {
            startActivity(Intent(this, AssignTechniciansActivity::class.java))
        }

        findViewById<CardView>(R.id.btnManageEquipments).setOnClickListener {
            startActivity(Intent(this, EquipmentListActivity::class.java))
        }

        findViewById<CardView>(R.id.btnCommunicateTeam).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

                totalIssues = findViewById(R.id.totalIssues)
                pendingIssues = findViewById(R.id.pendingIssues)
                inProgressIssues = findViewById(R.id.inProgressIssues)
                solvedIssues = findViewById(R.id.solvedIssues)
                mostProblematic = findViewById(R.id.mostProblematic)
    }

    private fun updateOverviewStats(total: Int, pending: Int, inProgress: Int, solved: Int, problematic: String) {
        totalIssues.text = getString(R.string.total_issues, total)
        pendingIssues.text = getString(R.string.pending_issues, pending)
        inProgressIssues.text = getString(R.string.in_progress_issues, inProgress)
        solvedIssues.text = getString(R.string.solved_issues, solved)
        mostProblematic.text = getString(R.string.most_problematic, problematic)
    }
}
