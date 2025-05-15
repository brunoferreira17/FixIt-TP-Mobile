package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val name: String,
    val email: String,
    val password: String,
    val role: String, // "operador", "tecnico", "gestor"
    val profilePhoto: String? = null,
    val phone: String? = null
)
