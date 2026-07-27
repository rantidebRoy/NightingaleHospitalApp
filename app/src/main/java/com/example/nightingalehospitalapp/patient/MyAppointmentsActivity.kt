package com.example.nightingalehospitalapp.patient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingaleEmptyState
import com.example.nightingalehospitalapp.ui.components.NightingaleListShimmer
import com.example.nightingalehospitalapp.ui.components.NightingaleUserScaffold
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.AppointmentViewModel
import com.google.firebase.auth.FirebaseAuth

class MyAppointmentsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_DOCTOR_ID = "EXTRA_DOCTOR_ID"
    }

    private val appointmentViewModel: AppointmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID)
        
        setContent {
            NightingaleHospitalAppTheme {
                MyAppointmentsScreen(appointmentViewModel, doctorId) { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(viewModel: AppointmentViewModel, doctorId: String?, onBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (doctorId != null) {
            viewModel.observeAppointmentsForDoctor(doctorId)
        } else if (currentUser != null) {
            viewModel.observeAppointmentsForPatient(currentUser.uid)
        }
    }

    val uiState by viewModel.appointments.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    NightingaleUserScaffold(
        title = "My Appointments",
        showBottomBar = false,
        onNavigateBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is AppointmentViewModel.UiState.Loading -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(4) { NightingaleListShimmer() }
                    }
                }
                is AppointmentViewModel.UiState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is AppointmentViewModel.UiState.Loaded -> {
                    if (state.appointments.isEmpty()) {
                        NightingaleEmptyState(
                            title = "No Appointments",
                            message = "You have no appointments booked.",
                            icon = Icons.Filled.Info
                        )
                    } else {
                        LazyColumn {
                            items(state.appointments) { appointment ->
                                NightingaleElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                            text = if (appointment.doctorName.isNotBlank()) "Doctor: ${appointment.doctorName}" else "Doctor ID: ${appointment.doctorId}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (appointment.doctorDisplayId.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Doctor ID: ${appointment.doctorDisplayId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (appointment.doctorSpecialization.isNotBlank() || appointment.doctorDepartment.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val deptInfo = listOf(appointment.doctorSpecialization, appointment.doctorDepartment).filter { it.isNotBlank() }.joinToString(" • ")
                                            Text(
                                                text = deptInfo,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Date: ${appointment.date} at ${appointment.time}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Status: ${appointment.status.name}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        if (appointment.notes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Notes: ${appointment.notes}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (appointment.status != com.example.nightingalehospitalapp.models.enums.AppointmentStatus.CANCELLED && appointment.status != com.example.nightingalehospitalapp.models.enums.AppointmentStatus.COMPLETED) {
                                            Button(
                                                onClick = {
                                                    viewModel.cancelAppointmentFromSlot(
                                                        slotId = appointment.appointmentId,
                                                        patientId = appointment.patientId,
                                                        doctorId = appointment.doctorId,
                                                        date = appointment.date,
                                                        time = appointment.time
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Cancel Appointment")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
