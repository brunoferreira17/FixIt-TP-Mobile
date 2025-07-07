package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.*
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.syncAllPendingFaults
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LauncherActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val context = this@LauncherActivity
            val db = AppDatabase.getDatabase(context)

            val savedUserId = SessionManager.getLoggedUserId(context)

            if (savedUserId != null) {
                val localUser = db.userDao().getUserById(savedUserId)
                if (localUser != null) {
                    Log.d(TAG, "Login automático como ${localUser.email} (${localUser.role})")
                    SessionManager.saveUserRole(context, localUser.role)

                    if (SupabaseClientInstance.isConnectedToInternet(context)) {
                        Log.d(TAG, "Ligado à internet. Sincronizando alterações locais...")
                        syncAllPendingFaults(context, lifecycleScope)

                        Log.d(TAG, "Importando dados do Supabase...")
                        syncDatabaseFromSupabase()
                    } else {
                        Log.d(TAG, "Sem ligação à internet. A sincronização não será feita.")
                    }

                    withContext(Dispatchers.Main) {
                        redirectToDashboard(localUser.role)
                    }
                    return@launch
                } else {
                    Log.w(TAG, "Utilizador guardado não encontrado localmente.")
                }
            }

            // Caso não haja sessão guardada ou user local, segue fluxo normal
            if (SupabaseClientInstance.isConnectedToInternet(context)) {
                Log.d(TAG, "Ligado à internet. Sincronizando alterações locais...")
                syncAllPendingFaults(context, lifecycleScope)

                Log.d(TAG, "Importando dados do Supabase...")
                syncDatabaseFromSupabase()
            } else {
                Log.d(TAG, "Sem ligação à internet. A sincronização não será feita.")
            }

            withContext(Dispatchers.Main) {
                startActivity(Intent(context, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun redirectToDashboard(role: String) {
        val intent = when (role.lowercase()) {
            "operator" -> Intent(this, OperatorDashboardActivity::class.java)
            "technical" -> Intent(this, TechnicalDashboardActivity::class.java)
            "manager" -> Intent(this, ManagerDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private suspend fun syncDatabaseFromSupabase() = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val client = SupabaseClientInstance.client

            db.userDao().clearAll()
            db.equipmentDao().clearAll()
            db.faultDao().clearAll()
            db.messageDao().clearAll()
            Log.d(TAG, "Dados locais apagados.")

            val users = client.from("Users").select().decodeList<User>()
            db.userDao().insertAll(users)
            Log.d(TAG, "Utilizadores sincronizados.")

            val equipment = client.from("Equipment").select().decodeList<Equipment>()
            db.equipmentDao().insertAll(equipment)
            Log.d(TAG, "Equipamentos sincronizados.")

            val faults = client.from("Fault").select().decodeList<Fault>()
            db.faultDao().insertAll(faults)
            Log.d(TAG, "Avarias sincronizadas.")

            val messages = client.from("Message").select().decodeList<Message>()
            db.messageDao().insertAll(messages)
            Log.d(TAG, "Mensagens sincronizadas.")

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização: ${e.message}", e)
        }
    }
}
