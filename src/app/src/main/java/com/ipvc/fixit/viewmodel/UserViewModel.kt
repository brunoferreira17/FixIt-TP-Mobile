package com.ipvc.fixit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _loggedUser = MutableStateFlow<User?>(null)
    val loggedUser: StateFlow<User?> = _loggedUser

    fun insert(user: User) {
        viewModelScope.launch {
            repository.insert(user)
            loadUsers()
        }
    }

    fun update(user: User) {
        viewModelScope.launch {
            repository.update(user)
            loadUsers()
        }
    }

    fun delete(user: User) {
        viewModelScope.launch {
            repository.delete(user)
            loadUsers()
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = repository.getAll()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loggedUser.value = repository.login(email, password)
        }
    }
}
