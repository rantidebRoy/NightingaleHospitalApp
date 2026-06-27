package com.example.nightingalehospitalapp.admin.admissions

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.admissions.CreateAdmissionViewModel

class CreateAdmissionActivity : ComponentActivity() {

    private val viewModel: CreateAdmissionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NightingaleHospitalAppTheme {
                CreateAdmissionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onSuccess = {
                        Toast.makeText(this, "Admission created successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdmissionScreen(
    viewModel: CreateAdmissionViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val patients by viewModel.patients.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val beds by viewModel.beds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var selectedPatientId by remember { mutableStateOf("") }
    var selectedPatientName by remember { mutableStateOf("Select Patient") }
    var patientExpanded by remember { mutableStateOf(false) }

    var selectedDoctorId by remember { mutableStateOf("") }
    var selectedDoctorName by remember { mutableStateOf("Select Doctor") }
    var doctorExpanded by remember { mutableStateOf(false) }

    var selectedDepartmentId by remember { mutableStateOf("") }
    var selectedDepartmentName by remember { mutableStateOf("Select Department") }
    var departmentExpanded by remember { mutableStateOf(false) }

    var selectedBedId by remember { mutableStateOf("") }
    var selectedBedRoom by remember { mutableStateOf("Select Bed") }
    var bedExpanded by remember { mutableStateOf(false) }

    var reason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Admission") },
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
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Patient Dropdown
                ExposedDropdownMenuBox(
                    expanded = patientExpanded,
                    onExpandedChange = { patientExpanded = !patientExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPatientName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Patient") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = patientExpanded,
                        onDismissRequest = { patientExpanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text(patient.name) },
                                onClick = {
                                    selectedPatientId = patient.userId
                                    selectedPatientName = patient.name
                                    patientExpanded = false
                                }
                            )
                        }
                    }
                }

                // Doctor Dropdown
                ExposedDropdownMenuBox(
                    expanded = doctorExpanded,
                    onExpandedChange = { doctorExpanded = !doctorExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDoctorName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Doctor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false }
                    ) {
                        doctors.forEach { doctor ->
                            DropdownMenuItem(
                                text = { Text(doctor.name) },
                                onClick = {
                                    selectedDoctorId = doctor.doctorId
                                    selectedDoctorName = doctor.name
                                    doctorExpanded = false
                                }
                            )
                        }
                    }
                }

                // Department Dropdown
                ExposedDropdownMenuBox(
                    expanded = departmentExpanded,
                    onExpandedChange = { departmentExpanded = !departmentExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDepartmentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        departments.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    selectedDepartmentId = department.departmentId
                                    selectedDepartmentName = department.name
                                    departmentExpanded = false
                                }
                            )
                        }
                    }
                }

                // Bed Dropdown
                ExposedDropdownMenuBox(
                    expanded = bedExpanded,
                    onExpandedChange = { bedExpanded = !bedExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBedRoom,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bed") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bedExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = bedExpanded,
                        onDismissRequest = { bedExpanded = false }
                    ) {
                        if (beds.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No beds available") },
                                onClick = { bedExpanded = false }
                            )
                        } else {
                            beds.forEach { bed ->
                                DropdownMenuItem(
                                    text = { Text("Room ${bed.roomNumber} - ${bed.ward}") },
                                    onClick = {
                                        selectedBedId = bed.bedId
                                        selectedBedRoom = "Room ${bed.roomNumber}"
                                        bedExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Reason TextField
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Admission") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.submitAdmission(
                            selectedPatientId,
                            selectedDoctorId,
                            selectedDepartmentId,
                            selectedBedId,
                            reason
                        ) { success, msg ->
                            if (success) {
                                onSuccess()
                            } else {
                                Toast.makeText(context, msg ?: "Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Admit Patient")
                }
            }
        }
    }
}
