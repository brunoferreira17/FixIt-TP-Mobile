package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.UserRepository
import com.ipvc.fixit.utils.UserRegistrationManager
import android.util.Log
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var registerButton: Button
    private lateinit var goToLoginText: TextView
    private lateinit var languageSwitcher: TextView

    private lateinit var userRepository: UserRepository

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
        setContentView(R.layout.activity_register)

        nameInput = findViewById(R.id.nameField)
        emailInput = findViewById(R.id.emailField)
        passwordInput = findViewById(R.id.passwordField)
        phoneInput = findViewById(R.id.phoneField)
        registerButton = findViewById(R.id.registerButton)
        goToLoginText = findViewById(R.id.goToLoginText)
        languageSwitcher = findViewById(R.id.languageSelector)

        val db = AppDatabase.getDatabase(applicationContext)
        userRepository = UserRepository(db.userDao())

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val userEmail = emailInput.text.toString().trim().replace("'", "").replace("\"", "")
            val userPassword = passwordInput.text.toString()
            val phone = phoneInput.text.toString()

            if (name.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUserWithSupabase(name, userEmail, userPassword, phone)
        }

        goToLoginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales.get(0).language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }
    }

    private fun registerUserWithSupabase(name: String, userEmail: String, userPassword: String, phone: String) {
        Log.d("EmailTest", "Email a enviar: [$userEmail]")
        UserRegistrationManager.registerUser(
            scope = lifecycleScope,
            name = name,
            userEmail = userEmail,
            userPassword = userPassword,
            phone = phone,
            role = "Operator",
            userRepository = userRepository,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Registo com sucesso!", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            },
            onError = { e ->
                runOnUiThread {
                    Toast.makeText(this, "Erro ao registar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
