package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.repository.FaultRepository
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.FaultViewModel
import com.ipvc.fixit.viewmodel.UserViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class ManagerIssueDetailsActivity : AppCompatActivity() {

    private lateinit var faultViewModel: FaultViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var issueContainer: LinearLayout
    private lateinit var languageSwitcher: TextView
    private lateinit var userRoleText: TextView
    private var expandedCard: View? = null
    private lateinit var userId: String

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
        setContentView(R.layout.activity_manager_issues)

        issueContainer = findViewById(R.id.issueDetailContainer)
        userRoleText = findViewById(R.id.userRole)
        languageSwitcher = findViewById(R.id.languageSwitcher)

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        setupBottomNavBar()

        userId = SessionManager.getLoggedUserId(this) ?: ""

        if (userId.isBlank()) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        faultViewModel = FaultViewModel(FaultRepository(db.faultDao()))
        userViewModel = UserViewModel(UserRepository(db.userDao()))

        lifecycleScope.launch {
            val user = userViewModel.getUserById(userId)
            if (user != null) {
                userRoleText.text = getString(R.string.user_role_prefix_manager) + " " + user.name
            }
        }

        loadAllIssues()
    }

    private fun loadAllIssues() {
        lifecycleScope.launch {
            faultViewModel.loadAll()
            faultViewModel.faults.collect { faults ->
                issueContainer.removeAllViews()
                faults.forEach { fault ->
                    val cardView = LayoutInflater.from(this@ManagerIssueDetailsActivity)
                        .inflate(R.layout.issue_card_item, issueContainer, false)

                    bindCard(cardView, fault)
                    issueContainer.addView(cardView)
                }
            }
        }
    }

    private fun bindCard(card: View, fault: Fault) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        card.findViewById<TextView>(R.id.equipmentText).text = "${getString(R.string.equipment)} #${fault.equipmentId}"
        card.findViewById<TextView>(R.id.locationText).text = "${getString(R.string.location)}: ${fault.location}"
        card.findViewById<TextView>(R.id.dateText).text = "${getString(R.string.date)}: ${sdf.format(Date(fault.reportedAt))}"
        card.findViewById<TextView>(R.id.statusText).text = "${getString(R.string.status)}: ${fault.status.capitalize()}"
        card.findViewById<TextView>(R.id.descriptionText).text = "${getString(R.string.description)}: ${fault.description}"

        val buttonContainer = card.findViewById<LinearLayout>(R.id.buttonContainer)

        card.setOnClickListener {
            if (expandedCard != null && expandedCard != card) {
                collapseCard(expandedCard!!)
            }

            if (buttonContainer.visibility == View.GONE) {
                expandCard(card)
            } else {
                collapseCard(card)
            }
        }

        card.findViewById<Button>(R.id.viewDetailsButton).setOnClickListener {
            val intent = Intent(this, IssueInfoActivity::class.java)
            intent.putExtra("fault_id", fault.faultId)
            startActivity(intent)
        }

        card.findViewById<Button>(R.id.sendMessageButton).setOnClickListener {
            val input = EditText(this@ManagerIssueDetailsActivity)
            input.hint = getString(R.string.type_your_message)

            val dialog = android.app.AlertDialog.Builder(this@ManagerIssueDetailsActivity)
                .setTitle(getString(R.string.send_message))
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val messageText = input.text.toString().trim()
                    if (messageText.isNotBlank()) {
                        if (SupabaseClientInstance.isConnectedToInternet(this@ManagerIssueDetailsActivity)) {
                            lifecycleScope.launch {
                                val success = sendMessageToChat(fault, messageText)
                                val toastMsg = if (success)
                                    getString(R.string.message_sent)
                                else
                                    getString(R.string.message_send_failed)
                                Toast.makeText(this@ManagerIssueDetailsActivity, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@ManagerIssueDetailsActivity, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            dialog.show()
        }

        card.findViewById<Button>(R.id.editDescriptionButton).text = getString(R.string.assign_to_other_technician)

        card.findViewById<Button>(R.id.editDescriptionButton).setOnClickListener {
            lifecycleScope.launch {
                val techUsers: List<User> = userViewModel.getAllTechnicians()
                if (techUsers.isEmpty()) {
                    Toast.makeText(this@ManagerIssueDetailsActivity, getString(R.string.no_technicians_available), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val technicianNames = techUsers.map { it.name }
                val technicianIds = techUsers.map { it.userId }

                val selectedIndex = intArrayOf(-1)

                val builder = android.app.AlertDialog.Builder(this@ManagerIssueDetailsActivity)
                builder.setTitle(getString(R.string.assign_to_other_technician))
                builder.setSingleChoiceItems(technicianNames.toTypedArray(), -1) { _, which ->
                    selectedIndex[0] = which
                }

                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    if (selectedIndex[0] != -1) {
                        val selectedTechId = technicianIds[selectedIndex[0]]
                        val updated = fault.copy(
                            assignedTo = selectedTechId,
                            syncStatus = SupabaseClientInstance.isConnectedToInternet(this@ManagerIssueDetailsActivity) ?: false
                        )
                        lifecycleScope.launch {
                            faultViewModel.update(updated)

                            if (updated.syncStatus) {
                                val success = SupabaseClientInstance.syncFault(updated, fault.reportedBy)
                                if (success) {
                                    faultViewModel.markAsSynced(updated.faultId)
                                }
                            }

                            Toast.makeText(this@ManagerIssueDetailsActivity, getString(R.string.technician_assigned), Toast.LENGTH_SHORT).show()
                            loadAllIssues()
                        }
                    }
                }

                builder.setNegativeButton(android.R.string.cancel, null)
                builder.show()
            }
        }

        card.findViewById<Button>(R.id.changeStatusButton).setOnClickListener {
            val statuses = arrayOf("pending", "in_progress", "resolved")
            val currentStatusIndex = statuses.indexOf(fault.status)

            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.change_status))
            builder.setSingleChoiceItems(statuses, currentStatusIndex) { dialog, which ->
                val newStatus = statuses[which]
                if (newStatus != fault.status) {
                    val updated = fault.copy(
                        status = newStatus,
                        syncStatus = SupabaseClientInstance.isConnectedToInternet(this) ?: false
                    )
                    lifecycleScope.launch {
                        faultViewModel.update(updated)

                        if (updated.syncStatus) {
                            val success = SupabaseClientInstance.syncFault(updated, updated.reportedBy)
                            if (success) faultViewModel.markAsSynced(updated.faultId)
                        }

                        Toast.makeText(this@ManagerIssueDetailsActivity, getString(R.string.status_updated), Toast.LENGTH_SHORT).show()
                        loadAllIssues()
                    }
                }
                dialog.dismiss()
            }
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
        }

    }

    private suspend fun sendMessageToChat(fault: Fault, messageText: String): Boolean {
        return try {
            val json = buildJsonObject {
                put("faultid", fault.faultId)
                put("senderid", userId)
                put("message", messageText)
                put("sentat", System.currentTimeMillis())
            }

            SupabaseClientInstance.client.postgrest
                .from("Message")
                .insert(json)

            true
        } catch (e: Exception) {
            Log.e("SendMessage", "Erro ao enviar mensagem: ${e.message}", e)
            false
        }
    }

    private fun expandCard(card: View) {
        val buttonContainer = card.findViewById<LinearLayout>(R.id.buttonContainer)
        val slideDown = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        buttonContainer.visibility = View.VISIBLE
        buttonContainer.startAnimation(slideDown)
        expandedCard = card
    }

    private fun collapseCard(card: View) {
        val buttonContainer = card.findViewById<LinearLayout>(R.id.buttonContainer)
        buttonContainer.visibility = View.GONE
        expandedCard = null
    }
}
