package com.example.nightingalehospitalapp.repository.bed

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.hospital.Bed
import com.example.nightingalehospitalapp.models.enums.BedStatus

class BedRepository {

    fun addBed(bed: Bed, onResult: (Boolean, String?) -> Unit = {_,_->}) {

        val id = FirebaseConfig.bedsRef.document().id
            ?: return onResult(false, "Failed to generate bed ID")

        val updatedBed = bed.copy(bedId = id)

        FirebaseConfig.bedsRef
            .document(id)
            .set(updatedBed)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun updateBed(bed: Bed, onResult: (Boolean, String?) -> Unit = {_,_->}) {
        FirebaseConfig.bedsRef.document(bed.bedId).set(bed)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun removeBed(bedId: String, onResult: (Boolean, String?) -> Unit = {_,_->}) {
        FirebaseConfig.bedsRef.document(bedId).delete()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun getBedsByStatus(status: BedStatus, onResult: (List<Bed>?, String?) -> Unit) {
        FirebaseConfig.bedsRef
            .whereEqualTo("status", status.name)
            .get()
            .addOnSuccessListener { documents ->
                val beds = documents.mapNotNull { it.toObject(Bed::class.java) }
                onResult(beds, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    fun updateBedStatus(bedId: String, status: BedStatus, patientId: String?, onResult: (Boolean, String?) -> Unit) {
        val updates = mutableMapOf<String, Any>(
            "status" to status.name
        )
        if (patientId != null) {
            updates["patientId"] = patientId
        } else {
            // Need to remove the patientId from the bed
            // We can update to null, but Firestore update map doesn't easily accept nulls unless we use FieldValue.delete()
            // It's safer to just set null explicitly if the map allows it or use FieldValue.delete()
            updates["patientId"] = com.google.firebase.firestore.FieldValue.delete()
        }
        
        FirebaseConfig.bedsRef.document(bedId)
            .update(updates)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }
}