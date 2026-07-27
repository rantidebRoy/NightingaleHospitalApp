package com.example.nightingalehospitalapp.viewmodel.admin.doctors

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.user.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
data class PendingDoctorWithDetails(
    val user: User,
    val doctor: Doctor? = null
)

class ManageDoctorsViewModel(
    private val doctorRepository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _approvedDoctors = MutableStateFlow<List<Doctor>>(emptyList())
    val approvedDoctors: StateFlow<List<Doctor>> = _approvedDoctors

    private val _pendingDoctors = MutableStateFlow<List<PendingDoctorWithDetails>>(emptyList())
    val pendingDoctors: StateFlow<List<PendingDoctorWithDetails>> = _pendingDoctors

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        var doctorsLoaded = false
        var pendingLoaded = false

        var allDoctorDocs = listOf<Doctor>()
        var pendingUserDocs = listOf<User>()

        fun updatePendingList() {
            _pendingDoctors.value = pendingUserDocs.map { user ->
                val matchingDoc = allDoctorDocs.find { it.userId == user.userId || it.doctorId == user.userId }
                PendingDoctorWithDetails(user, matchingDoc)
            }
        }

        fun checkLoading() {
            if (doctorsLoaded && pendingLoaded) {
                _isLoading.value = false
            }
        }

        FirebaseConfig.doctorsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Failed to load doctors: ${error.message}"
            } else if (snapshot != null) {
                val all = snapshot.toObjects(Doctor::class.java)
                allDoctorDocs = all
                _approvedDoctors.value = all.filter { it.isApproved }
                updatePendingList()
            }
            doctorsLoaded = true
            checkLoading()
        }

        FirebaseConfig.usersRef
            .whereEqualTo("role", "DOCTOR")
            .whereEqualTo("approved", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Failed to load pending doctors: ${error.message}"
                } else if (snapshot != null) {
                    pendingUserDocs = snapshot.toObjects(User::class.java)
                    updatePendingList()
                }
                pendingLoaded = true
                checkLoading()
            }

        FirebaseConfig.departmentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Failed to load departments: ${error.message}"
            } else if (snapshot != null) {
                _departments.value = snapshot.toObjects(Department::class.java)
            }
        }
    }

    fun approveDoctor(user: User) {
        val doctorRef = FirebaseConfig.doctorsRef.document(user.userId)
        val userRef = FirebaseConfig.usersRef.document(user.userId)

        val doctorUpdates = mapOf<String, Any>(
            "isApproved" to true,
            "approved" to true
        )

        doctorRef.update(doctorUpdates).addOnSuccessListener {
            userRef.update("approved", true)
        }.addOnFailureListener {
            doctorRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    doctorRef.update(doctorUpdates)
                } else {
                    val doctor = Doctor(
                        doctorId = user.userId,
                        userId = user.userId,
                        name = user.name,
                        email = user.email,
                        displayId = user.displayId,
                        isApproved = true
                    )
                    doctorRef.set(doctor)
                }
                userRef.update("approved", true)
            }
        }
    }

    fun rejectDoctor(user: User) {
        FirebaseConfig.usersRef.document(user.userId).delete()
        FirebaseConfig.doctorsRef.document(user.userId).delete()
    }

    fun removeDoctor(doctorId: String) {
        doctorRepository.removeDoctor(doctorId)
    }

    fun updateDoctor(doctor: Doctor) {
        doctorRepository.updateDoctor(doctor)
    }
}
