package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.appointment.Appointment
import com.example.nightingalehospitalapp.models.appointment.Slot
import com.example.nightingalehospitalapp.repository.appointment.AppointmentRepository
import com.example.nightingalehospitalapp.repository.appointment.SlotRepository
import com.example.nightingalehospitalapp.repository.user.DoctorRepository
import com.example.nightingalehospitalapp.repository.user.DoctorWithUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookingViewModel : ViewModel() {

    private val doctorRepository = DoctorRepository()
    private val slotRepository = SlotRepository()
    private val appointmentRepository = AppointmentRepository()

    private val _doctors = MutableStateFlow<List<DoctorWithUser>>(emptyList())
    val doctors: StateFlow<List<DoctorWithUser>> = _doctors.asStateFlow()

    private val _availableSlots = MutableStateFlow<List<Slot>>(emptyList())
    val availableSlots: StateFlow<List<Slot>> = _availableSlots.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    fun fetchDoctors() {
        viewModelScope.launch {
            _doctors.value = doctorRepository.getDoctorsWithDetails()
        }
    }

    fun fetchAvailableSlots(doctorId: String, date: String) {
        viewModelScope.launch {
            _availableSlots.value = slotRepository.getAvailableSlots(doctorId, date)
        }
    }

    fun bookAppointment(
        doctorId: String,
        patientId: String,
        patientName: String, // Ideally fetched from current user
        patientAge: Int = 30, // Mock for now
        patientGender: String = "Unknown", // Mock for now
        date: String,
        time: String,
        notes: String,
        slotId: String
    ) {
        _bookingState.value = BookingState.Loading
        viewModelScope.launch {
            try {
                // Mark slot as booked
                val slotResult = slotRepository.markSlotAsBooked(slotId, patientId, patientName)
                if (slotResult.isSuccess) {
                    val appointment = Appointment(
                        doctorId = doctorId,
                        patientId = patientId,
                        patientName = patientName,
                        patientAge = patientAge,
                        patientGender = patientGender,
                        date = date,
                        time = time,
                        notes = notes
                    )
                    appointmentRepository.bookAppointment(appointment)
                    _bookingState.value = BookingState.Success
                } else {
                    _bookingState.value = BookingState.Error("Failed to book slot")
                }
            } catch (e: Exception) {
                _bookingState.value = BookingState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun resetBookingState() {
        _bookingState.value = BookingState.Idle
    }

    sealed class BookingState {
        object Idle : BookingState()
        object Loading : BookingState()
        object Success : BookingState()
        data class Error(val message: String) : BookingState()
    }
}
