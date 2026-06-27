package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.prescription.Prescription
import com.example.nightingalehospitalapp.repository.prescription.PrescriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PrescriptionViewModel : ViewModel() {

    private val repository = PrescriptionRepository()

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Loaded(val prescriptions: List<Prescription>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Cache of doctorId -> doctor display name
    private val _doctorNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val doctorNames: StateFlow<Map<String, String>> = _doctorNames.asStateFlow()

    /**
     * Starts observing prescriptions for the given patient.
     * Matches the patient's Firebase Auth UID with the patientId field
     * stored in each prescription document.
     */
    fun observePrescriptions(patientId: String) {
        if (patientId.isBlank()) {
            _state.value = UiState.Error("Patient ID is missing")
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.observePrescriptionsForPatient(patientId)
                .catch { e -> _state.value = UiState.Error(e.message ?: "Failed to load prescriptions") }
                .collectLatest { prescriptions ->
                    _state.value = UiState.Loaded(prescriptions)
                    // Resolve doctor names for every unique doctorId
                    val uniqueIds = prescriptions.map { it.doctorId }.distinct()
                    val currentMap = _doctorNames.value.toMutableMap()
                    for (id in uniqueIds) {
                        if (id.isNotBlank() && id !in currentMap) {
                            currentMap[id] = repository.getDoctorName(id)
                        }
                    }
                    _doctorNames.value = currentMap
                }
        }
    }
}
