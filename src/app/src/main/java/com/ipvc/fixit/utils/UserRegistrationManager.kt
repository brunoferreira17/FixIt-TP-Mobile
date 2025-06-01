package com.ipvc.fixit.utils

import android.util.Log
import com.ipvc.fixit.SupabaseClientInstance
import com.ipvc.fixit.entities.User
import com.ipvc.fixit.repository.UserRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object UserRegistrationManager {

    private const val TAG = "UserRegistration"

    fun registerUser(
        scope: CoroutineScope,
        name: String,
        userEmail: String,
        userPassword: String,
        phone: String,
        role: String = "Operator",
        userRepository: UserRepository,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val client = SupabaseClientInstance.client

        val cleanName = name.trim()
        val cleanEmail = userEmail.trim()
        val cleanPassword = userPassword.trim()
        val cleanPhone = phone.trim()

        Log.d(TAG, "Início do registo -> nome: $cleanName, email: $cleanEmail, telefone: $cleanPhone, role: $role")

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            val exception = IllegalArgumentException("Formato de email inválido: '$cleanEmail'")
            Log.e(TAG, "Erro: ${exception.message}")
            onError(exception)
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "A tentar criar utilizador no Supabase Auth...")
                val user = client.auth.signUpWith(Email) {
                    email = cleanEmail
                    password = cleanPassword
                    data = buildJsonObject {
                        put("name", cleanName)
                        put("phone", cleanPhone)
                        put("role", role)
                    }
                }

                val userId = user?.id ?: throw Exception("Utilizador não criado no Auth.")
                Log.d(TAG, "Utilizador criado com sucesso no Auth! userId: $userId")

                Log.d(TAG, "A inserir utilizador na tabela Users do Supabase...")
                client.postgrest.from("Users").insert(
                    mapOf(
                        "id" to userId,
                        "name" to cleanName,
                        "email" to cleanEmail,
                        "password" to cleanPassword,
                        "phone" to cleanPhone,
                        "role" to role
                    )
                )

                Log.d(TAG, "Inserção na tabela Users concluída!")

                val localUser = User(
                    userId = userId,
                    name = cleanName,
                    email = cleanEmail,
                    password = cleanPassword,
                    phone = cleanPhone,
                    role = role
                )

                Log.d(TAG, "A guardar utilizador localmente com Room...")
                userRepository.insert(localUser)
                Log.d(TAG, "Utilizador guardado localmente.")

                onSuccess()
            } catch (e: Throwable) {
                Log.e(TAG, "Erro no registo do utilizador: ${e.message}", e)
                onError(e)
            }
        }
    }
}
