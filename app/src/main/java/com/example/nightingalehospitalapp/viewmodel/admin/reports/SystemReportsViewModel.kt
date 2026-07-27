package com.example.nightingalehospitalapp.viewmodel.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.repository.report.ReportRepository
import com.example.nightingalehospitalapp.repository.report.SystemReportMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SystemReportsViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _metrics = MutableStateFlow(SystemReportMetrics())
    val metrics: StateFlow<SystemReportMetrics> = _metrics

    private val _activities = MutableStateFlow<List<String>>(emptyList())
    val activities: StateFlow<List<String>> = _activities

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    fun clearMessage() {
        _errorMessage.value = null
        _actionMessage.value = null
    }

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _metrics.value = reportRepository.getSystemMetrics()
                _activities.value = reportRepository.getRecentActivities()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load metrics: ${e.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun generateFullReport() {
        viewModelScope.launch {
            _metrics.value = reportRepository.getSystemMetrics()
            _actionMessage.value = "Report generated & updated successfully!"
        }
    }
}
