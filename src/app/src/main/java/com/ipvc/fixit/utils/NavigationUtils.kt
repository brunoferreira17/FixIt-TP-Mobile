package com.ipvc.fixit.utils

import android.content.Intent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ipvc.fixit.OperatorDashboardActivity
import com.ipvc.fixit.R
import com.ipvc.fixit.TechnicalDashboardActivity

fun AppCompatActivity.setupBottomNavBar() {
    val userRole = SessionManager.getLoggedUserRole(this) ?: return

    val homeBtn = findViewById<FrameLayout>(R.id.nav_home)
    val messagesBtn = findViewById<FrameLayout>(R.id.nav_messages)
    val reportsBtn = findViewById<FrameLayout>(R.id.nav_reports)
    val profileBtn = findViewById<FrameLayout>(R.id.nav_profile)

    homeBtn.setOnClickListener {
        val intent = when (userRole) {
            "operator" -> Intent(this, OperatorDashboardActivity::class.java)
            "technical" -> Intent(this, TechnicalDashboardActivity::class.java)
            //"manager" -> Intent(this, ManagerDashboardActivity::class.java)
            else -> null
        }
        intent?.let {
            startActivity(it)
            finish()
        }
    }

    messagesBtn.setOnClickListener {
        Toast.makeText(this, "Messages screen (por implementar)", Toast.LENGTH_SHORT).show()
    }

    reportsBtn.setOnClickListener {
        when (userRole) {
            "operator" -> {
                //startActivity(Intent(this, OperatorReportsActivity::class.java))
            }
            "technical" -> {
                //startActivity(Intent(this, TechnicianReportsActivity::class.java))
            }
            "manager" -> {
                //startActivity(Intent(this, ManagerReportsActivity::class.java))
            }
        }
    }

    profileBtn.setOnClickListener {
        //startActivity(Intent(this, ProfileActivity::class.java))
    }
}
