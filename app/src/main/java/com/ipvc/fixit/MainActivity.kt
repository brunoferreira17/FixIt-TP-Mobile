package com.ipvc.fixit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val langEN = findViewById<TextView>(R.id.langEN)
        val langPT = findViewById<TextView>(R.id.langPT)

        val currentLocale = resources.configuration.locales[0].language
        if (currentLocale == "pt") {
            langPT.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            langEN.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            langEN.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            langPT.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        langEN.setOnClickListener {
            setLocale("en")
            langEN.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            langPT.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        langPT.setOnClickListener {
            setLocale("pt")
            langPT.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            langEN.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        recreate()
    }
}