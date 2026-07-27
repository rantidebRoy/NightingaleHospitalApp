package com.example.nightingalehospitalapp.models.hospital
import com.example.nightingalehospitalapp.models.enums.BedStatus


data class Bed(

    val bedId: String = "",
    val bedNumber: String = "",
    val roomNumber: String = "",
    val ward: String = "",
    val status: BedStatus = BedStatus.AVAILABLE,
    val patientId: String? = null

)