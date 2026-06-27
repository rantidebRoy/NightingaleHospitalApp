package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.prescription.Medicine
import com.example.nightingalehospitalapp.repository.prescription.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MedicineViewModel : ViewModel() {

    private val repository = MedicineRepository()

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Loaded(val medicines: List<Medicine>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        observeAllMedicines()
    }

    fun observeAllMedicines() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.observeAllMedicines()
                .catch { e -> _state.value = UiState.Error(e.message ?: "Failed to load medicines") }
                .collectLatest { list ->
                    _state.value = UiState.Loaded(list)
                }
        }
    }
}
