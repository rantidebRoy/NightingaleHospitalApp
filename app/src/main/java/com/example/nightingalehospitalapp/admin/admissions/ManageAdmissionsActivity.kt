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
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.components.NightingaleEmptyState
import com.example.nightingalehospitalapp.ui.components.NightingaleListShimmer
import androidx.compose.material.icons.filled.Info

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(4) { NightingaleListShimmer() }
                }
            } else if (admittedPatients.isEmpty()) {
                NightingaleEmptyState(
                    title = "No Admitted Patients",
                    message = "There are currently no patients admitted to the hospital.",
                    icon = Icons.Filled.Info,
                    modifier = Modifier.align(Alignment.Center)
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
                                        // Ideally use a coroutine to show snackbar here, but keeping toast for success/failure callback for simplicity, or we can emit to a new flow. Let's just use toast for the callback.
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

    NightingaleElevatedCard {
        Text(text = patientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Doctor: $doctorName")
        Text(text = "Room/Bed: $roomNumber")
        Text(text = "Admitted On: $dateStr")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onDischarge,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Discharge")
            }
        }
    }
}
