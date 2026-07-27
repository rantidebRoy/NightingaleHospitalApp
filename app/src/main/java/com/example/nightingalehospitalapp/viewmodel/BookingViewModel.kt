package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.appointment.Appointment
import com.example.nightingalehospitalapp.models.appointment.Slot
import com.example.nightingalehospitalapp.repository.appointment.AppointmentRepository
import com.example.nightingalehospitalapp.repository.appointment.SlotRepository
import com.example.nightingalehospitalapp.repository.user.DoctorRepository
import com.example.nightingalehospitalapp.repository.user.DoctorWithUser
import com.example.nightingalehospitalapp.models.user.Patient
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.database.FirebaseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookingViewModel : ViewModel() {

    private val doctorRepository = DoctorRepository()
    private val slotRepository = SlotRepository()
    private val appointmentRepository = AppointmentRepository()

    private val _allDoctors = MutableStateFlow<List<DoctorWithUser>>(emptyList())
    
    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val _selectedDepartment = MutableStateFlow<String>("All Departments")
    val selectedDepartment: StateFlow<String> = _selectedDepartment.asStateFlow()

    private val _doctors = MutableStateFlow<List<DoctorWithUser>>(emptyList())
    val doctors: StateFlow<List<DoctorWithUser>> = _doctors.asStateFlow()

    private val _availableSlots = MutableStateFlow<List<Slot>>(emptyList())
    val availableSlots: StateFlow<List<Slot>> = _availableSlots.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    fun fetchDoctors() {
        viewModelScope.launch {
            val list = doctorRepository.getDoctorsWithDetails()
            _allDoctors.value = list
            applyFilter()
        }
    }

    fun fetchDepartments() {
        viewModelScope.launch {
            try {
                val snapshot = FirebaseConfig.departmentsRef.get().await()
                val list = snapshot.documents.mapNotNull { it.toObject(Department::class.java) }
                _departments.value = list
                applyFilter()
            } catch (_: Exception) {}
        }
    }

    fun setDepartmentFilter(department: String) {
        _selectedDepartment.value = department
        applyFilter()
    }

    fun getDepartmentNameForDoctor(doctor: com.example.nightingalehospitalapp.models.user.Doctor): String {
        val deptList = _departments.value
        val matched = deptList.find { it.departmentId == doctor.departmentId || it.name.equals(doctor.departmentId, ignoreCase = true) }
        return matched?.name ?: doctor.departmentId.ifBlank { "General" }
    }

    private fun applyFilter() {
        val currentDept = _selectedDepartment.value
        if (currentDept == "All" || currentDept == "All Departments" || currentDept.isBlank()) {
            _doctors.value = _allDoctors.value
        } else {
            val deptList = _departments.value
            val targetDept = deptList.find { it.name.equals(currentDept, ignoreCase = true) }
            val targetDeptId = targetDept?.departmentId ?: currentDept

            _doctors.value = _allDoctors.value.filter { docWithUser ->
                val docDeptId = docWithUser.doctor.departmentId
                docDeptId.equals(targetDeptId, ignoreCase = true) ||
                docDeptId.equals(currentDept, ignoreCase = true) ||
                getDepartmentNameForDoctor(docWithUser.doctor).equals(currentDept, ignoreCase = true)
            }
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
        patientName: String,
        patientAge: Int = 0,
        patientGender: String = "",
        date: String,
        time: String,
        notes: String,
        slotId: String
    ) {
        _bookingState.value = BookingState.Loading
        viewModelScope.launch {
            try {
                var finalName = patientName
                var finalAge = patientAge
                var finalGender = patientGender

                if (patientId.isNotBlank()) {
                    val uDoc = try {
                        com.example.nightingalehospitalapp.database.FirebaseConfig.usersRef.document(patientId).get().await()
                    } catch (_: Exception) { null }

                    val pDoc = try {
                        com.example.nightingalehospitalapp.database.FirebaseConfig.patientsRef.document(patientId).get().await()
                    } catch (_: Exception) { null }

                    val fetchedName = uDoc?.getString("name")
                    if (!fetchedName.isNullOrBlank()) finalName = fetchedName

                    val patientObj = pDoc?.toObject(com.example.nightingalehospitalapp.models.user.Patient::class.java)
                    if (patientObj != null) {
                        if (patientObj.age > 0) finalAge = patientObj.age
                        if (patientObj.gender.isNotBlank()) finalGender = patientObj.gender
                    }
                }

                // Mark slot as booked
                val slotResult = slotRepository.markSlotAsBooked(slotId, patientId, finalName)
                if (slotResult.isSuccess) {
                    val appointment = Appointment(
                        doctorId = doctorId,
                        patientId = patientId,
                        patientName = finalName,
                        patientAge = finalAge,
                        patientGender = finalGender,
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
