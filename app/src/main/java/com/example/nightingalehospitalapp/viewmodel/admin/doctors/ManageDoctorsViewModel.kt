package com.example.nightingalehospitalapp.viewmodel.admin.doctors

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.user.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ManageDoctorsViewModel(
    private val doctorRepository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _approvedDoctors = MutableStateFlow<List<Doctor>>(emptyList())
    val approvedDoctors: StateFlow<List<Doctor>> = _approvedDoctors

    private val _pendingDoctors = MutableStateFlow<List<User>>(emptyList())
    val pendingDoctors: StateFlow<List<User>> = _pendingDoctors

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments

    init {
        FirebaseConfig.doctorsRef.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                _approvedDoctors.value = snapshot.toObjects(Doctor::class.java)
            }
        }

        FirebaseConfig.usersRef
            .whereEqualTo("role", "DOCTOR")
            .whereEqualTo("approved", false)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    _pendingDoctors.value = snapshot.toObjects(User::class.java)
                }
            }

        FirebaseConfig.departmentsRef.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                _departments.value = snapshot.toObjects(Department::class.java)
            }
        }
    }

    fun approveDoctor(user: User) {
        // Create the doctor in doctorsRef
        val doctor = Doctor(
            doctorId = user.userId,
            userId = user.userId,
            name = user.name,
            email = user.email
        )
        FirebaseConfig.doctorsRef.document(user.userId).set(doctor).addOnSuccessListener {
            // Update user to approved
            FirebaseConfig.usersRef.document(user.userId).update("approved", true)
        }
    }

    fun rejectDoctor(user: User) {
        FirebaseConfig.usersRef.document(user.userId).delete()
    }

    fun removeDoctor(doctorId: String) {
        doctorRepository.removeDoctor(doctorId)
    }

    fun updateDoctor(doctor: Doctor) {
        doctorRepository.updateDoctor(doctor)
    }
}
