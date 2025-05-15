package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val messageId: Int = 0,
    val faultId: Int,
    val senderId: Int,
    val message: String,
    val sentAt: Long
)
