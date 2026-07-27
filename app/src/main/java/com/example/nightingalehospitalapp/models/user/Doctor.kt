package com.example.nightingalehospitalapp.models.user
import com.example.nightingalehospitalapp.models.enums.UserRole

import com.google.firebase.firestore.PropertyName

data class Doctor(
    val doctorId: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val specialization: String = "",
    val qualification: String = "",
    val experienceYears: Int = 0,
    val departmentId: String = "",
    @get:PropertyName("isApproved") @set:PropertyName("isApproved") var isApproved: Boolean = false,
    val displayId: String = ""
)