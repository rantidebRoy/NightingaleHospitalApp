package com.example.nightingalehospitalapp.doctor

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nightingalehospitalapp.models.prescription.Prescription
import com.example.nightingalehospitalapp.repository.prescription.PrescriptionRepository
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.google.firebase.auth.FirebaseAuth

class WritePrescriptionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val patientId = intent.getStringExtra(EXTRA_PATIENT_ID).orEmpty()
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Patient"
        val doctorId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        setContent {
            NightingaleHospitalAppTheme {
                WritePrescriptionScreen(
                    patientId = patientId,
                    patientName = patientName,
                    doctorId = doctorId
                )
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT_ID = "extra_patient_id"
        const val EXTRA_PATIENT_NAME = "extra_patient_name"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePrescriptionScreen(
    patientId: String,
    patientName: String,
    doctorId: String,
    repository: PrescriptionRepository = remember { PrescriptionRepository() }
) {
    val context = LocalContext.current
    var diagnosis by rememberSaveable { mutableStateOf("") }
    var appointmentId by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(todayString()) }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prescription — $patientName") },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Write a new prescription",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = appointmentId,
                onValueChange = { appointmentId = it },
                label = { Text("Appointment ID (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("Diagnosis / Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 4
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (saving) return@Button
                    if (patientId.isBlank() || doctorId.isBlank()) {
                        Toast.makeText(context, "Missing patient or doctor identity", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (diagnosis.isBlank()) {
                        Toast.makeText(context, "Diagnosis is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    saving = true
                    val prescription = Prescription(
                        prescriptionId = "",
                        appointmentId = appointmentId.trim(),
                        doctorId = doctorId,
                        patientId = patientId,
                        diagnosis = diagnosis.trim(),
                        date = date.trim()
                    )
                    repository.addPrescription(prescription)
                    Toast.makeText(context, "Prescription saved", Toast.LENGTH_SHORT).show()
                    (context as? ComponentActivity)?.finish()
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Save Prescription")
            }
        }
    }
}

private fun todayString(): String {
    val now = java.util.Calendar.getInstance()
    val y = now.get(java.util.Calendar.YEAR)
    val m = now.get(java.util.Calendar.MONTH) + 1
    val d = now.get(java.util.Calendar.DAY_OF_MONTH)
    return "%04d-%02d-%02d".format(y, m, d)
}