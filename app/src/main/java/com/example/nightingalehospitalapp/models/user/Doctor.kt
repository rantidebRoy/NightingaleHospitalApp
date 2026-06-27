package com.example.nightingalehospitalapp.models.user
import com.example.nightingalehospitalapp.models.enums.UserRole

data class Doctor(
    val doctorId: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val specialization: String = "",
    val qualification: String = "",
    val experienceYears: Int = 0,
    val departmentId: String = "",
    val isApproved: Boolean = false
)