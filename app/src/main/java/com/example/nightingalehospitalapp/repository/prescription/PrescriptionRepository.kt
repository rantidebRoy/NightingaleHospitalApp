package com.example.nightingalehospitalapp.repository.prescription

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.prescription.Prescription
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PrescriptionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /* ------------------ READ (real-time) ------------------ */

    fun observePrescriptionsForPatient(patientId: String): Flow<List<Prescription>> = callbackFlow {
        val reg: ListenerRegistration = db.collection("prescriptions")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Prescription::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /* ------------------ READ (one-shot) ------------------ */

    suspend fun getPrescriptionsForPatient(patientId: String): List<Prescription> {
        return try {
            val snap = db.collection("prescriptions")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(Prescription::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /* ------------------ WRITE ------------------ */

    fun addPrescription(prescription: Prescription) {

        val id = FirebaseConfig.prescriptionsRef.document().id
            ?: throw Exception("Failed to generate prescription ID")

        val updatedPrescription = prescription.copy(prescriptionId = id)

        FirebaseConfig.prescriptionsRef
            .document(id)
            .set(updatedPrescription)
    }
}