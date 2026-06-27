package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.appointment.Slot
import com.example.nightingalehospitalapp.repository.appointment.SlotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManageSlotsViewModel : ViewModel() {

    private val slotRepository = SlotRepository()
    private var observeJob: Job? = null

    private val _slots = MutableStateFlow<List<Slot>>(emptyList())
    val slots: StateFlow<List<Slot>> = _slots.asStateFlow()

    fun observeSlots(doctorId: String, date: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            slotRepository.observeSlotsForDoctor(doctorId, date).collect { slotsList ->
                _slots.value = slotsList
            }
        }
    }

    fun addSlot(doctorId: String, date: String, time: String) {
        slotRepository.addSlot(doctorId, date, time)
    }

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            slotRepository.deleteSlot(slotId)
        }
    }
}
