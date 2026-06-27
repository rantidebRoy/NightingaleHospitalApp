package com.example.nightingalehospitalapp.viewmodel.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.repository.report.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SystemReportsViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _activities = MutableStateFlow<List<String>>(emptyList())
    val activities: StateFlow<List<String>> = _activities

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            _activities.value = reportRepository.getRecentActivities()
        }
    }
    
    fun generateFullReport() {
        // logic to generate PDF/CSV
    }
}
