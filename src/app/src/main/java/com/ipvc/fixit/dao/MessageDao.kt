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

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>)

    suspend fun clearAndInsert(messages: List<Message>) {
        clearAll()
        insertAll(messages)
    }
}
