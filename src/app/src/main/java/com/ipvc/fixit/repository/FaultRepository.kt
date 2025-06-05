package com.ipvc.fixit.repository

import com.ipvc.fixit.dao.FaultDao
import com.ipvc.fixit.entities.Fault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FaultRepository(private val dao: FaultDao) {

    suspend fun insert(fault: Fault) = withContext(Dispatchers.IO) {
        dao.insertFault(fault)
    }

    suspend fun update(fault: Fault) = withContext(Dispatchers.IO) {
        dao.updateFault(fault)
    }

    suspend fun delete(fault: Fault) = withContext(Dispatchers.IO) {
        dao.deleteFault(fault)
    }

    suspend fun getAll(): List<Fault> = withContext(Dispatchers.IO) {
        dao.getAllFaults()
    }

    suspend fun getByStatus(status: String): List<Fault> = withContext(Dispatchers.IO) {
        dao.getFaultsByStatus(status)
    }

    suspend fun getByTechnician(userId: Int): List<Fault> = withContext(Dispatchers.IO) {
        dao.getFaultsByTechnician(userId)
    }

    suspend fun getByReporter(userId: Int): List<Fault> = withContext(Dispatchers.IO) {
        dao.getFaultsByReporter(userId)
    }

    suspend fun getUnsynced(): List<Fault> = withContext(Dispatchers.IO) {
        dao.getUnsyncedFaults()
    }

    suspend fun getByReporterUUID(userId: String): List<Fault> = dao.getByReporterUUID(userId)

    suspend fun markAsSynced(faultId: Int) = dao.markAsSynced(faultId)
}
