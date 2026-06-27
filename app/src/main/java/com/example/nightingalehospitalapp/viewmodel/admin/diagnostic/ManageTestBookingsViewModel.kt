package com.example.nightingalehospitalapp.viewmodel.admin.diagnostic

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.diagnostic.TestResult
import com.example.nightingalehospitalapp.models.diagnostic.TestResultItem
import com.example.nightingalehospitalapp.models.enums.TestStatus
import com.example.nightingalehospitalapp.repository.diagnostic.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TestBookingItem(
    val bookingId: String,
    val testId: String,
    val testName: String,
    val patientId: String,
    val patientName: String,
    val doctorName: String,
    val date: String,
    val status: TestStatus
)

class ManageTestBookingsViewModel : ViewModel() {
    private val diagnosticRepository = DiagnosticRepository()

    private val _testBookings = MutableStateFlow<List<TestBookingItem>>(emptyList())
    val testBookings: StateFlow<List<TestBookingItem>> = _testBookings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchTestBookings()
    }

    fun fetchTestBookings() {
        _isLoading.value = true
        diagnosticRepository.getAllTestBookings { bookings, error ->
            if (error != null || bookings == null) {
                _errorMessage.value = error ?: "Failed to load test bookings"
                _isLoading.value = false
                return@getAllTestBookings
            }

            if (bookings.isEmpty()) {
                _testBookings.value = emptyList()
                _isLoading.value = false
                return@getAllTestBookings
            }

            val resultList = mutableListOf<TestBookingItem>()
            var fetchedCount = 0

            bookings.forEach { booking ->
                // Fetch Patient Name
                FirebaseConfig.usersRef.document(booking.patientId).get()
                    .addOnSuccessListener { patientDoc ->
                        val patientName = patientDoc.getString("name") ?: "Unknown Patient"
                        
                        // Fetch Doctor Name
                        FirebaseConfig.usersRef.document(booking.doctorId).get()
                            .addOnSuccessListener { doctorDoc ->
                                val doctorName = doctorDoc.getString("name") ?: "Unknown Doctor"
                                
                                // Fetch Test Name
                                FirebaseConfig.diagnosticTestsRef.document(booking.testId).get()
                                    .addOnSuccessListener { testDoc ->
                                        val testName = testDoc.getString("testName") ?: "Unknown Test"
                                        
                                        resultList.add(
                                            TestBookingItem(
                                                bookingId = booking.bookingId,
                                                testId = booking.testId,
                                                testName = testName,
                                                patientId = booking.patientId,
                                                patientName = patientName,
                                                doctorName = doctorName,
                                                date = booking.date,
                                                status = booking.status
                                            )
                                        )
                                        
                                        fetchedCount++
                                        if (fetchedCount == bookings.size) {
                                            _testBookings.value = resultList
                                            _isLoading.value = false
                                        }
                                    }
                            }
                    }
            }
        }
    }

    fun updateStatus(bookingId: String, newStatus: TestStatus, onResult: (Boolean, String?) -> Unit) {
        diagnosticRepository.updateTestBookingStatus(bookingId, newStatus) { success, error ->
            if (success) {
                fetchTestBookings()
                onResult(true, null)
            } else {
                onResult(false, error ?: "Failed to update status")
            }
        }
    }

    fun uploadTestResult(
        bookingId: String,
        patientId: String,
        results: List<TestResultItem>,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (results.isEmpty()) {
            onResult(false, "No results provided")
            return
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val testResult = TestResult(
            patientId = patientId,
            date = dateStr,
            results = results
        )

        diagnosticRepository.saveTestResult(testResult) { success, error ->
            if (success) {
                // Update booking status to COMPLETED
                diagnosticRepository.updateTestBookingStatus(bookingId, TestStatus.COMPLETED) { statusSuccess, statusError ->
                    if (statusSuccess) {
                        fetchTestBookings()
                        onResult(true, null)
                    } else {
                        onResult(false, statusError ?: "Result saved, but failed to update status")
                    }
                }
            } else {
                onResult(false, error ?: "Failed to save test result")
            }
        }
    }
}
