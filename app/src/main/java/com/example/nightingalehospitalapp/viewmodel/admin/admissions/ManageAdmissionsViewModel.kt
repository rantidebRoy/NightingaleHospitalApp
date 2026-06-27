package com.example.nightingalehospitalapp.viewmodel.admin.admissions

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.admission.Admission
import com.example.nightingalehospitalapp.models.hospital.Bed
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.admission.AdmissionRepository
import com.example.nightingalehospitalapp.repository.bed.BedRepository
import com.example.nightingalehospitalapp.models.enums.BedStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AdmittedPatientItem(
    val admissionId: String,
    val patientName: String,
    val doctorName: String,
    val bedRoom: String,
    val admissionDate: Long,
    val bedId: String
)

class ManageAdmissionsViewModel : ViewModel() {
    private val admissionRepository = AdmissionRepository()
    private val bedRepository = BedRepository()

    private val _admittedPatients = MutableStateFlow<List<AdmittedPatientItem>>(emptyList())
    val admittedPatients: StateFlow<List<AdmittedPatientItem>> = _admittedPatients

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchAdmissions()
    }

    fun fetchAdmissions() {
        _isLoading.value = true
        admissionRepository.getAdmittedPatients { admissions, error ->
            if (error != null || admissions == null) {
                _errorMessage.value = error ?: "Failed to load admissions"
                _isLoading.value = false
                return@getAdmittedPatients
            }

            if (admissions.isEmpty()) {
                _admittedPatients.value = emptyList()
                _isLoading.value = false
                return@getAdmittedPatients
            }

            val resultList = mutableListOf<AdmittedPatientItem>()
            var fetchedCount = 0

            admissions.forEach { admission ->
                // Fetch Patient Name
                FirebaseConfig.usersRef.document(admission.patientId).get()
                    .addOnSuccessListener { patientDoc ->
                        val patientName = patientDoc.getString("name") ?: "Unknown Patient"
                        
                        // Fetch Doctor Name
                        FirebaseConfig.usersRef.document(admission.doctorId).get()
                            .addOnSuccessListener { doctorDoc ->
                                val doctorName = doctorDoc.getString("name") ?: "Unknown Doctor"
                                
                                // Fetch Bed Room
                                FirebaseConfig.bedsRef.document(admission.bedId).get()
                                    .addOnSuccessListener { bedDoc ->
                                        val bedRoom = bedDoc.getString("roomNumber") ?: "Unknown Bed"
                                        
                                        resultList.add(
                                            AdmittedPatientItem(
                                                admissionId = admission.id,
                                                patientName = patientName,
                                                doctorName = doctorName,
                                                bedRoom = bedRoom,
                                                admissionDate = admission.admissionDate,
                                                bedId = admission.bedId
                                            )
                                        )
                                        
                                        fetchedCount++
                                        if (fetchedCount == admissions.size) {
                                            _admittedPatients.value = resultList
                                            _isLoading.value = false
                                        }
                                    }
                            }
                    }
            }
        }
    }

    fun dischargePatient(admissionId: String, bedId: String, onResult: (Boolean, String?) -> Unit) {
        admissionRepository.dischargePatient(admissionId) { success, error ->
            if (success) {
                bedRepository.updateBedStatus(bedId, BedStatus.AVAILABLE, null) { bedSuccess, bedError ->
                    if (bedSuccess) {
                        fetchAdmissions() // Refresh list
                        onResult(true, null)
                    } else {
                        onResult(false, bedError ?: "Failed to free bed")
                    }
                }
            } else {
                onResult(false, error ?: "Failed to discharge patient")
            }
        }
    }
}
