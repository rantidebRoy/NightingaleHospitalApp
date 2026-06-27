package com.example.nightingalehospitalapp.repository.test

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.test.TestResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TestResultRepository {

    /**
     * Real-time stream of test results for a given patient.
     */
    fun observeTestResultsForPatient(patientId: String): Flow<List<TestResult>> = callbackFlow {
        val registration = FirebaseConfig.testResultsRef
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(TestResult::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
}
