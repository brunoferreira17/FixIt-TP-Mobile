package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "faults")
data class Fault(
    @PrimaryKey(autoGenerate = true) val faultId: Int = 0,

    @SerialName("equipmentid")
    val equipmentId: Int,

    @SerialName("userid")
    val reportedBy: String,

    @SerialName("assignedto")
    val assignedTo: String?,

    val description: String,
    val photo: String?,

    val urgency: String,
    val location: String,
    val status: String,

    @SerialName("createdat")
    val reportedAt: Long,

    @SerialName("resolvedat")
    val resolvedAt: Long?,

    @SerialName("syncstatus")
    val syncStatus: Boolean
)


