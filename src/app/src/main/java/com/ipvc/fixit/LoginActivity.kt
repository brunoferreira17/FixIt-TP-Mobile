package com.ipvc.fixit

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.viewmodel.UserViewModel
import java.util.Locale

class LoginActivity : ComponentActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var showPasswordButton: ImageView
    private lateinit var languageSwitcher: TextView

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
        languageSwitcher = findViewById(R.id.textView3)

        val userDao = AppDatabase.getDatabase(this).userDao()
        val repository = UserRepository(userDao)
        viewModel = UserViewModel(repository)

        showPasswordButton.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                passwordField.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showPasswordButton.setImageResource(R.drawable.ic_eye_off)
            } else {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                showPasswordButton.setImageResource(R.drawable.ic_eye)
            }
            passwordField.setSelection(passwordField.text.length)
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Preenche todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.login(email, password)
            }
        }

        lifecycleScope.launch {
            viewModel.loggedUser.collectLatest { user ->
                if (user != null) {
                    Toast.makeText(this@LoginActivity, "Login como ${user.role}", Toast.LENGTH_SHORT).show()
                    // TODO: redirecionar
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciais inválidas", Toast.LENGTH_SHORT).show()
                }
            }
        }

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales.get(0).language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }
    }
}
