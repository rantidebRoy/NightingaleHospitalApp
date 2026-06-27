package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.test.TestResult
import com.example.nightingalehospitalapp.repository.test.TestResultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestResultViewModel : ViewModel() {

    private val repository = TestResultRepository()

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Loaded(val testResults: List<TestResult>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun observeTestResults(patientId: String) {
        if (patientId.isBlank()) {
            _state.value = UiState.Error("Patient ID is missing")
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.observeTestResultsForPatient(patientId)
                .catch { e -> _state.value = UiState.Error(e.message ?: "Failed to load test results") }
                .collectLatest { list ->
                    _state.value = UiState.Loaded(list)
                }
        }
    }
}
