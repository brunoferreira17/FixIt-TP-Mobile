package com.ipvc.fixit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.repository.FaultRepository
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.syncAllPendingFaults
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.FaultViewModel
import com.ipvc.fixit.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TechnicalIssueActivity : AppCompatActivity() {

    private lateinit var faultViewModel: FaultViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var issueContainer: LinearLayout
    private lateinit var languageSwitcher: TextView
    private lateinit var userRoleText: TextView
    private var expandedCard: View? = null
    private lateinit var urgencyLevel: String
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
        setContentView(R.layout.activity_technical_issue)

        issueContainer = findViewById(R.id.issueDetailContainer)
        userRoleText = findViewById(R.id.userRole)
        languageSwitcher = findViewById(R.id.languageSwitcher)

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        setupBottomNavBar()

        urgencyLevel = intent.getStringExtra("urgency_level") ?: ""
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
                userRoleText.text = getString(R.string.user_role_prefix_technical) + " " + user.name
            }
        }

        loadFilteredIssues()
    }

    override fun onResume() {
        super.onResume()
        syncAllPendingFaults(this, lifecycleScope)
    }

    private fun loadFilteredIssues() {
        lifecycleScope.launch {
            faultViewModel.loadAllAssignedTo(userId)
            faultViewModel.faults.collect { faults ->
                val filtered = faults.filter { it.urgency.equals(urgencyLevel, ignoreCase = true) }

                issueContainer.removeAllViews()
                filtered.forEach { fault ->
                    val cardView = LayoutInflater.from(this@TechnicalIssueActivity)
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
            Toast.makeText(this, "${getString(R.string.view_details)} #${fault.faultId}", Toast.LENGTH_SHORT).show()
        }

        card.findViewById<Button>(R.id.sendMessageButton).setOnClickListener {
            Toast.makeText(this, "${getString(R.string.send_message)} #${fault.faultId}", Toast.LENGTH_SHORT).show()
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
                            if (success) {
                                faultViewModel.markAsSynced(updated.faultId)
                            }
                        }

                        Toast.makeText(this@TechnicalIssueActivity, getString(R.string.status_updated), Toast.LENGTH_SHORT).show()
                        loadFilteredIssues()
                    }
                }
                dialog.dismiss()
            }
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
        }

        card.findViewById<Button>(R.id.editDescriptionButton).setOnClickListener {
            val input = EditText(this)
            input.setText(fault.description)

            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.edit_description))
            builder.setView(input)

            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                val newDesc = input.text.toString().trim()
                if (newDesc.isNotBlank() && newDesc != fault.description) {
                    val updated = fault.copy(
                        description = newDesc,
                        syncStatus = SupabaseClientInstance.isConnectedToInternet(this) ?: false
                    )
                    lifecycleScope.launch {
                        faultViewModel.update(updated)

                        if (updated.syncStatus) {
                            val success = SupabaseClientInstance.syncFault(updated, updated.reportedBy)
                            if (success) {
                                faultViewModel.markAsSynced(updated.faultId)
                            }
                        }

                        Toast.makeText(this@TechnicalIssueActivity, getString(R.string.description_updated), Toast.LENGTH_SHORT).show()
                        loadFilteredIssues()
                    }
                }
            }

            builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
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