package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ipvc.fixit.adapters.MessagesAdapter
import com.ipvc.fixit.entities.Equipment
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.entities.Message
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MessagesActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var adapter: MessagesAdapter
    private lateinit var userRoleText: TextView
    private lateinit var userId: String
    private lateinit var userRole: String

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
        setContentView(R.layout.activity_messages)

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessagesAdapter(emptyList()) { fault ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("faultId", fault.faultId)
            startActivity(intent)
        }
        messagesRecyclerView.adapter = adapter

        val languageSwitcher = findViewById<TextView>(R.id.languageSwitcher)
        userRoleText = findViewById(R.id.userRole)

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        setupBottomNavBar()

        userId = SessionManager.getLoggedUserId(this) ?: run {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val user = SupabaseClientInstance.client.from("Users").select {
                filter { eq("id", userId) }
            }.decodeSingle<User>()

            userRole = user.role
            val rolePrefix = if (userRole == "Manager") "Manager " else if (userRole == "Technical") getString(R.string.user_role_prefix_technical) else getString(R.string.user_role_prefix)
            userRoleText.text = rolePrefix + user.name

            loadConversations()
        }
    }

    private suspend fun loadConversations() {
        lifecycleScope.launch {
            try {
                val allFaults = SupabaseClientInstance.client.from("Fault").select().decodeList<Fault>()
                val allMessages = SupabaseClientInstance.client.from("Message").select().decodeList<Message>()
                val allEquipments = SupabaseClientInstance.client.from("Equipment").select().decodeList<Equipment>()

                val latestMessages = allMessages
                    .groupBy { it.faultId }
                    .mapValues { it.value.maxByOrNull { msg -> msg.sentAt } }
                    .filterValues { it != null }
                    .mapValues { it.value!! }

                val filteredFaults = if (userRole == "Manager") {
                    allFaults
                } else {
                    allFaults.filter {
                        it.assignedTo == userId || it.reportedBy == userId
                    }
                }

                val faultTriples = filteredFaults.mapNotNull { fault ->
                    val message = latestMessages[fault.faultId] ?: return@mapNotNull null
                    val equipment = allEquipments.find { it.equipmentId == fault.equipmentId } ?: return@mapNotNull null
                    Triple(fault, message, equipment)
                }.sortedByDescending { it.second.sentAt }

                withContext(Dispatchers.Main) {
                    adapter.updateData(faultTriples)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MessagesActivity, getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
