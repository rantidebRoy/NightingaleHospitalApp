package com.example.nightingalehospitalapp.viewmodel.admin.admissions

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.admission.Admission
import com.example.nightingalehospitalapp.models.hospital.Bed
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.admission.AdmissionRepository
import com.example.nightingalehospitalapp.repository.bed.BedRepository
import com.example.nightingalehospitalapp.models.enums.AdmissionStatus
import com.example.nightingalehospitalapp.models.enums.BedStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateAdmissionViewModel : ViewModel() {
    private val admissionRepository = AdmissionRepository()
    private val bedRepository = BedRepository()

    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors: StateFlow<List<Doctor>> = _doctors

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments

    private val _beds = MutableStateFlow<List<Bed>>(emptyList())
    val beds: StateFlow<List<Bed>> = _beds

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
        val totalToLoad = 4
        
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

        // Fetch Departments
        FirebaseConfig.departmentsRef.get()
            .addOnSuccessListener { documents ->
                _departments.value = documents.mapNotNull { it.toObject(Department::class.java) }
                checkFinished()
            }
            .addOnFailureListener { checkFinished() }

        // Fetch Available Beds
        bedRepository.getBedsByStatus(BedStatus.AVAILABLE) { beds, _ ->
            _beds.value = beds ?: emptyList()
            checkFinished()
        }
    }

    fun submitAdmission(
        patientId: String,
        doctorId: String,
        departmentId: String,
        bedId: String,
        reason: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (patientId.isEmpty() || doctorId.isEmpty() || departmentId.isEmpty() || bedId.isEmpty() || reason.isEmpty()) {
            onResult(false, "Please fill all fields")
            return
        }

        val admission = Admission(
            patientId = patientId,
            doctorId = doctorId,
            departmentId = departmentId,
            bedId = bedId,
            reason = reason,
            admissionDate = System.currentTimeMillis(),
            status = AdmissionStatus.ADMITTED
        )

        admissionRepository.admitPatient(admission) { success, error ->
            if (success) {
                // Update bed status to OCCUPIED
                bedRepository.updateBedStatus(bedId, BedStatus.OCCUPIED, patientId) { bedSuccess, bedError ->
                    if (bedSuccess) {
                        onResult(true, null)
                    } else {
                        // Rollback admission would be ideal here, but for simplicity we return error
                        onResult(false, bedError ?: "Failed to update bed status")
                    }
                }
            } else {
                onResult(false, error ?: "Failed to create admission")
            }
        }
    }
}
