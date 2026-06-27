package com.example.nightingalehospitalapp.viewmodel.admin.surgery

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.enums.SurgeryStatus
import com.example.nightingalehospitalapp.repository.surgery.SurgeryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SurgeryBookingItem(
    val surgeryId: String,
    val patientName: String,
    val doctorName: String,
    val otRoom: String,
    val surgeryType: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: SurgeryStatus,
    val otId: String
)

class ManageSurgeriesViewModel : ViewModel() {
    private val surgeryRepository = SurgeryRepository()

    private val _surgeries = MutableStateFlow<List<SurgeryBookingItem>>(emptyList())
    val surgeries: StateFlow<List<SurgeryBookingItem>> = _surgeries

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchSurgeries()
    }

    fun fetchSurgeries() {
        _isLoading.value = true
        surgeryRepository.getAllSurgeries { surgeries, error ->
            if (error != null || surgeries == null) {
                _errorMessage.value = error ?: "Failed to load surgeries"
                _isLoading.value = false
                return@getAllSurgeries
            }

            if (surgeries.isEmpty()) {
                _surgeries.value = emptyList()
                _isLoading.value = false
                return@getAllSurgeries
            }

            val resultList = mutableListOf<SurgeryBookingItem>()
            var fetchedCount = 0

            surgeries.forEach { surgery ->
                // Fetch Patient Name
                FirebaseConfig.usersRef.document(surgery.patientId).get()
                    .addOnSuccessListener { patientDoc ->
                        val patientName = patientDoc.getString("name") ?: "Unknown Patient"
                        
                        // Fetch Doctor Name
                        FirebaseConfig.usersRef.document(surgery.doctorId).get()
                            .addOnSuccessListener { doctorDoc ->
                                val doctorName = doctorDoc.getString("name") ?: "Unknown Doctor"
                                
                                // Fetch OT Room
                                FirebaseConfig.operationTheatresRef.document(surgery.otId).get()
                                    .addOnSuccessListener { otDoc ->
                                        val otRoom = otDoc.getString("roomNumber") ?: "Unknown OT"
                                        
                                        resultList.add(
                                            SurgeryBookingItem(
                                                surgeryId = surgery.surgeryId,
                                                patientName = patientName,
                                                doctorName = doctorName,
                                                otRoom = otRoom,
                                                surgeryType = surgery.surgeryType,
                                                date = surgery.date,
                                                startTime = surgery.startTime,
                                                endTime = surgery.endTime,
                                                status = surgery.status,
                                                otId = surgery.otId
                                            )
                                        )
                                        
                                        fetchedCount++
                                        if (fetchedCount == surgeries.size) {
                                            // Sort by date (descending string comparison works if format is yyyy-MM-dd, but let's just reverse or keep as is)
                                            _surgeries.value = resultList
                                            _isLoading.value = false
                                        }
                                    }
                            }
                    }
            }
        }
    }

    fun updateSurgeryStatus(surgeryId: String, newStatus: SurgeryStatus, otId: String, onResult: (Boolean, String?) -> Unit) {
        surgeryRepository.updateSurgeryStatus(surgeryId, newStatus) { success, error ->
            if (success) {
                // If status is COMPLETED or CANCELLED, free up the OT
                if (newStatus == SurgeryStatus.COMPLETED || newStatus == SurgeryStatus.CANCELLED) {
                    surgeryRepository.updateOperationTheatreStatus(otId, "AVAILABLE") { otSuccess, otError ->
                        if (otSuccess) {
                            fetchSurgeries()
                            onResult(true, null)
                        } else {
                            onResult(false, otError ?: "Failed to free operation theatre")
                        }
                    }
                } else {
                    fetchSurgeries()
                    onResult(true, null)
                }
            } else {
                onResult(false, error ?: "Failed to update status")
            }
        }
    }
}
