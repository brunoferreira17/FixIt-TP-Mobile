package com.ipvc.fixit.utils

import android.content.Intent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.ipvc.fixit.ManagerDashboardActivity
import com.ipvc.fixit.MessagesActivity
import com.ipvc.fixit.OperatorDashboardActivity
import com.ipvc.fixit.ProfileActivity
import com.ipvc.fixit.R
import com.ipvc.fixit.TechnicalDashboardActivity

fun AppCompatActivity.setupBottomNavBar() {
    val userRole = SessionManager.getLoggedUserRole(this) ?: return

    val homeBtn = findViewById<FrameLayout>(R.id.nav_home)
    val messagesBtn = findViewById<FrameLayout>(R.id.nav_messages)
    val profileBtn = findViewById<FrameLayout>(R.id.nav_profile)

    homeBtn.setOnClickListener {
        val intent = when (userRole) {
            "Operator" -> Intent(this, OperatorDashboardActivity::class.java)
            "Technical" -> Intent(this, TechnicalDashboardActivity::class.java)
            "Manager" -> Intent(this, ManagerDashboardActivity::class.java)
            else -> null
        }
        intent?.let {
            startActivity(it)
            finish()
        }
    }

    messagesBtn.setOnClickListener {
        val intent = Intent(this, MessagesActivity::class.java)
        startActivity(intent)
        finish()
    }

    profileBtn.setOnClickListener {
        startActivity(Intent(this, ProfileActivity::class.java))
    }
}
