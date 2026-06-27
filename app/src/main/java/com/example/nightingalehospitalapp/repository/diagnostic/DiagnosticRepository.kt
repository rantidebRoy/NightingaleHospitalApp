package com.example.nightingalehospitalapp.repository.diagnostic

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.diagnostic.DiagnosticTest
import com.example.nightingalehospitalapp.models.diagnostic.TestBooking
import com.example.nightingalehospitalapp.models.diagnostic.TestResult
import com.example.nightingalehospitalapp.models.enums.TestStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DiagnosticRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /* ------------------ READ (real-time) ------------------ */

    fun observeTestResultsForPatient(patientId: String): Flow<List<TestResult>> = callbackFlow {
        val reg: ListenerRegistration = db.collection("test_results")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(TestResult::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /* ------------------ READ (one-shot) ------------------ */

    suspend fun getTestResultsForPatient(patientId: String): List<TestResult> {
        return try {
            val snap = db.collection("test_results")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(TestResult::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /* ------------------ READ (real-time) — Test bookings for patient ------------------ */

    fun observeTestBookingsForPatient(patientId: String): Flow<List<TestBooking>> = callbackFlow {
        val reg: ListenerRegistration = db.collection("test_bookings")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(TestBooking::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /* ------------------ READ (one-shot) — Single diagnostic test lookup ------------------ */

    suspend fun getDiagnosticTest(testId: String): DiagnosticTest? {
        if (testId.isBlank()) return null
        return try {
            val snap = db.collection("diagnostic_tests")
                .document(testId)
                .get()
                .await()
            snap.toObject(DiagnosticTest::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /* ------------------ WRITE ------------------ */

    fun bookTest(testBooking: TestBooking, onResult: (Boolean, String?) -> Unit) {

        val id = FirebaseConfig.testBookingsRef.document().id
            ?: return onResult(false, "Failed to generate test booking ID")

        val updatedBooking = testBooking.copy(bookingId = id)

        FirebaseConfig.testBookingsRef
            .document(id)
            .set(updatedBooking)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun getAllTestBookings(onResult: (List<TestBooking>?, String?) -> Unit) {
        FirebaseConfig.testBookingsRef
            .get()
            .addOnSuccessListener { documents ->
                val bookings = documents.mapNotNull { it.toObject(TestBooking::class.java) }
                onResult(bookings, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    fun updateTestBookingStatus(bookingId: String, status: TestStatus, onResult: (Boolean, String?) -> Unit) {
        FirebaseConfig.testBookingsRef
            .document(bookingId)
            .update("status", status.name)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun saveTestResult(testResult: TestResult, onResult: (Boolean, String?) -> Unit) {
        val id = FirebaseConfig.testResultsRef.document().id
            ?: return onResult(false, "Failed to generate test result ID")
            
        val updatedResult = testResult.copy(resultId = id)
        
        FirebaseConfig.testResultsRef
            .document(id)
            .set(updatedResult)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun getAllDiagnosticTests(onResult: (List<DiagnosticTest>?, String?) -> Unit) {
        FirebaseConfig.diagnosticTestsRef
            .get()
            .addOnSuccessListener { documents ->
                val tests = documents.mapNotNull { it.toObject(DiagnosticTest::class.java) }
                onResult(tests, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    fun addTest(test: DiagnosticTest) {
        val id = FirebaseConfig.diagnosticTestsRef.document().id
            ?: throw Exception("Failed to generate test ID")
        val updatedTest = test.copy(testId = id)
        FirebaseConfig.diagnosticTestsRef.document(id).set(updatedTest)
    }

    fun updateTest(test: DiagnosticTest) {
        FirebaseConfig.diagnosticTestsRef.document(test.testId).set(test)
    }

    fun removeTest(testId: String) {
        FirebaseConfig.diagnosticTestsRef.document(testId).delete()
    }
}