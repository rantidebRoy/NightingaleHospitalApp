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
import kotlinx.coroutines.tasks.await

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
            repository.seedDemoDataIfEmpty(doctorId)
            repository.observeAppointmentsForDoctor(doctorId)
                .catch { e -> _appointments.value = UiState.Error(e.message ?: "Failed to load") }
                .collectLatest { list ->
                    val activeList = list.filter { it.status != AppointmentStatus.CANCELLED }
                    val enrichedList = activeList.map { appt ->
                        var updated = appt
                        if (appt.patientId.isNotBlank()) {
                            try {
                                val patientDoc = com.example.nightingalehospitalapp.database.FirebaseConfig.patientsRef
                                    .document(appt.patientId).get().await()

                                val userDoc = com.example.nightingalehospitalapp.database.FirebaseConfig.usersRef
                                    .document(appt.patientId).get().await()

                                val patientObj = if (patientDoc.exists()) patientDoc.toObject(com.example.nightingalehospitalapp.models.user.Patient::class.java) else null
                                val realName = userDoc.getString("name")

                                updated = updated.copy(
                                    patientName = if (!realName.isNullOrBlank()) realName else appt.patientName,
                                    patientAge = if ((patientObj?.age ?: 0) > 0) patientObj!!.age else appt.patientAge,
                                    patientGender = if (!patientObj?.gender.isNullOrBlank()) patientObj!!.gender else appt.patientGender
                                )
                            } catch (_: Exception) {
                                // Keep original if fetch fails
                            }
                        }
                        updated
                    }
                    _appointments.value = UiState.Loaded(enrichedList)
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
            repository.observeAppointmentsForPatient(patientId)
                .catch { e -> _appointments.value = UiState.Error(e.message ?: "Failed to load") }
                .collectLatest { list ->
                    val enrichedList = list.map { appt ->
                        var updated = appt
                        if (appt.doctorId.isNotBlank()) {
                            try {
                                val doctorDoc = com.example.nightingalehospitalapp.database.FirebaseConfig.doctorsRef
                                    .document(appt.doctorId).get().await()

                                val userDoc = com.example.nightingalehospitalapp.database.FirebaseConfig.usersRef
                                    .document(appt.doctorId).get().await()

                                val doctorObj = if (doctorDoc.exists()) doctorDoc.toObject(com.example.nightingalehospitalapp.models.user.Doctor::class.java) else null
                                val realDoctorName = userDoc.getString("name") ?: doctorObj?.name ?: ""
                                val docDisplayId = userDoc.getString("displayId") ?: doctorObj?.displayId ?: ""

                                var deptName = ""
                                if (!doctorObj?.departmentId.isNullOrBlank()) {
                                    try {
                                        val deptDoc = com.example.nightingalehospitalapp.database.FirebaseConfig.departmentsRef
                                            .document(doctorObj!!.departmentId).get().await()
                                        deptName = deptDoc.getString("name") ?: ""
                                    } catch (_: Exception) {}
                                }

                                updated = updated.copy(
                                    doctorName = if (realDoctorName.isNotBlank()) realDoctorName else appt.doctorName,
                                    doctorDisplayId = docDisplayId,
                                    doctorSpecialization = doctorObj?.specialization ?: "",
                                    doctorDepartment = deptName
                                )
                            } catch (_: Exception) {
                                // Keep original if fetch fails
                            }
                        }
                        updated
                    }
                    _appointments.value = UiState.Loaded(enrichedList)
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

    fun cancelAppointmentFromSlot(slotId: String, patientId: String, doctorId: String, date: String, time: String) {
        viewModelScope.launch {
            val slotResult = slotRepository.freeSlot(slotId)
            if (slotResult.isSuccess) {
                try {
                    val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("appointments")
                        .whereEqualTo("patientId", patientId)
                        .whereEqualTo("doctorId", doctorId)
                        .whereEqualTo("date", date)
                        .whereEqualTo("time", time)
                        .get()
                        .await()
                    for (doc in snapshot.documents) {
                        repository.updateStatus(doc.id, AppointmentStatus.CANCELLED)
                    }
                    _updateResult.value = "Appointment cancelled"
                } catch (e: Exception) {
                    _updateResult.value = "Slot freed, but failed to update appointment status."
                }
            } else {
                _updateResult.value = "Failed to free slot"
            }
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }
}