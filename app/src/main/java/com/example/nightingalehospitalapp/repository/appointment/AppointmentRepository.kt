package com.example.nightingalehospitalapp.repository.appointment

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.appointment.Appointment
import com.example.nightingalehospitalapp.models.enums.AppointmentStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AppointmentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /* ------------------ READ (real-time) ------------------ */

    fun observeAppointmentsForDoctor(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        val reg: ListenerRegistration =
            db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents
                        ?.mapNotNull { it.toObject(Appointment::class.java) }
                        ?: emptyList()
                    trySend(list)
                }
        awaitClose { reg.remove() }
    }

    fun observeAppointmentsForPatient(patientId: String): Flow<List<Appointment>> = callbackFlow {
        val reg: ListenerRegistration =
            db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents
                        ?.mapNotNull { it.toObject(Appointment::class.java) }
                        ?: emptyList()
                    trySend(list)
                }
        awaitClose { reg.remove() }
    }

    /* ------------------ READ (one-shot) ------------------ */

    suspend fun getAppointmentsForDoctor(doctorId: String): List<Appointment> {
        return try {
            val snap = db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(Appointment::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAppointmentsForPatient(patientId: String): List<Appointment> {
        return try {
            val snap = db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(Appointment::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /* ------------------ WRITE ------------------ */

    fun bookAppointment(appointment: Appointment) {
        val id = FirebaseConfig.appointmentsRef.document().id
        val updatedAppointment = appointment.copy(appointmentId = id)
        FirebaseConfig.appointmentsRef.document(id).set(updatedAppointment)
    }

    suspend fun updateStatus(
        appointmentId: String,
        newStatus: AppointmentStatus
    ): Result<Unit> {
        return try {
            db.collection("appointments")
                .document(appointmentId)
                .update("status", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /* ------------------ DEMO DATA ------------------ */

    /**
     * Inserts a small set of demo appointments if the collection is empty.
     * Safe to call multiple times - it only seeds when there are no docs.
     */
    suspend fun seedDemoDataIfEmpty(doctorId: String) {
        try {
            val existing = db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            if (!existing.isEmpty) return

            val demos = listOf(
                Appointment(
                    appointmentId = "",
                    doctorId = doctorId,
                    patientId = "demo-patient-1",
                    patientName = "Rahul Sharma",
                    patientAge = 34,
                    patientGender = "Male",
                    date = "2026-06-28",
                    time = "10:30 AM",
                    status = AppointmentStatus.PENDING,
                    notes = "Recurring headache for the past 5 days."
                ),
                Appointment(
                    appointmentId = "",
                    doctorId = doctorId,
                    patientId = "demo-patient-2",
                    patientName = "Priya Verma",
                    patientAge = 28,
                    patientGender = "Female",
                    date = "2026-06-28",
                    time = "11:15 AM",
                    status = AppointmentStatus.CONFIRMED,
                    notes = "Follow-up for blood pressure monitoring."
                ),
                Appointment(
                    appointmentId = "",
                    doctorId = doctorId,
                    patientId = "demo-patient-3",
                    patientName = "Amit Kumar",
                    patientAge = 52,
                    patientGender = "Male",
                    date = "2026-06-29",
                    time = "09:00 AM",
                    status = AppointmentStatus.PENDING,
                    notes = "Persistent cough and mild fever."
                ),
                Appointment(
                    appointmentId = "",
                    doctorId = doctorId,
                    patientId = "demo-patient-4",
                    patientName = "Sneha Iyer",
                    patientAge = 22,
                    patientGender = "Female",
                    date = "2026-06-30",
                    time = "02:45 PM",
                    status = AppointmentStatus.COMPLETED,
                    notes = "Routine annual check-up."
                )
            )
            demos.forEach { bookAppointment(it) }
        } catch (_: Exception) {
            // Firestore unreachable - app should still work using in-memory fallback in ViewModel.
        }
    }
}