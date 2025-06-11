package com.ipvc.fixit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ipvc.fixit.R
import com.ipvc.fixit.entities.Message
import com.ipvc.fixit.entities.User
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var userMap: Map<String, User> = emptyMap()

    fun setUserMap(map: Map<String, User>) {
        userMap = map
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: TextView = itemView.findViewById(R.id.messageSender)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val time: TextView = itemView.findViewById(R.id.messageTime)
        val bubble: LinearLayout = itemView.findViewById(R.id.messageBubble)

        fun bind(message: Message) {
            val isOwnMessage = message.senderId == currentUserId
            val senderName = userMap[message.senderId]?.name ?: message.senderId

            sender.text = senderName
            messageText.text = message.message
            time.text = formatDate(message.sentAt)

            // Ajustar constraints dinamicamente
            val senderParams = sender.layoutParams as ConstraintLayout.LayoutParams
            val bubbleParams = bubble.layoutParams as ConstraintLayout.LayoutParams
            val timeParams = time.layoutParams as ConstraintLayout.LayoutParams

            if (isOwnMessage) {
                // Alinhar à direita
                senderParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                senderParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

                bubbleParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                bubbleParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

                timeParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                timeParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                // Alinhar à esquerda
                senderParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                senderParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID

                bubbleParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                bubbleParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID

                timeParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
                timeParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            }

            sender.layoutParams = senderParams
            bubble.layoutParams = bubbleParams
            time.layoutParams = timeParams
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
