package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    @SerialName("messageid")
    @PrimaryKey(autoGenerate = true)
    val messageId: Int = 0,

    @SerialName("faultid")
    val faultId: Int,

    @SerialName("senderid")
    val senderId: String,

    val message: String,
    @SerialName("sentat")
    val sentAt: Long

)
