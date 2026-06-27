package com.example.nightingalehospitalapp.viewmodel.admin.surgery

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.enums.SurgeryStatus
import com.example.nightingalehospitalapp.models.hospital.OperationTheatre
import com.example.nightingalehospitalapp.models.surgery.SurgeryBooking
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.surgery.SurgeryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScheduleSurgeryViewModel : ViewModel() {
    private val surgeryRepository = SurgeryRepository()

    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors: StateFlow<List<Doctor>> = _doctors

    private val _operationTheatres = MutableStateFlow<List<OperationTheatre>>(emptyList())
    val operationTheatres: StateFlow<List<OperationTheatre>> = _operationTheatres

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadFormData()
    }

    private fun loadFormData() {
        _isLoading.value = true
        
        var loadedCount = 0
        val totalToLoad = 3
        
        fun checkFinished() {
            loadedCount++
            if (loadedCount >= totalToLoad) {
                _isLoading.value = false
            }
        }

        // Fetch Patients (Users with role PATIENT)
        FirebaseConfig.usersRef.whereEqualTo("role", "PATIENT").get()
            .addOnSuccessListener { documents ->
                _patients.value = documents.mapNotNull { it.toObject(User::class.java) }
                checkFinished()
            }
            .addOnFailureListener { checkFinished() }

        // Fetch Doctors
        FirebaseConfig.doctorsRef.get()
            .addOnSuccessListener { documents ->
                _doctors.value = documents.mapNotNull { it.toObject(Doctor::class.java) }
                checkFinished()
            }
            .addOnFailureListener { checkFinished() }

        // Fetch Available Operation Theatres
        surgeryRepository.getAvailableOperationTheatres { ots, _ ->
            _operationTheatres.value = ots ?: emptyList()
            checkFinished()
        }
    }

    fun submitSurgery(
        patientId: String,
        doctorId: String,
        otId: String,
        surgeryType: String,
        date: String,
        startTime: String,
        endTime: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (patientId.isEmpty() || doctorId.isEmpty() || otId.isEmpty() || surgeryType.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            onResult(false, "Please fill all fields")
            return
        }

        val surgery = SurgeryBooking(
            patientId = patientId,
            doctorId = doctorId,
            otId = otId,
            surgeryType = surgeryType,
            date = date,
            startTime = startTime,
            endTime = endTime,
            status = SurgeryStatus.BOOKED // Initial status
        )

        surgeryRepository.bookSurgery(surgery) { success, error ->
            if (success) {
                // Update OT status to OCCUPIED
                surgeryRepository.updateOperationTheatreStatus(otId, "OCCUPIED") { otSuccess, otError ->
                    if (otSuccess) {
                        onResult(true, null)
                    } else {
                        onResult(false, otError ?: "Failed to update operation theatre status")
                    }
                }
            } else {
                onResult(false, error ?: "Failed to schedule surgery")
            }
        }
    }
}
