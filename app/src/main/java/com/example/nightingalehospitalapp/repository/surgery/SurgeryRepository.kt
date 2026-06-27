package com.example.nightingalehospitalapp.repository.surgery

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.enums.SurgeryStatus
import com.example.nightingalehospitalapp.models.hospital.OperationTheatre
import com.example.nightingalehospitalapp.models.surgery.SurgeryBooking
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SurgeryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /* ------------------ READ (real-time) ------------------ */

    fun observeSurgeriesForPatient(patientId: String): Flow<List<SurgeryBooking>> = callbackFlow {
        val reg: ListenerRegistration = db.collection("surgery_bookings")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(SurgeryBooking::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeSurgeriesForDoctor(doctorId: String): Flow<List<SurgeryBooking>> = callbackFlow {
        val reg: ListenerRegistration = db.collection("surgery_bookings")
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(SurgeryBooking::class.java) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /* ------------------ WRITE ------------------ */

    fun bookSurgery(surgery: SurgeryBooking, onResult: (Boolean, String?) -> Unit) {

        val id = FirebaseConfig.surgeryBookingsRef.document().id
            ?: return onResult(false, "Failed to generate surgery ID")

        val updatedSurgery = surgery.copy(surgeryId = id)

        FirebaseConfig.surgeryBookingsRef
            .document(id)
            .set(updatedSurgery)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun getAllSurgeries(onResult: (List<SurgeryBooking>?, String?) -> Unit) {
        FirebaseConfig.surgeryBookingsRef
            .get()
            .addOnSuccessListener { documents ->
                val surgeries = documents.mapNotNull { it.toObject(SurgeryBooking::class.java) }
                onResult(surgeries, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    fun updateSurgeryStatus(surgeryId: String, status: SurgeryStatus, onResult: (Boolean, String?) -> Unit) {
        FirebaseConfig.surgeryBookingsRef
            .document(surgeryId)
            .update("status", status.name)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun getAvailableOperationTheatres(onResult: (List<OperationTheatre>?, String?) -> Unit) {
        FirebaseConfig.operationTheatresRef
            .whereEqualTo("status", "AVAILABLE")
            .get()
            .addOnSuccessListener { documents ->
                val ots = documents.mapNotNull { it.toObject(OperationTheatre::class.java) }
                onResult(ots, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    fun updateOperationTheatreStatus(otId: String, status: String, onResult: (Boolean, String?) -> Unit) {
        FirebaseConfig.operationTheatresRef
            .document(otId)
            .update("status", status)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun addOperationTheatre(ot: OperationTheatre) {
        val id = FirebaseConfig.operationTheatresRef.document().id
            ?: throw Exception("Failed to generate OT ID")
        val updatedOT = ot.copy(otId = id)
        FirebaseConfig.operationTheatresRef.document(id).set(updatedOT)
    }

    fun updateOperationTheatre(ot: OperationTheatre) {
        FirebaseConfig.operationTheatresRef.document(ot.otId).set(ot)
    }

    fun removeOperationTheatre(otId: String) {
        FirebaseConfig.operationTheatresRef.document(otId).delete()
    }
}