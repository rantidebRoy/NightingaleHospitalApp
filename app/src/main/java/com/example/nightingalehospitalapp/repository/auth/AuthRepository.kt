package com.example.nightingalehospitalapp.repository.auth

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.user.User
import com.google.firebase.auth.FirebaseAuth

import com.example.nightingalehospitalapp.models.user.Patient
import com.example.nightingalehospitalapp.models.user.Doctor

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    /* ------------------ REGISTER USER (GENERIC) ------------------ */

    fun registerUser(
        user: User,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                val approvedStatus = if (user.role == "DOCTOR") false else true
                
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val counterRef = db.collection("metadata").document("counters")
                
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(counterRef)
                    val currentCount = snapshot.getLong("globalUserCounter") ?: 1000L
                    val nextCount = currentCount + 1
                    
                    transaction.set(counterRef, mapOf("globalUserCounter" to nextCount), com.google.firebase.firestore.SetOptions.merge())
                    
                    val displayId = (if (user.role == "PATIENT") "P" else "D") + nextCount.toString()
                    val updatedUser = user.copy(
                        userId = uid,
                        approved = approvedStatus,
                        displayId = displayId
                    )
                    
                    transaction.set(FirebaseConfig.usersRef.document(uid), updatedUser)
                    null // return null for transaction result
                }.addOnSuccessListener {
                    onResult(true, null)
                }.addOnFailureListener {
                    onResult(false, it.message)
                }
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    /* ------------------ REGISTER PATIENT ------------------ */

    fun registerPatient(
        user: User,
        patient: Patient,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val counterRef = db.collection("metadata").document("counters")
                
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(counterRef)
                    val currentCount = snapshot.getLong("globalUserCounter") ?: 1000L
                    val nextCount = currentCount + 1
                    
                    transaction.set(counterRef, mapOf("globalUserCounter" to nextCount), com.google.firebase.firestore.SetOptions.merge())
                    
                    val displayId = "P$nextCount"
                    val updatedUser = user.copy(
                        userId = uid,
                        role = "PATIENT",
                        approved = true,
                        displayId = displayId
                    )
                    val updatedPatient = patient.copy(
                        patientId = uid,
                        userId = uid,
                        displayId = displayId
                    )
                    
                    transaction.set(FirebaseConfig.usersRef.document(uid), updatedUser)
                    transaction.set(FirebaseConfig.patientsRef.document(uid), updatedPatient)
                    null
                }.addOnSuccessListener {
                    onResult(true, null)
                }.addOnFailureListener {
                    onResult(false, it.message)
                }
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    /* ------------------ REGISTER DOCTOR ------------------ */

    fun registerDoctor(
        user: User,
        doctor: Doctor,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val counterRef = db.collection("metadata").document("counters")
                
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(counterRef)
                    val currentCount = snapshot.getLong("globalUserCounter") ?: 1000L
                    val nextCount = currentCount + 1
                    
                    transaction.set(counterRef, mapOf("globalUserCounter" to nextCount), com.google.firebase.firestore.SetOptions.merge())
                    
                    val displayId = "D$nextCount"
                    val updatedUser = user.copy(
                        userId = uid,
                        role = "DOCTOR",
                        approved = false,
                        displayId = displayId
                    )
                    val updatedDoctor = doctor.copy(
                        doctorId = uid,
                        userId = uid,
                        displayId = displayId,
                        name = user.name,
                        email = user.email,
                        isApproved = false
                    )
                    
                    transaction.set(FirebaseConfig.usersRef.document(uid), updatedUser)
                    transaction.set(FirebaseConfig.doctorsRef.document(uid), updatedDoctor)
                    null
                }.addOnSuccessListener {
                    onResult(true, null)
                }.addOnFailureListener {
                    onResult(false, it.message)
                }
            }
            .addOnFailureListener {
                onResult(false, it.message)
            }
    }

    /* ------------------ LOGIN USER ------------------ */

    fun loginUser(
        email: String,
        password: String,
        onResult: (String?, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                FirebaseConfig.usersRef.document(uid).get()
                    .addOnSuccessListener { document ->
                        if (!document.exists()) {
                            auth.signOut()
                            onResult(null, "User profile not found")
                            return@addOnSuccessListener
                        }

                        val role = document.getString("role")
                        val approved = document.getBoolean("approved")

                        if (role == "DOCTOR" && approved == false) {
                            auth.signOut()
                            onResult(null, "Doctor not approved yet")
                            return@addOnSuccessListener
                        }

                        onResult(role, null)
                    }
                    .addOnFailureListener {
                        onResult(null, it.message)
                    }
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    /* ------------------ SESSION CHECK ------------------ */

    fun checkSession(onResult: (String?, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(null, null)
            return
        }

        FirebaseConfig.usersRef.document(uid).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    auth.signOut()
                    onResult(null, "User profile not found")
                    return@addOnSuccessListener
                }

                val role = document.getString("role")
                val approved = document.getBoolean("approved")

                if (role == "DOCTOR" && approved == false) {
                    auth.signOut()
                    onResult(null, "Doctor not approved yet")
                    return@addOnSuccessListener
                }

                onResult(role, null)
            }
            .addOnFailureListener {
                onResult(null, it.message)
            }
    }

    /* ------------------ LOGOUT ------------------ */

    fun logoutUser() {
        auth.signOut()
    }
}