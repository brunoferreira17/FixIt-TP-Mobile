package com.ipvc.fixit.dao

import androidx.room.*
import com.ipvc.fixit.entities.Fault

@Dao
interface FaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFault(fault: Fault)

    @Update
    suspend fun updateFault(fault: Fault)

    @Delete
    suspend fun deleteFault(fault: Fault)

    @Query("SELECT * FROM faults ORDER BY reportedAt DESC")
    suspend fun getAllFaults(): List<Fault>

    @Query("SELECT * FROM faults WHERE status = :status ORDER BY reportedAt DESC")
    suspend fun getFaultsByStatus(status: String): List<Fault>

    @Query("SELECT * FROM faults WHERE assignedTo = :userId ORDER BY reportedAt DESC")
    suspend fun getFaultsByTechnician(userId: Int): List<Fault>
    @Query("SELECT * FROM faults WHERE reportedBy = :userId ORDER BY reportedAt DESC")
    suspend fun getFaultsByReporter(userId: Int): List<Fault>

    @Query("SELECT * FROM faults WHERE syncStatus = 0")
    suspend fun getUnsyncedFaults(): List<Fault>
}
