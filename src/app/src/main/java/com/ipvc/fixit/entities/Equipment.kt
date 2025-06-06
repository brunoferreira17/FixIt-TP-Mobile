package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val equipmentId: Int = 0,
    val name: String,
    val model: String,
    val location: String,
    val installedAt: Long? = null
)
