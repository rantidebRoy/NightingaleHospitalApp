package com.example.nightingalehospitalapp.repository.prescription

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.prescription.Medicine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MedicineRepository {

    /**
     * Real-time stream of all medicines available in the hospital collection.
     */
    fun observeAllMedicines(): Flow<List<Medicine>> = callbackFlow {
        val registration = FirebaseConfig.medicinesRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Medicine::class.java)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
}
