package com.ipvc.fixit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.ipvc.fixit.entities.Equipment
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.entities.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object SupabaseClientInstance {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            Log.d("SupabaseDebug", "URL: ${BuildConfig.SUPABASE_URL}")
            Log.d("SupabaseDebug", "KEY: ${BuildConfig.SUPABASE_KEY.take(10)}...")
            install(Auth)
            install(Postgrest)
            install(Storage)
            httpEngine = CIO.create()
        }
    }

    suspend fun syncFault(fault: Fault, userId: String): Boolean {
        return try {
            val json = buildJsonObject {
                put("userid", JsonPrimitive(userId))
                put("title", JsonPrimitive(fault.description.take(32)))
                put("description", JsonPrimitive(fault.description))
                put("status", JsonPrimitive(fault.status))
                put("createdat", JsonPrimitive(fault.reportedAt))
                put("equipmentid", JsonPrimitive(fault.equipmentId))
                put("assignedto", fault.assignedTo?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                put("photo", fault.photo?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as String?))
                put("urgency", JsonPrimitive(fault.urgency))
                put("location", JsonPrimitive(fault.location))
                put("resolvedat", fault.resolvedAt?.let { JsonPrimitive(it) } ?: JsonPrimitive(null as Long?))
                put("syncstatus", JsonPrimitive(true))
            }

            client.postgrest
                .from("Fault")
                .update(json) {
                    filter {
                        eq("faultid", fault.faultId)
                    }
                }

            Log.d("SupabaseSync", "Avaria sincronizada com sucesso.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Erro ao sincronizar avaria: ${e.message}", e)
            false
        }
    }

    suspend fun syncEquipment(equipment: Equipment): Boolean {
        return try {
            val json = buildJsonObject {
                put("name", JsonPrimitive(equipment.name))
                put("model", JsonPrimitive(equipment.model))
                put("location", JsonPrimitive(equipment.location))
                put("installedat", JsonPrimitive(equipment.installedAt))
            }

            client.postgrest.from("Equipment").insert(json)
            Log.d("SupabaseSync", "Equipamento sincronizado com sucesso.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Erro ao sincronizar equipamento: ${e.message}", e)
            false
        }
    }


    fun isConnectedToInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun updateUserInSupabase(user: User): Boolean {
        return try {
            val json = buildJsonObject {
                put("name", JsonPrimitive(user.name))
                put("email", JsonPrimitive(user.email))
                put("password", JsonPrimitive(user.password))
                put("phone", JsonPrimitive(user.phone ?: ""))
                put("profilephoto", JsonPrimitive(user.profilePhoto ?: ""))
            }

            client.postgrest
                .from("Users")
                .update(json) {
                    filter { eq("id", user.userId) }
                }

            true
        } catch (e: Exception) {
            Log.e("Supabase", "Erro ao atualizar utilizador no Supabase: ${e.message}", e)
            false
        }
    }
}
