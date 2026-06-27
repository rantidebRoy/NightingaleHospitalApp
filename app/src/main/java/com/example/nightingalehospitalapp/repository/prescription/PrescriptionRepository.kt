package com.example.nightingalehospitalapp.repository.prescription

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.prescription.Prescription
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PrescriptionRepository {

    /* ------------------ WRITE ------------------ */

    fun addPrescription(prescription: Prescription) {
        val id = FirebaseConfig.prescriptionsRef.document().id

        val updatedPrescription = prescription.copy(
            prescriptionId = id
        )

        FirebaseConfig.prescriptionsRef
            .document(id)
            .set(updatedPrescription)
    }

    /* ------------------ READ (real-time) ------------------ */

    /**
     * Real-time stream of prescriptions for a given patient,
     * sorted by date string descending.
     */
    fun observePrescriptionsForPatient(patientId: String): Flow<List<Prescription>> = callbackFlow {
        val registration = FirebaseConfig.prescriptionsRef
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Prescription::class.java)
                }?.sortedByDescending { it.date } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /* ------------------ HELPERS ------------------ */

    /**
     * Fetches the doctor's display name from the users collection.
     */
    suspend fun getDoctorName(doctorId: String): String {
        return try {
            val doc = FirebaseConfig.usersRef.document(doctorId).get().await()
            doc.getString("name") ?: "Unknown Doctor"
        } catch (_: Exception) {
            "Unknown Doctor"
        }
    }
}