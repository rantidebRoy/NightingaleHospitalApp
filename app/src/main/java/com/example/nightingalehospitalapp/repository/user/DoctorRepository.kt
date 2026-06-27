package com.example.nightingalehospitalapp.repository.user

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import kotlinx.coroutines.tasks.await

data class DoctorWithUser(
    val doctor: Doctor,
    val user: User
)

class DoctorRepository {

    fun addDoctor(doctor: Doctor) {
        val id = FirebaseConfig.doctorsRef.document().id
        val updatedDoctor = doctor.copy(doctorId = id)
        FirebaseConfig.doctorsRef
            .document(id)
            .set(updatedDoctor)
    }

    suspend fun getDoctorsWithDetails(): List<DoctorWithUser> {
        return try {
            // First, get all users with role == "DOCTOR"
            val usersSnap = FirebaseConfig.usersRef.whereEqualTo("role", "DOCTOR").get().await()
            val doctorUsers = usersSnap.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(userId = doc.id)
            }

            // Then, fetch all doctors from the doctors collection to match them
            val doctorsSnap = FirebaseConfig.doctorsRef.get().await()
            val doctorProfiles = doctorsSnap.documents.mapNotNull { it.toObject(Doctor::class.java) }

            val result = mutableListOf<DoctorWithUser>()
            for (user in doctorUsers) {
                // Find matching profile or create a default one
                val profile = doctorProfiles.find { it.userId == user.userId }
                    ?: Doctor(doctorId = user.userId, userId = user.userId, specialization = "General")

                result.add(DoctorWithUser(profile, user))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateDoctor(doctor: Doctor) {
        FirebaseConfig.doctorsRef.document(doctor.doctorId).set(doctor)
        if (doctor.isApproved) {
            FirebaseConfig.usersRef.document(doctor.userId).update("approved", true)
        }
    }

    fun removeDoctor(doctorId: String) {
        FirebaseConfig.doctorsRef.document(doctorId).get().addOnSuccessListener { document ->
            val userId = document.getString("userId")
            if (userId != null) {
                FirebaseConfig.usersRef.document(userId).delete()
            }
            FirebaseConfig.doctorsRef.document(doctorId).delete()
        }.addOnFailureListener {
            FirebaseConfig.doctorsRef.document(doctorId).delete()
        }
    }
}