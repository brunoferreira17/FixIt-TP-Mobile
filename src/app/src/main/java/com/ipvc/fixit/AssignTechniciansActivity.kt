package com.ipvc.fixit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.UserViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AssignTechniciansActivity : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var techniciansContainer: LinearLayout
    private lateinit var operatorsContainer: LinearLayout
    private lateinit var userRoleText: TextView
    private lateinit var languageSwitcher: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_technicians)

        techniciansContainer = findViewById(R.id.techniciansContainer)
        operatorsContainer = findViewById(R.id.operatorsContainer)
        userRoleText = findViewById(R.id.userRole)
        languageSwitcher = findViewById(R.id.languageSwitcher)

        languageSwitcher.setOnClickListener {
            val currentLang = resources.configuration.locales[0].language
            val newLang = if (currentLang == "pt") "en" else "pt"
            val locale = java.util.Locale(newLang)
            java.util.Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
            recreate()
        }

        setupBottomNavBar()

        val db = AppDatabase.getDatabase(this)
        userViewModel = UserViewModel(UserRepository(db.userDao()))

        val userId = SessionManager.getLoggedUserId(this)
        if (userId == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val user = userViewModel.getUserById(userId)
            user?.let {
                userRoleText.text = getString(R.string.user_role_prefix_manager) + " " + it.name
            }

            loadUsers()
        }
    }

    private suspend fun loadUsers() {
        val allUsers = userViewModel.getAllUsers()
        val technicians = allUsers.filter { it.role == "Technical" }
        val operators = allUsers.filter { it.role == "Operator" }

        val inflater = LayoutInflater.from(this)

        for (user in technicians) {
            val card = createUserCard(inflater, user, true)
            techniciansContainer.addView(card)
        }

        for (user in operators) {
            val card = createUserCard(inflater, user, false)
            operatorsContainer.addView(card)
        }
    }

    private fun createUserCard(inflater: LayoutInflater, user: User, isTechnical: Boolean): View {
        val cardView = inflater.inflate(R.layout.item_user_card, null, false)

        cardView.findViewById<TextView>(R.id.userName).text = user.name
        cardView.findViewById<TextView>(R.id.userEmail).text = user.email
        cardView.findViewById<TextView>(R.id.userRole).text =
            if (isTechnical) getString(R.string.technicians) else getString(R.string.operators)

        val actionButton = cardView.findViewById<Button>(R.id.actionButton)
        actionButton.text = if (isTechnical) getString(R.string.demote) else getString(R.string.promote)

        actionButton.setOnClickListener {
            val newRole = if (isTechnical) "Operator" else "Technical"
            val updatedUser = user.copy(role = newRole)

            lifecycleScope.launch {
                userViewModel.updateUser(updatedUser)

                try {
                    val json = buildJsonObject {
                        put("role", JsonPrimitive(newRole))
                    }

                    SupabaseClientInstance.client.postgrest
                        .from("Users")
                        .update(json) {
                            filter { eq("id", user.userId) }
                        }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@AssignTechniciansActivity,
                        "Erro ao sincronizar Supabase: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Toast.makeText(
                    this@AssignTechniciansActivity,
                    if (newRole == "Technical") getString(R.string.promoted_success)
                    else getString(R.string.demoted_success),
                    Toast.LENGTH_SHORT
                ).show()
                recreate()
            }
        }

        return cardView
    }
}
