package com.ipvc.fixit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipvc.fixit.entities.Message
import com.ipvc.fixit.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MessageViewModel(private val repository: MessageRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    fun insert(message: Message) {
        viewModelScope.launch {
            repository.insert(message)
            loadByFault(message.faultId)
        }
    }

    fun delete(message: Message) {
        viewModelScope.launch {
            repository.delete(message)
            loadByFault(message.faultId)
        }
    }

    fun loadByFault(faultId: Int) {
        viewModelScope.launch {
            _messages.value = repository.getByFault(faultId)
        }
    }
}
