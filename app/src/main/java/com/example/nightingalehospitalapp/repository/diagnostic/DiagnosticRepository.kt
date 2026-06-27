package com.example.nightingalehospitalapp.repository.diagnostic

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.diagnostic.TestBooking
import com.example.nightingalehospitalapp.models.diagnostic.TestResult
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

    /* ------------------ WRITE ------------------ */

    fun bookTest(testBooking: TestBooking) {

        val id = FirebaseConfig.testBookingsRef.document().id
            ?: throw Exception("Failed to generate test booking ID")

        val updatedBooking = testBooking.copy(bookingId = id)

        FirebaseConfig.testBookingsRef
            .document(id)
            .set(updatedBooking)
    }
}