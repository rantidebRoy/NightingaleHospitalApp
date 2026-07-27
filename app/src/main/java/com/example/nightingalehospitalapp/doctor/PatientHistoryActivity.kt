package com.example.nightingalehospitalapp.doctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingaleEmptyState
import com.example.nightingalehospitalapp.ui.components.NightingaleListShimmer
import com.example.nightingalehospitalapp.ui.components.NightingaleUserScaffold
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.nightingalehospitalapp.viewmodel.HistoryItem
import com.example.nightingalehospitalapp.viewmodel.PatientHistoryViewModel

class PatientHistoryActivity : ComponentActivity() {

    private val viewModel: PatientHistoryViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val patientId = intent.getStringExtra(EXTRA_PATIENT_ID).orEmpty()
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Patient"
        val canRedact = intent.getBooleanExtra(EXTRA_CAN_REDACT, false)

        viewModel.observe(patientId)

        setContent {
            NightingaleHospitalAppTheme {
                NightingaleUserScaffold(
                    title = "Patient History — $patientName",
                    showBottomBar = false,
                    onNavigateBack = { finish() }
                ) { padding ->
                    val state by viewModel.uiState.collectAsState()
                    HistoryScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        patientName = patientName,
                        patientId = patientId,
                        canRedact = canRedact,
                        state = state,
                        onRedactPrescription = { id, reason ->
                            viewModel.redactPrescription(id, reason)
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT_ID = "extra_patient_id"
        const val EXTRA_PATIENT_NAME = "extra_patient_name"
        const val EXTRA_CAN_REDACT = "extra_can_redact"
    }
}

@Composable
private fun HistoryScreen(
    modifier: Modifier = Modifier,
    patientName: String,
    patientId: String,
    canRedact: Boolean,
    state: PatientHistoryViewModel.UiState,
    onRedactPrescription: (String, String) -> Unit
) {
    when (state) {
        PatientHistoryViewModel.UiState.Idle,
        PatientHistoryViewModel.UiState.Loading -> {
            LazyColumn(
                modifier = modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(3) { NightingaleListShimmer() }
            }
        }
        PatientHistoryViewModel.UiState.Empty -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                NightingaleEmptyState(
                    title = "No medical history recorded yet",
                    message = "Prescriptions, tests and surgeries will appear here",
                    icon = Icons.Filled.Info
                )
            }
        }
        is PatientHistoryViewModel.UiState.Error -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "Failed to load history: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is PatientHistoryViewModel.UiState.Loaded -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { PatientHeader(patientName, patientId) }
                item { SectionTitle("Prescriptions") }
                val prescriptions = state.history.items
                    .filterIsInstance<HistoryItem.PrescriptionRow>()
                if (prescriptions.isEmpty()) {
                    item { EmptySection("No prescriptions yet") }
                } else {
                    items(prescriptions, key = { "rx-${it.prescriptionId}" }) { rx ->
                        PrescriptionRow(
                            row = rx,
                            canRedact = canRedact,
                            onRedact = onRedactPrescription
                        )
                    }
                }

                item { SectionTitle("Diagnostic Tests") }
                val bookings = state.history.items
                    .filterIsInstance<HistoryItem.TestBookingRow>()
                if (bookings.isEmpty()) {
                    item { EmptySection("No diagnostic tests booked") }
                } else {
                    items(bookings, key = { "test-${it.bookingId}" }) {
                        TestBookingRow(it)
                    }
                }

                item { SectionTitle("Surgeries") }
                val surgeries = state.history.items
                    .filterIsInstance<HistoryItem.SurgeryRow>()
                if (surgeries.isEmpty()) {
                    item { EmptySection("No surgeries recorded") }
                } else {
                    items(surgeries, key = { "surg-${it.surgeryId}" }) {
                        SurgeryRow(it)
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun PatientHeader(name: String, id: String) {
    NightingaleElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "ID: $id",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun EmptySection(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PrescriptionRow(
    row: HistoryItem.PrescriptionRow,
    canRedact: Boolean,
    onRedact: (prescriptionId: String, reason: String) -> Unit
) {
    var showRedactDialog by remember { mutableStateOf(false) }
    var reasonInput by remember { mutableStateOf("") }

    NightingaleElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Medication,
                    contentDescription = null,
                    tint = if (row.isRedacted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prescription by ${row.doctorName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (row.isRedacted) {
                        Text(
                            text = "[REDACTED / CANCELLED]",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    text = row.date,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (row.diagnosis.isNotBlank()) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = row.diagnosis,
                    fontSize = 13.sp,
                    style = if (row.isRedacted) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle(),
                    color = if (row.isRedacted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            if (row.isRedacted && row.redactionReason.isNotBlank()) {
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "Reason: ${row.redactionReason}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (row.medicines.isNotEmpty()) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Medicines",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                row.medicines.forEach { line ->
                    Text(
                        text = "• $line",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!row.isRedacted && canRedact) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = { showRedactDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Redact / Cancel", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }

    if (showRedactDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRedactDialog = false },
            title = { Text("Redact Prescription") },
            text = {
                Column {
                    Text("This will mark the prescription as Redacted / Cancelled. It will remain in medical history for audit purposes.")
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Reason for Redaction (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onRedact(row.prescriptionId, reasonInput.trim().ifBlank { "Redacted by Doctor" })
                        showRedactDialog = false
                    }
                ) {
                    Text("Redact", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRedactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TestBookingRow(row: HistoryItem.TestBookingRow) {
    NightingaleElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = row.testName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.date,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Status: ${row.status}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SurgeryRow(row: HistoryItem.SurgeryRow) {
    NightingaleElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = row.surgeryType,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.date,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Status: ${row.status}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}