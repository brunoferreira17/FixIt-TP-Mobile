package com.ipvc.fixit.repository

import com.ipvc.fixit.dao.EquipmentDao
import com.ipvc.fixit.entities.Equipment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EquipmentRepository(private val dao: EquipmentDao) {

    suspend fun insert(equipment: Equipment) = withContext(Dispatchers.IO) {
        dao.insertEquipment(equipment)
    }

    suspend fun update(equipment: Equipment) = withContext(Dispatchers.IO) {
        dao.updateEquipment(equipment)
    }

    suspend fun delete(equipment: Equipment) = withContext(Dispatchers.IO) {
        dao.deleteEquipment(equipment)
    }

    suspend fun getAll(): List<Equipment> = withContext(Dispatchers.IO) {
        dao.getAllEquipments()
    }

    suspend fun getByType(type: String): List<Equipment> = withContext(Dispatchers.IO) {
        dao.getEquipmentsByType(type)
    }

    suspend fun deleteAll() {
        dao.clearAll()
    }
}
