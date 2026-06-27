package com.example.nightingalehospitalapp.viewmodel.admin.diagnostic

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.diagnostic.DiagnosticTest
import com.example.nightingalehospitalapp.models.diagnostic.TestBooking
import com.example.nightingalehospitalapp.models.enums.TestStatus
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.diagnostic.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookTestViewModel : ViewModel() {
    private val diagnosticRepository = DiagnosticRepository()

    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors: StateFlow<List<Doctor>> = _doctors

    private val _diagnosticTests = MutableStateFlow<List<DiagnosticTest>>(emptyList())
    val diagnosticTests: StateFlow<List<DiagnosticTest>> = _diagnosticTests

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

        // Fetch Diagnostic Tests
        diagnosticRepository.getAllDiagnosticTests { tests, _ ->
            _diagnosticTests.value = tests ?: emptyList()
            checkFinished()
        }
    }

    fun submitTestBooking(
        patientId: String,
        doctorId: String,
        testId: String,
        date: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (patientId.isEmpty() || doctorId.isEmpty() || testId.isEmpty() || date.isEmpty()) {
            onResult(false, "Please fill all fields")
            return
        }

        val testBooking = TestBooking(
            testId = testId,
            patientId = patientId,
            doctorId = doctorId,
            date = date,
            status = TestStatus.SCHEDULED
        )

        diagnosticRepository.bookTest(testBooking) { success, error ->
            onResult(success, error ?: "Failed to book test")
        }
    }
}
