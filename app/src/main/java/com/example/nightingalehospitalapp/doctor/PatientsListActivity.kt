package com.example.nightingalehospitalapp.doctor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

class PatientsListActivity : ComponentActivity() {

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
                            title = { Text("My Patients") },
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
                    PatientsContent(
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

private data class PatientRow(
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val patientGender: String,
    val nextAppointment: Appointment,
    val totalAppointments: Int
)

private fun aggregatePatients(appointments: List<Appointment>): List<PatientRow> {
    return appointments
        .groupBy { it.patientId.ifBlank { it.patientName } }
        .map { (_, group) ->
            // Pick the next upcoming appointment; if none, the most recent.
            val sorted = group.sortedWith(compareBy({ it.date }, { it.time }))
            val next = sorted.firstOrNull { it.status != AppointmentStatus.COMPLETED }
                ?: sorted.last()
            PatientRow(
                patientId = next.patientId,
                patientName = next.patientName.ifBlank { next.patientId },
                patientAge = next.patientAge,
                patientGender = next.patientGender,
                nextAppointment = next,
                totalAppointments = group.size
            )
        }
        .sortedWith(compareBy({ it.nextAppointment.date }, { it.nextAppointment.time }))
}

@Composable
private fun PatientsContent(
    modifier: Modifier = Modifier,
    viewModel: AppointmentViewModel
) {
    val state by viewModel.appointments.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    when (val s = state) {
        AppointmentViewModel.UiState.Idle,
        AppointmentViewModel.UiState.Loading -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is AppointmentViewModel.UiState.Error -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Error: ${s.message}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        is AppointmentViewModel.UiState.Loaded -> {
            val rows = remember(s.appointments) { aggregatePatients(s.appointments) }
            if (rows.isEmpty()) {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No patients yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rows, key = { it.patientId }) { row ->
                        PatientRowCard(row = row) {
                            val intent = Intent(context, PatientDetailActivity::class.java)
                                .putExtra(PatientDetailActivity.EXTRA_PATIENT_ID, row.patientId)
                                .putExtra(PatientDetailActivity.EXTRA_PATIENT_NAME, row.patientName)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientRowCard(
    row: PatientRow,
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PatientAvatar(name = row.patientName)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.patientName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (row.patientAge > 0)
                        "${row.patientAge} yrs  |  ${row.patientGender.ifBlank { "—" }}"
                    else "ID: ${row.patientId}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "${row.nextAppointment.date}  •  ${row.nextAppointment.time}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (row.totalAppointments > 1) {
                    Text(
                        text = "${row.totalAppointments} appointments",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusChip(status = row.nextAppointment.status)
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