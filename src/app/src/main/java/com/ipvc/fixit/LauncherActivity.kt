package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.*
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
            if (SupabaseClientInstance.isConnectedToInternet(this@LauncherActivity)) {
                Log.d(TAG, "Ligado à internet. Sincronizando alterações locais...")
                syncAllPendingFaults(this@LauncherActivity, lifecycleScope)

                Log.d(TAG, "Importando dados do Supabase...")
                syncDatabaseFromSupabase()
            } else {
                Log.d(TAG, "Sem ligação à internet. A sincronização não será feita.")
            }

            startActivity(Intent(this@LauncherActivity, LoginActivity::class.java))
            finish()
        }
    }


    private suspend fun syncDatabaseFromSupabase() = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val client = SupabaseClientInstance.client

            // Apagar dados locais
            db.userDao().clearAll()
            db.equipmentDao().clearAll()
            db.faultDao().clearAll()
            db.messageDao().clearAll()
            Log.d(TAG, "Dados locais apagados.")

            // USERS
            val users = client.from("Users").select().decodeList<User>()
            Log.d(TAG, "Utilizadores recebidos: ${users.size}")
            db.userDao().insertAll(users)
            Log.d(TAG, "Utilizadores inseridos localmente.")

            // EQUIPMENT
            val equipment = client.from("Equipment").select().decodeList<Equipment>()
            Log.d(TAG, "Equipamentos recebidos: ${equipment.size}")
            db.equipmentDao().insertAll(equipment)
            Log.d(TAG, "Equipamentos inseridos localmente.")

            // FAULTS
            val faults = client.from("Fault").select().decodeList<Fault>()
            Log.d(TAG, "Avarias recebidas: ${faults.size}")
            db.faultDao().insertAll(faults)
            Log.d(TAG, "Avarias inseridas localmente.")

            // MESSAGES
            val messages = client.from("Message").select().decodeList<Message>()
            Log.d(TAG, "Mensagens recebidas: ${messages.size}")
            db.messageDao().insertAll(messages)
            Log.d(TAG, "Mensagens inseridas localmente.")

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização: ${e.message}", e)
        }
    }
}
