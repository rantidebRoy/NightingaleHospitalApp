package com.example.nightingalehospitalapp.repository.admission

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.admission.Admission
import com.example.nightingalehospitalapp.models.enums.AdmissionStatus

class AdmissionRepository {

    fun admitPatient(admission: Admission, onResult: (Boolean, String?) -> Unit) {

        val id = FirebaseConfig.admissionsRef.document().id
            ?: return onResult(false, "Failed to generate admission ID")

        val updatedAdmission = admission.copy(id = id)

        FirebaseConfig.admissionsRef
            .document(id)
            .set(updatedAdmission)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    fun getAdmittedPatients(onResult: (List<Admission>?, String?) -> Unit) {
        FirebaseConfig.admissionsRef
            .whereEqualTo("status", AdmissionStatus.ADMITTED.name)
            .get()
            .addOnSuccessListener { documents ->
                val admissions = documents.mapNotNull { it.toObject(Admission::class.java) }
                onResult(admissions, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    fun dischargePatient(admissionId: String, onResult: (Boolean, String?) -> Unit) {
        FirebaseConfig.admissionsRef
            .document(admissionId)
            .update(
                mapOf(
                    "status" to AdmissionStatus.DISCHARGED.name,
                    "dischargeDate" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }
}