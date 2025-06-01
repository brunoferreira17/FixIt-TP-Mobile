package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "faults")
data class Fault(
    @PrimaryKey(autoGenerate = true) val faultId: Int = 0,
    val equipmentId: Int,
    val reportedBy: Int,
    val assignedTo: Int?,
    val description: String,
    val photo: String?,
    val urgency: String, // "baixa", "media", "alta"
    val location: String,
    val status: String, // "pendente", "em_resolucao", "resolvida"
    val reportedAt: Long,
    val resolvedAt: Long?,
    val syncStatus: Boolean
)
