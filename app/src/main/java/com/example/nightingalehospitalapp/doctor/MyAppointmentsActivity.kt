package com.example.nightingalehospitalapp.doctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nightingalehospitalapp.models.appointment.Appointment
import com.example.nightingalehospitalapp.models.enums.AppointmentStatus
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.AppointmentViewModel

class MyAppointmentsActivity : ComponentActivity() {

    private val viewModel: AppointmentViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID) ?: ""
        viewModel.observeAppointmentsForDoctor(doctorId)

        setContent {
            NightingaleHospitalAppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("My Appointments") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                ) { padding ->
                    AppointmentsContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_DOCTOR_ID = "extra_doctor_id"
    }
}

@Composable
private fun AppointmentsContent(
    modifier: Modifier = Modifier,
    viewModel: AppointmentViewModel
) {
    val state by viewModel.appointments.collectAsState()
    var selected by remember { mutableStateOf<Appointment?>(null) }

    Column(modifier = modifier) {
        when (val s = state) {
            is AppointmentViewModel.UiState.Idle,
            is AppointmentViewModel.UiState.Loading -> CenteredLoader()

            is AppointmentViewModel.UiState.Error -> CenteredMessage("Error: ${s.message}")

            is AppointmentViewModel.UiState.Loaded -> {
                if (s.appointments.isEmpty()) {
                    CenteredMessage("No appointments yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                    ) {
                        items(s.appointments, key = { it.appointmentId }) { appt ->
                            AppointmentCard(
                                appointment = appt,
                                onClick = { selected = appt }
                            )
                        }
                    }
                }
            }
        }
    }

    val current = selected
    if (current != null) {
        AppointmentDetailDialog(
            appointment = current,
            onDismiss = { selected = null },
            onUpdate = { newStatus ->
                viewModel.updateStatus(current.appointmentId, newStatus)
                selected = null
            }
        )
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PatientAvatar(name = appointment.patientName.ifBlank { appointment.patientId })
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = appointment.patientName.ifBlank { "Unknown patient" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "ID: ${appointment.patientId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = appointment.status)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBlock(label = "Date", value = appointment.date.ifBlank { "—" })
                InfoBlock(label = "Time", value = appointment.time.ifBlank { "—" })
                if (appointment.patientAge > 0) {
                    InfoBlock(
                        label = "Age/Gender",
                        value = "${appointment.patientAge} / ${appointment.patientGender.ifBlank { "—" }}"
                    )
                }
            }
            if (appointment.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = appointment.notes,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PatientAvatar(name: String) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusChip(status: AppointmentStatus) {
    val (bg, fg) = when (status) {
        AppointmentStatus.PENDING -> Color(0xFFFFE0B2) to Color(0xFF7C4A00)
        AppointmentStatus.CONFIRMED -> Color(0xFFBBDEFB) to Color(0xFF0D47A1)
        AppointmentStatus.COMPLETED -> Color(0xFFC8E6C9) to Color(0xFF1B5E20)
        AppointmentStatus.CANCELLED -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
    }
    AssistChip(
        onClick = {},
        label = { Text(status.name, color = fg, fontWeight = FontWeight.SemiBold) },
        colors = AssistChipDefaults.assistChipColors(containerColor = bg)
    )
}

@Composable
private fun AppointmentDetailDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onUpdate: (AppointmentStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appointment.patientName.ifBlank { "Patient" }) },
        text = {
            Column {
                Text("Patient ID: ${appointment.patientId}")
                if (appointment.patientAge > 0) {
                    Text("Age: ${appointment.patientAge}  |  Gender: ${appointment.patientGender}")
                }
                Spacer(Modifier.height(8.dp))
                Text("Date: ${appointment.date}  |  Time: ${appointment.time}")
                Spacer(Modifier.height(8.dp))
                Text("Status: ${appointment.status}")
                if (appointment.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Notes: ${appointment.notes}")
                }
                Spacer(Modifier.height(12.dp))
                Text("Update status:", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Column {
                StatusAction("Confirm", AppointmentStatus.CONFIRMED, onUpdate)
                StatusAction("Mark Completed", AppointmentStatus.COMPLETED, onUpdate)
                StatusAction("Cancel", AppointmentStatus.CANCELLED, onUpdate)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun StatusAction(
    label: String,
    status: AppointmentStatus,
    onUpdate: (AppointmentStatus) -> Unit
) {
    TextButton(onClick = { onUpdate(status) }) {
        Text(label)
    }
}

@Composable
private fun CenteredLoader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}