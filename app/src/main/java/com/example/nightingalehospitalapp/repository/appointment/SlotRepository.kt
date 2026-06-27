package com.example.nightingalehospitalapp.repository.appointment

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.appointment.Slot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class SlotRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getAvailableSlots(doctorId: String, date: String): List<Slot> {
        return try {
            val snapshot = db.collection("slots")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .whereEqualTo("booked", false)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.toObject(Slot::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun observeSlotsForDoctor(doctorId: String, date: String): kotlinx.coroutines.flow.Flow<List<Slot>> = kotlinx.coroutines.flow.callbackFlow {
        val reg = db.collection("slots")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Slot::class.java) }
                    ?.sortedBy { it.time }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun observeSlotsForPatient(patientId: String): kotlinx.coroutines.flow.Flow<List<Slot>> = kotlinx.coroutines.flow.callbackFlow {
        val reg = db.collection("slots")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Slot::class.java) }
                    ?.sortedByDescending { it.date }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun addSlot(doctorId: String, date: String, time: String) {
        val ref = db.collection("slots").document()
        val slot = Slot(
            slotId = ref.id,
            doctorId = doctorId,
            date = date,
            time = time,
            booked = false
        )
        ref.set(slot)
    }

    suspend fun deleteSlot(slotId: String): Result<Unit> {
        return try {
            db.collection("slots").document(slotId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markSlotAsBooked(slotId: String, patientId: String, patientName: String): Result<Unit> {
        return try {
            db.collection("slots")
                .document(slotId)
                .update(
                    mapOf(
                        "booked" to true,
                        "patientId" to patientId,
                        "patientName" to patientName
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun freeSlot(slotId: String): Result<Unit> {
        return try {
            db.collection("slots")
                .document(slotId)
                .update(
                    mapOf(
                        "booked" to false,
                        "patientId" to "",
                        "patientName" to ""
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper function to insert demo slots
    suspend fun seedDemoSlotsIfEmpty(doctorId: String, date: String) {
        try {
            val existing = db.collection("slots")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .get()
                .await()
            
            if (!existing.isEmpty) return

            val times = listOf("09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "02:00 PM", "02:30 PM", "03:00 PM")
            
            val batch = db.batch()
            times.forEach { time ->
                val ref = db.collection("slots").document()
                val slot = Slot(
                    slotId = ref.id,
                    doctorId = doctorId,
                    date = date,
                    time = time,
                    booked = false
                )
                batch.set(ref, slot)
            }
            batch.commit().await()
        } catch (_: Exception) {
            // Ignore if fails
        }
    }
}
