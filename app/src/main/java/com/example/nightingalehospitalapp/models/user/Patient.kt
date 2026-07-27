package com.example.nightingalehospitalapp.models.user

data class Patient(
    val patientId: String = "",
    val userId: String = "",
    val displayId: String = "",
    val dob: String = "",
    val age: Int = 0,
    val gender: String = "",
    val bloodGroup: String = "",
    val address: String = "",
    val emergencyContact: String = ""
)