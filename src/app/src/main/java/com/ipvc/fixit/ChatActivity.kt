package com.ipvc.fixit

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ipvc.fixit.adapters.ChatAdapter
import com.ipvc.fixit.entities.Message
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.utils.SessionManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.*
import kotlinx.coroutines.launch
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var chatTitle: TextView
    private lateinit var userRoleText: TextView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var languageSwitcher: TextView

    private var faultId: Int = -1
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatTitle = findViewById(R.id.chatTitle)
        userRoleText = findViewById(R.id.userRole)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatRecyclerView = findViewById(R.id.recyclerViewMessages)
        languageSwitcher = findViewById(R.id.languageSwitcher)

        findViewById<ImageView>(R.id.logoIcon).setOnClickListener {
            finish()
        }

        faultId = intent.getIntExtra("faultId", -1)
        if (faultId == -1) {
            Toast.makeText(this, "Invalid fault", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        userId = SessionManager.getLoggedUserId(this) ?: run {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatTitle.text = getString(R.string.chat_title_prefix) + faultId

        lifecycleScope.launch {
            try {
                val user = SupabaseClientInstance.client.from("Users").select {
                    filter { eq("id", userId) }
                }.decodeSingle<User>()

                val rolePrefix = when (user.role) {
                    "Technical" -> getString(R.string.user_role_prefix_technical)
                    "Manager" -> "Manager "
                    else -> getString(R.string.user_role_prefix)
                }

                userRoleText.text = rolePrefix + user.name

            } catch (e: Exception) {
                userRoleText.text = userId
            }
        }

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(emptyList(), userId)
        chatRecyclerView.adapter = adapter

        sendButton.setOnClickListener {
            val content = messageInput.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
            }
        }

        languageSwitcher.setOnClickListener {
            val current = resources.configuration.locales[0].language
            val newLang = if (current == "pt") "en" else "pt"
            setLocale(newLang)
        }

        loadMessages()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val users = SupabaseClientInstance.client.from("Users").select().decodeList<User>()
                val userMap = users.associateBy { it.userId }

                val allMessages = SupabaseClientInstance.client.from("Message")
                    .select {
                        filter { eq("faultid", faultId) }
                        order("sentat", Order.ASCENDING)
                    }
                    .decodeList<Message>()

                adapter.setUserMap(userMap)
                adapter.updateMessages(allMessages)
                chatRecyclerView.scrollToPosition(allMessages.size - 1)

            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Erro ao carregar mensagens: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(content: String) {
        val timestamp = System.currentTimeMillis()

        val message = Message(
            faultId = faultId,
            senderId = userId,
            message = content,
            sentAt = timestamp
        )

        lifecycleScope.launch {
            try {
                SupabaseClientInstance.client.from("Message").insert(message)
                messageInput.text.clear()
                loadMessages()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Erro ao enviar mensagem: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
