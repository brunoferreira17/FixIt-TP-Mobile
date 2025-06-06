package com.ipvc.fixit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipvc.fixit.entities.Fault
import com.ipvc.fixit.repository.FaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FaultViewModel(private val repository: FaultRepository) : ViewModel() {

    private val _faults = MutableStateFlow<List<Fault>>(emptyList())
    val faults: StateFlow<List<Fault>> = _faults

    fun insert(fault: Fault) {
        viewModelScope.launch {
            repository.insert(fault)
            loadAll()
        }
    }

    fun update(fault: Fault) {
        viewModelScope.launch {
            repository.update(fault)
            loadAll()
        }
    }

    fun delete(fault: Fault) {
        viewModelScope.launch {
            repository.delete(fault)
            loadAll()
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _faults.value = repository.getAll()
        }
    }

    fun loadByStatus(status: String) {
        viewModelScope.launch {
            _faults.value = repository.getByStatus(status)
        }
    }

    fun loadByTechnician(userId: Int) {
        viewModelScope.launch {
            _faults.value = repository.getByTechnician(userId)
        }
    }

    fun loadByReporter(userId: Int) {
        viewModelScope.launch {
            _faults.value = repository.getByReporter(userId)
        }
    }

    fun getFaultsByUserId(userId: String): StateFlow<List<Fault>> {
        viewModelScope.launch {
            _faults.value = repository.getByReporterUUID(userId)
        }
        return faults
    }

    fun markAsSynced(faultId: Int) {
        viewModelScope.launch {
            repository.markAsSynced(faultId)
            loadAll()
        }
    }

    suspend fun loadAllAssignedTo(userId: String) {
        _faults.value = repository.getByAssignedUUID(userId)
    }

    suspend fun getUnsynced(): List<Fault> {
        return repository.getUnsynced()
    }
}
