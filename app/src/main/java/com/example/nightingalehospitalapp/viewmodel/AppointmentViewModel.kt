package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.appointment.Appointment
import com.example.nightingalehospitalapp.models.enums.AppointmentStatus
import com.example.nightingalehospitalapp.repository.appointment.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppointmentViewModel : ViewModel() {

    private val repository = AppointmentRepository()
    private val slotRepository = com.example.nightingalehospitalapp.repository.appointment.SlotRepository()

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Loaded(val appointments: List<Appointment>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _appointments = MutableStateFlow<UiState>(UiState.Idle)
    val appointments: StateFlow<UiState> = _appointments.asStateFlow()

    private val _updateResult = MutableStateFlow<String?>(null)
    val updateResult: StateFlow<String?> = _updateResult.asStateFlow()

    /**
     * Seeds demo data once, then observes the doctor's appointments.
     */
    fun observeAppointmentsForDoctor(doctorId: String) {
        if (doctorId.isBlank()) {
            _appointments.value = UiState.Error("Missing doctor id")
            return
        }
        _appointments.value = UiState.Loading
        viewModelScope.launch {
            // Seed demo data only on the first call (idempotent in repo).
            repository.seedDemoDataIfEmpty(doctorId)
            repository.observeAppointmentsForDoctor(doctorId)
                .catch { e -> _appointments.value = UiState.Error(e.message ?: "Failed to load") }
                .collectLatest { list ->
                    _appointments.value = UiState.Loaded(list)
                }
        }
    }

    fun observeAppointmentsForPatient(patientId: String) {
        if (patientId.isBlank()) {
            _appointments.value = UiState.Error("Missing patient id")
            return
        }
        _appointments.value = UiState.Loading
        viewModelScope.launch {
            slotRepository.observeSlotsForPatient(patientId)
                .catch { e -> _appointments.value = UiState.Error(e.message ?: "Failed to load") }
                .collectLatest { slots ->
                    val mapped = slots.map { slot ->
                        Appointment(
                            appointmentId = slot.slotId,
                            doctorId = slot.doctorId,
                            patientId = slot.patientId,
                            patientName = slot.patientName,
                            date = slot.date,
                            time = slot.time,
                            status = AppointmentStatus.CONFIRMED,
                            notes = "Booked slot"
                        )
                    }
                    _appointments.value = UiState.Loaded(mapped)
                }
        }
    }

    fun updateStatus(appointmentId: String, newStatus: AppointmentStatus) {
        viewModelScope.launch {
            val result = repository.updateStatus(appointmentId, newStatus)
            _updateResult.value = result.fold(
                onSuccess = { "Status updated to ${newStatus.name}" },
                onFailure = { it.message ?: "Update failed" }
            )
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    /**
     * Returns demo patient history records when nothing is in Firestore yet,
     * so the UI has something meaningful to show during development.
     */
    fun demoPatientHistory(patientId: String): PatientHistory {
        return PatientHistory.demoFor(patientId)
    }
}

data class PatientHistory(
    val patientId: String,
    val patientName: String,
    val age: Int,
    val gender: String,
    val phone: String,
    val bloodGroup: String,
    val appointments: List<HistoryItem>,
    val prescriptions: List<HistoryItem>,
    val tests: List<HistoryItem>,
    val surgeries: List<HistoryItem>
) {
    companion object {
        fun demoFor(patientId: String): PatientHistory = PatientHistory(
            patientId = patientId,
            patientName = "Demo Patient",
            age = 42,
            gender = "Male",
            phone = "+1 555 010 0123",
            bloodGroup = "O+",
            appointments = listOf(
                HistoryItem("Cardiology consult", "Dr. Anika Roy", "2026-06-10 10:00", "COMPLETED"),
                HistoryItem("Follow-up review", "Dr. Anika Roy", "2026-06-24 11:30", "CONFIRMED"),
                HistoryItem("Routine check-up", "Dr. Anika Roy", "2026-07-02 09:00", "PENDING")
            ),
            prescriptions = listOf(
                HistoryItem("Atorvastatin 20mg", "Dr. Anika Roy", "2026-06-10", "30 days"),
                HistoryItem("Aspirin 75mg", "Dr. Anika Roy", "2026-06-10", "30 days")
            ),
            tests = listOf(
                HistoryItem("ECG", "Lab A", "2026-06-10", "Normal"),
                HistoryItem("Lipid Profile", "Lab A", "2026-06-10", "Slightly elevated LDL")
            ),
            surgeries = listOf(
                HistoryItem("Appendectomy", "Dr. M. Khan", "2018-03-14", "Recovered")
            )
        )
    }
}

data class HistoryItem(
    val title: String,
    val provider: String,
    val date: String,
    val detail: String
)