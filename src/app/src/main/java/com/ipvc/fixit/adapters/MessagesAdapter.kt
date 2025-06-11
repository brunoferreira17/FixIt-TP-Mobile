package com.ipvc.fixit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ipvc.fixit.R
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.entities.Message
import com.ipvc.fixit.entities.Equipment
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private var conversations: List<Triple<Fault, Message, Equipment>>,
    private val onItemClick: (Fault) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.ConversationViewHolder>() {

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val issueTitle: TextView = itemView.findViewById(R.id.textIssueTitle)
        private val equipmentName: TextView = itemView.findViewById(R.id.textEquipmentName)
        private val lastMessage: TextView = itemView.findViewById(R.id.textLastMessage)
        private val lastMessageTime: TextView = itemView.findViewById(R.id.textLastMessageTime)

        fun bind(fault: Fault, message: Message, equipment: Equipment) {
            val context = itemView.context
            issueTitle.text = context.getString(R.string.issue_prefix) + fault.faultId
            equipmentName.text = context.getString(R.string.equipment_prefix) + " " + equipment.name
            lastMessage.text = context.getString(R.string.last_message_label) + " " + message.message
            lastMessageTime.text = formatTimestamp(message.sentAt)

            itemView.setOnClickListener {
                onItemClick(fault)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_preview, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val (fault, message, equipment) = conversations[position]
        holder.bind(fault, message, equipment)
    }

    override fun getItemCount(): Int = conversations.size

    fun updateData(newList: List<Triple<Fault, Message, Equipment>>) {
        conversations = newList
        notifyDataSetChanged()
    }
}
