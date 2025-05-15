package com.ipvc.fixit.repository

import com.ipvc.fixit.dao.MessageDao
import com.ipvc.fixit.entities.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessageRepository(private val dao: MessageDao) {

    suspend fun insert(message: Message) = withContext(Dispatchers.IO) {
        dao.insertMessage(message)
    }

    suspend fun delete(message: Message) = withContext(Dispatchers.IO) {
        dao.deleteMessage(message)
    }

    suspend fun getByFault(faultId: Int): List<Message> = withContext(Dispatchers.IO) {
        dao.getMessagesByFault(faultId)
    }
}
