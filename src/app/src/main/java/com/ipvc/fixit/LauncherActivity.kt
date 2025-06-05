package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.Equipment
import com.ipvc.fixit.repository.EquipmentRepository
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)
        val equipmentDao = db.equipmentDao()
        val repository = EquipmentRepository(equipmentDao)

        lifecycleScope.launch {
            // ⚠️ Limpar todos os equipamentos locais (apenas para testes)
            repository.deleteAll()

            val sample = Equipment(
                name = "Impressora 3D",
                model = "FDM",
                location = "Sala 101",
                installedAt = System.currentTimeMillis()
            )

            // Inserir localmente
            repository.insert(sample)

            // Sincronizar com Supabase
            val sucesso = SupabaseClientInstance.syncEquipment(sample)
            Log.d("Sync", "Resultado da sincronização: $sucesso")

            startActivity(Intent(this@LauncherActivity, LoginActivity::class.java))
            finish()
        }
    }
}
