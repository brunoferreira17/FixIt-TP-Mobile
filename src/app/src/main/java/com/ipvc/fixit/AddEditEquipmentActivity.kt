package com.ipvc.fixit

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.entities.Equipment
import com.ipvc.fixit.repository.EquipmentRepository
import com.ipvc.fixit.viewmodel.EquipmentViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AddEditEquipmentActivity : AppCompatActivity() {

    private lateinit var viewModel: EquipmentViewModel
    private var equipmentId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_equipment)

        val db = AppDatabase.getDatabase(this)
        viewModel = EquipmentViewModel(EquipmentRepository(db.equipmentDao()))

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val modelInput = findViewById<EditText>(R.id.modelInput)
        val locationInput = findViewById<EditText>(R.id.locationInput)
        val saveButton = findViewById<Button>(R.id.btnSaveEquipment)
        val deleteButton = findViewById<Button>(R.id.btnDeleteEquipment)

        equipmentId = intent.getIntExtra("equipment_id", -1).takeIf { it != -1 }

        if (equipmentId != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val equipment = viewModel.getById(equipmentId!!)
                equipment?.let {
                    withContext(Dispatchers.Main) {
                        nameInput.setText(it.name)
                        modelInput.setText(it.model)
                        locationInput.setText(it.location)
                    }
                }
            }
            deleteButton.visibility = Button.VISIBLE
        } else {
            deleteButton.visibility = Button.GONE
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString()
            val model = modelInput.text.toString()
            val location = locationInput.text.toString()

            val equipment = Equipment(
                equipmentId ?: 0,
                name = name,
                model = model,
                location = location,
                installedAt = System.currentTimeMillis()
            )

            lifecycleScope.launch {
                if (equipmentId == null) {
                    val newId = viewModel.insert(equipment)
                    try {
                        val json = buildJsonObject {
                            put("equipmentid", JsonPrimitive(newId))
                            put("name", JsonPrimitive(name))
                            put("model", JsonPrimitive(model))
                            put("location", JsonPrimitive(location))
                            put("installedat", JsonPrimitive(equipment.installedAt ?: 0))
                        }
                        SupabaseClientInstance.client.postgrest
                            .from("Equipment")
                            .insert(json)
                    } catch (e: Exception) {
                        Toast.makeText(this@AddEditEquipmentActivity, "Erro ao sincronizar Supabase", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.update(equipment)
                    try {
                        val json = buildJsonObject {
                            put("name", JsonPrimitive(name))
                            put("model", JsonPrimitive(model))
                            put("location", JsonPrimitive(location))
                            put("installedat", JsonPrimitive(equipment.installedAt ?: 0))
                        }
                        SupabaseClientInstance.client.postgrest
                            .from("Equipment")
                            .update(json) {
                                filter { eq("equipmentid", equipmentId!!) }
                            }
                    } catch (e: Exception) {
                        Toast.makeText(this@AddEditEquipmentActivity, "Erro ao sincronizar Supabase", Toast.LENGTH_SHORT).show()
                    }
                }

                Toast.makeText(this@AddEditEquipmentActivity, getString(R.string.equipment_saved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        deleteButton.setOnClickListener {
            equipmentId?.let { id ->
                lifecycleScope.launch {
                    viewModel.deleteById(id)
                    try {
                        SupabaseClientInstance.client.postgrest
                            .from("Equipment")
                            .delete {
                                filter { eq("equipmentid", id) }
                            }
                    } catch (e: Exception) {
                        Toast.makeText(this@AddEditEquipmentActivity, "Erro ao eliminar no Supabase", Toast.LENGTH_SHORT).show()
                    }
                    Toast.makeText(this@AddEditEquipmentActivity, getString(R.string.equipment_deleted), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
