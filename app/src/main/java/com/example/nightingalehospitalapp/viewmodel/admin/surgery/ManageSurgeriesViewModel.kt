package com.example.nightingalehospitalapp.viewmodel.admin.surgery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.enums.SurgeryStatus
import com.example.nightingalehospitalapp.models.surgery.SurgeryBooking
import com.example.nightingalehospitalapp.repository.surgery.SurgeryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

/**
 * Surfaces every surgery in the system (admin view) or just the surgeries
 * assigned to a specific doctor (read-only doctor view).
 *
 * Pass [doctorId] in the constructor / via [bindDoctor] to restrict the list.
 * While a [doctorId] is bound, status-change actions are disabled.
 */
class ManageSurgeriesViewModel(
    initialDoctorId: String? = null
) : ViewModel() {

    private val surgeryRepository = SurgeryRepository()

    private val _doctorId = MutableStateFlow(initialDoctorId?.takeIf { it.isNotBlank() })

    /** True when the VM is being driven by a doctor (read-only view). */
    val isDoctorScope: StateFlow<Boolean> = _doctorId
        .let { MutableStateFlow(it.value != null) }

    private val _surgeries = MutableStateFlow<List<SurgeryBookingItem>>(emptyList())
    val surgeries: StateFlow<List<SurgeryBookingItem>> = _surgeries

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchSurgeries()
    }

    /**
     * Bind the VM to a single doctor's view. Subsequent fetches will only
     * return surgeries where `doctorId == [doctorId]`. Status-change actions
     * become no-ops.
     */
    fun bindDoctor(doctorId: String) {
        if (doctorId.isBlank()) return
        _doctorId.value = doctorId
        fetchSurgeries()
    }

    /**
     * Loads surgeries from `surgery_bookings`. When a doctor scope is bound,
     * only that doctor's bookings are returned. Patient / doctor / OT display
     * strings are resolved in parallel — individual sub-lookup failures fall
     * back to a labelled "Unknown" placeholder rather than hanging the screen.
     */
    fun fetchSurgeries() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val raw: List<SurgeryBooking> = _doctorId.value
                    ?.let { surgeryRepository.getAllSurgeriesForDoctorAsync(it) }
                    ?: surgeryRepository.getAllSurgeriesAsync()

                if (raw.isEmpty()) {
                    _surgeries.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val items = coroutineScope {
                    raw.map { surgery ->
                        async {
                            val patientName = lookupUserName(
                                surgery.patientId,
                                fallback = "Unknown Patient"
                            )
                            val doctorName = lookupUserName(
                                surgery.doctorId,
                                fallback = "Unknown Doctor"
                            )
                            val otRoom = lookupOtRoom(
                                surgery.otId,
                                fallback = "Unassigned OT"
                            )
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
                        }
                    }.awaitAll()
                }

                _surgeries.value = items
                _isLoading.value = false
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Failed to load surgeries"
                _isLoading.value = false
            }
        }
    }

    private suspend fun lookupUserName(userId: String, fallback: String): String {
        if (userId.isBlank()) return fallback
        return try {
            val snap = FirebaseConfig.usersRef.document(userId).get().await()
            snap.getString("name")?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Throwable) {
            fallback
        }
    }

    private suspend fun lookupOtRoom(otId: String, fallback: String): String {
        if (otId.isBlank()) return fallback
        return try {
            val snap = FirebaseConfig.operationTheatresRef.document(otId).get().await()
            snap.getString("roomNumber")?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Throwable) {
            fallback
        }
    }

    /**
     * Status updates are admin-only. While a doctor scope is bound this is a
     * no-op that reports a friendly error so the UI can ignore it.
     */
    fun updateSurgeryStatus(
        surgeryId: String,
        newStatus: SurgeryStatus,
        otId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (_doctorId.value != null) {
            onResult(false, "Only admins can update surgery status")
            return
        }

        surgeryRepository.updateSurgeryStatus(surgeryId, newStatus) { success, error ->
            if (!success) {
                onResult(false, error ?: "Failed to update surgery status")
                return@updateSurgeryStatus
            }

            // Release the OT back to AVAILABLE when the surgery is finished or cancelled.
            val shouldFreeOt = newStatus == SurgeryStatus.COMPLETED ||
                newStatus == SurgeryStatus.CANCELLED
            if (shouldFreeOt && otId.isNotBlank()) {
                surgeryRepository.updateOperationTheatreStatus(otId, "AVAILABLE") { otOk, otErr ->
                    fetchSurgeries()
                    if (otOk) {
                        onResult(true, null)
                    } else {
                        onResult(false, otErr ?: "Surgery updated, but failed to free OT")
                    }
                }
            } else {
                fetchSurgeries()
                onResult(true, null)
            }
        }
    }
}
