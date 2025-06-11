package com.ipvc.fixit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.EquipmentRepository
import com.ipvc.fixit.utils.SessionManager
import com.ipvc.fixit.utils.setupBottomNavBar
import com.ipvc.fixit.viewmodel.EquipmentViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EquipmentListActivity : AppCompatActivity() {

    private lateinit var equipmentViewModel: EquipmentViewModel
    private lateinit var equipmentContainer: LinearLayout
    private lateinit var userRoleText: TextView
    private lateinit var languageSwitcher: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_equipments)

        equipmentContainer = findViewById(R.id.equipmentListContainer)
        userRoleText = findViewById(R.id.userRole)
        languageSwitcher = findViewById(R.id.languageSwitcher)

        languageSwitcher.setOnClickListener {
            val currentLang = resources.configuration.locales[0].language
            val newLang = if (currentLang == "pt") "en" else "pt"
            val locale = Locale(newLang)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
            recreate()
        }

        setupBottomNavBar()

        val db = AppDatabase.getDatabase(this)
        equipmentViewModel = EquipmentViewModel(EquipmentRepository(db.equipmentDao()))

        val userId = SessionManager.getLoggedUserId(this)
        if (userId == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<View>(R.id.fabAddEquipment).setOnClickListener {
            val intent = Intent(this, AddEditEquipmentActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadEquipments()
        }
    }

    private suspend fun loadEquipments() {
        val equipmentList = withContext(Dispatchers.IO) {
            equipmentViewModel.getAllEquipments()
        }

        equipmentContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (equipment in equipmentList) {
            val card = inflater.inflate(R.layout.item_equipment_card, equipmentContainer, false)

            card.findViewById<TextView>(R.id.equipmentName).text = getString(R.string.equipment_label2, equipment.name)
            card.findViewById<TextView>(R.id.equipmentModel).text = getString(R.string.model_label, equipment.model)
            card.findViewById<TextView>(R.id.equipmentLocation).text = getString(R.string.location_label, equipment.location)
            card.findViewById<TextView>(R.id.equipmentDate).text = getString(R.string.installed_label, sdf.format(equipment.installedAt?.let { Date(it) } ?: Date()))

            card.findViewById<Button>(R.id.btnEditEquipment).setOnClickListener {
                val intent = Intent(this, AddEditEquipmentActivity::class.java)
                intent.putExtra("equipment_id", equipment.equipmentId)
                startActivity(intent)
            }

            card.findViewById<Button>(R.id.btnDeleteEquipment).setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        equipmentViewModel.delete(equipment)
                        SupabaseClientInstance.client.postgrest
                            .from("Equipment")
                            .delete {
                                filter { eq("equipmentid", equipment.equipmentId) }
                            }
                    }
                    Toast.makeText(this@EquipmentListActivity, getString(R.string.equipment_deleted), Toast.LENGTH_SHORT).show()
                    loadEquipments()
                }
            }

            equipmentContainer.addView(card)
        }
    }
}
