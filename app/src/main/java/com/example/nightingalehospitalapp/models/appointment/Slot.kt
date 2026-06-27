package com.example.nightingalehospitalapp.models.appointment

data class Slot(
    val slotId: String = "",
    val doctorId: String = "",
    val date: String = "",
    val time: String = "",
    val booked: Boolean = false,
    val patientId: String = "",
    val patientName: String = ""
)
