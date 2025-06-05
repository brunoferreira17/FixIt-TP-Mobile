package com.ipvc.fixit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipvc.fixit.entities.Equipment
import com.ipvc.fixit.repository.EquipmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EquipmentViewModel(private val repository: EquipmentRepository) : ViewModel() {

    private val _equipments = MutableStateFlow<List<Equipment>>(emptyList())
    val equipments: StateFlow<List<Equipment>> = _equipments

    fun insert(equipment: Equipment) {
        viewModelScope.launch {
            repository.insert(equipment)
            loadAll()
        }
    }

    fun update(equipment: Equipment) {
        viewModelScope.launch {
            repository.update(equipment)
            loadAll()
        }
    }

    fun delete(equipment: Equipment) {
        viewModelScope.launch {
            repository.delete(equipment)
            loadAll()
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _equipments.value = repository.getAll()
        }
    }

    suspend fun getAllEquipments(): List<Equipment> {
        return repository.getAll()
    }
}
