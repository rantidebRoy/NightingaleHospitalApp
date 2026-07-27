package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightingalehospitalapp.models.diagnostic.TestBooking
import com.example.nightingalehospitalapp.models.prescription.Prescription
import com.example.nightingalehospitalapp.models.prescription.PrescriptionMedicine
import com.example.nightingalehospitalapp.models.surgery.SurgeryBooking
import com.example.nightingalehospitalapp.repository.diagnostic.DiagnosticRepository
import com.example.nightingalehospitalapp.repository.prescription.PrescriptionRepository
import com.example.nightingalehospitalapp.repository.surgery.SurgeryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * VM that drives PatientHistoryActivity. Combines 3 real-time repositories
 * (prescriptions, test bookings, surgeries) into a single
 * [PatientHistory] snapshot keyed by patient id.
 */
class PatientHistoryViewModel(
    private val prescriptionRepo: PrescriptionRepository = PrescriptionRepository(),
    private val diagnosticRepo: DiagnosticRepository = DiagnosticRepository(),
    private val surgeryRepo: SurgeryRepository = SurgeryRepository()
) : ViewModel() {

    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data object Empty : UiState()
        data class Loaded(val history: PatientHistory) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    /**
     * Starts observing every collection relevant to the patient's history.
     * Emits a new [PatientHistory] whenever any source updates.
     */
    fun observe(patientId: String) {
        if (patientId.isBlank()) {
            _uiState.value = UiState.Error("Invalid patient id")
            return
        }
        if (observeJob?.isActive == true) return

        _uiState.value = UiState.Loading
        observeJob = viewModelScope.launch {
            try {
                launch {
                    prescriptionRepo.observePrescriptionsForPatient(patientId)
                        .catch { /* swallow per-collection errors */ }
                        .collectLatest { prescriptions ->
                            val items = buildPrescriptionItems(prescriptions)
                            publishCombined(prescriptionItems = items)
                        }
                }
                launch {
                    diagnosticRepo.observeTestBookingsForPatient(patientId)
                        .catch { /* swallow per-collection errors */ }
                        .collectLatest { bookings ->
                            val items = buildTestBookingItems(bookings)
                            publishCombined(testBookingItems = items)
                        }
                }
                launch {
                    surgeryRepo.observeSurgeriesForPatient(patientId)
                        .catch { /* swallow per-collection errors */ }
                        .collectLatest { surgeries ->
                            val items = buildSurgeryItems(surgeries)
                            publishCombined(surgeryItems = items)
                        }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load history")
            }
        }
    }

    /* ------------------ builders ------------------ */

    private suspend fun buildPrescriptionItems(prescriptions: List<Prescription>): List<HistoryItem.PrescriptionRow> {
        return prescriptions.map { p ->
            val doctorName = prescriptionRepo.getDoctorName(p.doctorId)
            val medicines: List<PrescriptionMedicine> = try {
                prescriptionRepo.observeMedicinesForPrescription(p.prescriptionId).firstOrNull()
                    ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            HistoryItem.PrescriptionRow(
                prescriptionId = p.prescriptionId,
                doctorName = doctorName,
                date = p.date,
                diagnosis = p.diagnosis,
                medicines = medicines.map {
                    "${it.medicineId} • ${it.dosage} • ${it.duration} • ${it.instructions}"
                },
                isRedacted = p.isRedacted,
                redactionReason = p.redactionReason
            )
        }
    }

    fun redactPrescription(prescriptionId: String, reason: String) {
        viewModelScope.launch {
            prescriptionRepo.redactPrescription(prescriptionId, reason)
        }
    }

    private suspend fun buildTestBookingItems(bookings: List<TestBooking>): List<HistoryItem.TestBookingRow> {
        return bookings.map { booking ->
            val testName = diagnosticRepo.getDiagnosticTest(booking.testId)?.testName
                ?: "Test #${booking.testId.take(6)}"
            HistoryItem.TestBookingRow(
                bookingId = booking.bookingId,
                testName = testName,
                doctorId = booking.doctorId,
                date = booking.date,
                status = booking.status.name
            )
        }
    }

    private fun buildSurgeryItems(surgeries: List<SurgeryBooking>): List<HistoryItem.SurgeryRow> {
        return surgeries.map { s ->
            HistoryItem.SurgeryRow(
                surgeryId = s.surgeryId,
                surgeryType = s.surgeryType,
                doctorId = s.doctorId,
                date = s.date,
                status = s.status.name
            )
        }
    }

    /* ------------------ state combiner ------------------ */

    private var currentPrescriptions: List<HistoryItem.PrescriptionRow> = emptyList()
    private var currentBookings: List<HistoryItem.TestBookingRow> = emptyList()
    private var currentSurgeries: List<HistoryItem.SurgeryRow> = emptyList()

    private fun publishCombined(
        prescriptionItems: List<HistoryItem.PrescriptionRow>? = null,
        testBookingItems: List<HistoryItem.TestBookingRow>? = null,
        surgeryItems: List<HistoryItem.SurgeryRow>? = null
    ) {
        if (prescriptionItems != null) currentPrescriptions = prescriptionItems
        if (testBookingItems != null) currentBookings = testBookingItems
        if (surgeryItems != null) currentSurgeries = surgeryItems

        val combined: List<HistoryItem> =
            currentPrescriptions + currentBookings + currentSurgeries
        _uiState.value = if (combined.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Loaded(PatientHistory(items = combined))
        }
    }
}

/* ------------------ DTOs ------------------ */

data class PatientHistory(val items: List<HistoryItem>)

sealed class HistoryItem {
    data class PrescriptionRow(
        val prescriptionId: String,
        val doctorName: String,
        val date: String,
        val diagnosis: String,
        val medicines: List<String>,
        val isRedacted: Boolean = false,
        val redactionReason: String = ""
    ) : HistoryItem()

    data class TestBookingRow(
        val bookingId: String,
        val testName: String,
        val doctorId: String,
        val date: String,
        val status: String
    ) : HistoryItem()

    data class SurgeryRow(
        val surgeryId: String,
        val surgeryType: String,
        val doctorId: String,
        val date: String,
        val status: String
    ) : HistoryItem()
}