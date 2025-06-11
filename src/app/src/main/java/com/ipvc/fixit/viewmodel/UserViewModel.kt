package com.ipvc.fixit.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipvc.fixit.SupabaseClientInstance
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.repository.UserRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

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

    suspend fun getUserById(userId: String): User? {
        return repository.getUserById(userId)
    }

    suspend fun getAnyTechnician(): User? {
        return repository.getAnyTechnician()
    }

    suspend fun getAllTechnicians(): List<User> {
        return repository.getAllByRole("Technical")
    }

    suspend fun getAllUsers(): List<User> {
        return repository.getAll()
    }


    fun login(email: String, password: String, context: Context) {
        viewModelScope.launch {
            try {
                val client = SupabaseClientInstance.client

                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Sessão sem utilizador")

                val userData = client.postgrest.from("Users")
                    .select {
                        filter { eq("id", userId) }
                    }.decodeList<User>().firstOrNull() ?: throw Exception("Utilizador não encontrado na base de dados.")

                val prefs = context.getSharedPreferences("FixItPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("LOGGED_USER_ID", userData.userId).apply()

                val existing = repository.getUserById(userData.userId)
                if (existing == null) {
                    repository.insert(userData)
                } else {
                    repository.update(userData)
                }

                _loggedUser.value = userData

            } catch (e: Exception) {
                val errorKey = when {
                    e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                        "error_email_not_confirmed"
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "error_invalid_credentials"
                    else -> "error_generic"
                }

                Log.e("Login", "Erro ao iniciar sessão: $errorKey", e)
                _loginError.value = errorKey
                _loggedUser.value = null
            }
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            repository.updateUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }


}