package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val equipmentId: Int = 0,
    val name: String,
    val type: String,
    val location: String
)
