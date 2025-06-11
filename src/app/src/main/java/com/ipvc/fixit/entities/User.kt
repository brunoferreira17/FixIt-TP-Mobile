package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @SerialName("id")
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    @SerialName("profilephoto")
    val profilePhoto: String? = null,
    val phone: String? = null
)

