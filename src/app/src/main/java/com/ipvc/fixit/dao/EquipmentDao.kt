package com.ipvc.fixit.dao

import androidx.room.*
import com.ipvc.fixit.entities.Equipment

interface EquipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment)

    @Update
    suspend fun updateEquipment(equipment: Equipment)

    @Delete
    suspend fun deleteEquipment(equipment: Equipment)

    @Query("SELECT * FROM equipment ORDER BY equipmentId ASC")
    suspend fun getAllEquipments(): List<Equipment>

    @Query("SELECT * FROM equipment WHERE type = :type")
    suspend fun getEquipmentsByType(type: String): List<Equipment>
}