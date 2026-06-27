package com.example.nightingalehospitalapp.repository.auth

import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    /* ------------------ REGISTER USER ------------------ */

    fun registerUser(
        user: User,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener {

                val uid = auth.currentUser?.uid
                    ?: return@addOnSuccessListener

                val approvedStatus =
                    if (user.role == "DOCTOR") false else true

                val updatedUser = user.copy(
                    userId = uid,
                    approved = approvedStatus
                )

                FirebaseConfig.usersRef.child(uid)
                    .setValue(updatedUser)
                    .addOnSuccessListener {

                        onResult(true, null)

                    }
                    .addOnFailureListener {
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

                FirebaseConfig.usersRef.child(uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {

                            if (!snapshot.exists()) {
                                auth.signOut()
                                onResult(null, "User profile not found")
                                return
                            }

                            val role =
                                snapshot.child("role").getValue(String::class.java)

                            val approved =
                                snapshot.child("approved").getValue(Boolean::class.java)

                            if (role == "DOCTOR" && approved == false) {

                                auth.signOut()
                                onResult(null, "Doctor not approved yet")
                                return
                            }

                            onResult(role, null)
                        }

                        override fun onCancelled(error: DatabaseError) {

                            onResult(null, error.message)

                        }
                    })
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

        FirebaseConfig.usersRef.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        auth.signOut()
                        onResult(null, "User profile not found")
                        return
                    }

                    val role = snapshot.child("role").getValue(String::class.java)
                    val approved = snapshot.child("approved").getValue(Boolean::class.java)

                    if (role == "DOCTOR" && approved == false) {
                        auth.signOut()
                        onResult(null, "Doctor not approved yet")
                        return
                    }

                    onResult(role, null)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null, error.message)
                }
            })
    }

    /* ------------------ LOGOUT ------------------ */

    fun logoutUser() {
        auth.signOut()
    }
}