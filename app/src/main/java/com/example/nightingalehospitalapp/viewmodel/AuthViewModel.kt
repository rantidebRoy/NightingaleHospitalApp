package com.example.nightingalehospitalapp.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.Patient
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.auth.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    val departments = mutableStateListOf<Department>()

    fun fetchDepartments() {
        FirebaseConfig.departmentsRef.get()
            .addOnSuccessListener { snapshot ->
                departments.clear()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Department::class.java)?.copy(departmentId = doc.id)
                }
                departments.addAll(list)
            }
    }

    fun registerUser(
        user: User,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        repository.registerUser(user, password, callback)
    }

    fun registerPatient(
        user: User,
        patient: Patient,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        repository.registerPatient(user, patient, password, callback)
    }

    fun registerDoctor(
        user: User,
        doctor: Doctor,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        repository.registerDoctor(user, doctor, password, callback)
    }

    fun loginUser(
        email: String,
        password: String,
        callback: (String?, String?) -> Unit
    ) {
        repository.loginUser(email, password, callback)
    }

    fun checkSession(callback: (String?, String?) -> Unit) {
        repository.checkSession(callback)
    }

    fun logoutUser() {
        repository.logoutUser()
    }
}