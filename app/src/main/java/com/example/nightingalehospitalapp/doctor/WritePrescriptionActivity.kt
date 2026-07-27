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
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.components.NightingaleTextField
import com.example.nightingalehospitalapp.ui.components.NightingaleUserScaffold
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
    var date by rememberSaveable { mutableStateOf(todayString()) }
    var saving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    NightingaleUserScaffold(
        title = "Prescription — $patientName",
        showBottomBar = false,
        onNavigateBack = {
            (context as? ComponentActivity)?.finish()
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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

            NightingaleTextField(
                value = date,
                onValueChange = { date = it },
                label = "Date (YYYY-MM-DD)",
                modifier = Modifier.fillMaxWidth()
            )

            NightingaleTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = "Notes / Diagnosis",
                modifier = Modifier.fillMaxWidth()
            )

            NightingalePrimaryButton(
                text = "Save Prescription",
                onClick = {
                    if (saving) return@NightingalePrimaryButton
                    if (patientId.isBlank() || doctorId.isBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Missing patient or doctor identity") }
                        return@NightingalePrimaryButton
                    }
                    if (diagnosis.isBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Notes / Diagnosis is required") }
                        return@NightingalePrimaryButton
                    }
                    saving = true
                    val prescription = Prescription(
                        prescriptionId = "",
                        appointmentId = "",
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
                modifier = Modifier.fillMaxWidth()
            )
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