package com.example.nightingalehospitalapp.models.prescription

import com.google.firebase.firestore.PropertyName

data class Prescription(

    val prescriptionId: String = "",
    val appointmentId: String = "",
    val doctorId: String = "",
    val patientId: String = "",

    val diagnosis: String = "",
    val date: String = "",

    @get:PropertyName("isRedacted")
    val isRedacted: Boolean = false,
    val redactionReason: String = ""

)