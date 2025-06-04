package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "faults")
data class Fault(
    @PrimaryKey(autoGenerate = true) val faultId: Int = 0,
    val equipmentId: Int,
    val reportedBy: String,
    val assignedTo: String?,
    val description: String,
    val photo: String?,
    val urgency: String,
    val location: String,
    val status: String,
    val reportedAt: Long,
    val resolvedAt: Long?,
    val syncStatus: Boolean
)

