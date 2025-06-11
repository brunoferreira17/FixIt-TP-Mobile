package com.ipvc.fixit.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "equipment")
data class Equipment(
    @SerialName("equipmentid")
    @PrimaryKey(autoGenerate = true)
    val equipmentId: Int = 0,

    @SerialName("name")
    val name: String,

    @SerialName("model")
    val model: String,

    @SerialName("location")
    val location: String,

    @SerialName("installedat")
    val installedAt: Long? = null
)
