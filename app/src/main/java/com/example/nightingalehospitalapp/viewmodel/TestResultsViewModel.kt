package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.diagnostic.TestResult
import com.example.nightingalehospitalapp.repository.diagnostic.DiagnosticRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TestResultsViewModel(
    private val repository: DiagnosticRepository = DiagnosticRepository()
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Empty : UiState()
        data class Loaded(val results: List<TestResult>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun observe(patientId: String) {
        // Cancel any previous subscription before starting a new one.
        observeJob?.cancel()
        if (patientId.isBlank()) {
            _uiState.value = UiState.Empty
            return
        }
        _uiState.value = UiState.Loading
        observeJob = viewModelScope.launch {
            repository.observeTestResultsForPatient(patientId)
                .catch { e ->
                    _uiState.value = UiState.Error(e.message ?: "Failed to load test results")
                }
                .collect { results ->
                    _uiState.value = if (results.isEmpty()) UiState.Empty
                    else UiState.Loaded(results)
                }
        }
    }
}