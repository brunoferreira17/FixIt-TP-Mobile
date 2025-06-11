package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.viewmodel.UserViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var showPasswordButton: ImageView
    private lateinit var languageSwitcher: TextView
    private lateinit var goToRegisterText: TextView

    private var passwordVisible = false
    private lateinit var viewModel: UserViewModel

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
        setContentView(R.layout.activity_login)

        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        loginButton = findViewById(R.id.loginButton)
        showPasswordButton = findViewById(R.id.showPasswordButton)
        languageSwitcher = findViewById(R.id.languageSelector)
        goToRegisterText = findViewById(R.id.goToRegisterText)

        val userDao = AppDatabase.getDatabase(this).userDao()
        val repository = UserRepository(userDao)
        viewModel = UserViewModel(repository)

        showPasswordButton.setOnClickListener {
            passwordVisible = !passwordVisible
            passwordField.inputType = if (passwordVisible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordField.setSelection(passwordField.text.length)
            showPasswordButton.setImageResource(
                if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.login(email, password, this)
            }
        }

        goToRegisterText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        lifecycleScope.launch {
            viewModel.loggedUser.collectLatest { user ->
                user?.let {
                    SessionManager.saveUserId(this@LoginActivity, it.userId)
                    SessionManager.saveUserRole(this@LoginActivity, it.role)

                    redirectToDashboard(it.role)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.loginError.collectLatest { errorKey ->
                errorKey?.let {
                    val message = when (it) {
                        "error_email_not_confirmed" -> getString(R.string.error_email_not_confirmed)
                        "error_invalid_credentials" -> getString(R.string.error_invalid_credentials)
                        else -> getString(R.string.error_generic)
                    }
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }
    }

    private fun redirectToDashboard(role: String) {
        val intent = when (role.lowercase()) {
            "operator" -> Intent(this, OperatorDashboardActivity::class.java)
            "technical" -> Intent(this, TechnicalDashboardActivity::class.java)
            "manager" -> Intent(this, ManagerDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
