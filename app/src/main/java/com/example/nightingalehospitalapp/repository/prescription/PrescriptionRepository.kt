package com.example.nightingalehospitalapp.repository.prescription

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.prescription.Prescription
import com.example.nightingalehospitalapp.models.prescription.PrescriptionMedicine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PrescriptionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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

    /* ------------------ READ (real-time) — Medicines for prescription ------------------ */

    /**
     * Real-time stream of medicines tied to a specific prescription.
     */
    fun observeMedicinesForPrescription(prescriptionId: String): Flow<List<PrescriptionMedicine>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("prescription_medicines")
            .whereEqualTo("prescriptionId", prescriptionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(PrescriptionMedicine::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun redactPrescription(prescriptionId: String, reason: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "isRedacted" to true,
                "redactionReason" to reason
            )
            FirebaseConfig.prescriptionsRef.document(prescriptionId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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