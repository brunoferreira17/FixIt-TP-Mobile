package com.ipvc.fixit.dao

import androidx.room.*
import com.ipvc.fixit.entities.Message

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("SELECT * FROM messages WHERE faultId = :faultId ORDER BY sentAt ASC")
    suspend fun getMessagesByFault(faultId: Int): List<Message>
}
