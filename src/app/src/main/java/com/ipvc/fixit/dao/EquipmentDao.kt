package com.ipvc.fixit.dao

import androidx.room.*
import com.ipvc.fixit.entities.Equipment

@Dao
interface EquipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment)

    @Update
    suspend fun updateEquipment(equipment: Equipment)

    @Delete
    suspend fun deleteEquipment(equipment: Equipment)

    @Query("SELECT * FROM equipment ORDER BY equipmentId ASC")
    suspend fun getAllEquipments(): List<Equipment>

    @Query("SELECT * FROM equipment WHERE model = :model")
    suspend fun getEquipmentsByType(model: String): List<Equipment>

    @Query("DELETE FROM equipment")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(equipmentList: List<Equipment>)

    suspend fun clearAndInsert(equipmentList: List<Equipment>) {
        clearAll()
        insertAll(equipmentList)
    }
}