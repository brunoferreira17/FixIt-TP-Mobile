package com.ipvc.fixit.repository

import com.ipvc.fixit.dao.UserDao
import com.ipvc.fixit.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    suspend fun insert(user: User) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun update(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun delete(user: User) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    suspend fun getAll(): List<User> = withContext(Dispatchers.IO) {
        userDao.getAllUsers()
    }

    suspend fun getByRole(role: String): List<User> = withContext(Dispatchers.IO) {
        userDao.getUsersByRole(role)
    }

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        userDao.login(email, password)
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        userDao.getCurrentUser()
    }

    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

}
