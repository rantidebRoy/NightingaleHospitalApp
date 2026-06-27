package com.example.nightingalehospitalapp.admin.admissions

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.admissions.ManageAdmissionsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageAdmissionsActivity : ComponentActivity() {

    private val viewModel: ManageAdmissionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NightingaleHospitalAppTheme {
                ManageAdmissionsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToCreate = {
                        startActivity(Intent(this, CreateAdmissionActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchAdmissions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAdmissionsScreen(
    viewModel: ManageAdmissionsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val admittedPatients by viewModel.admittedPatients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Admissions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Admit Patient")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (admittedPatients.isEmpty()) {
                Text(
                    text = "No admitted patients.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(admittedPatients) { patient ->
                        AdmittedPatientCard(
                            patientName = patient.patientName,
                            doctorName = patient.doctorName,
                            roomNumber = patient.bedRoom,
                            admissionDate = patient.admissionDate,
                            onDischarge = {
                                viewModel.dischargePatient(patient.admissionId, patient.bedId) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Patient Discharged", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdmittedPatientCard(
    patientName: String,
    doctorName: String,
    roomNumber: String,
    admissionDate: Long,
    onDischarge: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(admissionDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = patientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Doctor: $doctorName")
            Text(text = "Room/Bed: $roomNumber")
            Text(text = "Admitted On: $dateStr")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDischarge,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Discharge")
            }
        }
    }
}
