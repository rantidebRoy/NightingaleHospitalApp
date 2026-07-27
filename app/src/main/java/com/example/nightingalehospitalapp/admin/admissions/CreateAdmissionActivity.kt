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
    var patientSearchQuery by remember { mutableStateOf("") }
    var patientExpanded by remember { mutableStateOf(false) }
    val filteredPatients = patients.filter { it.name.contains(patientSearchQuery, ignoreCase = true) || it.userId.contains(patientSearchQuery, ignoreCase = true) || it.displayId.contains(patientSearchQuery, ignoreCase = true) }

    var selectedDoctorId by remember { mutableStateOf("") }
    var doctorSearchQuery by remember { mutableStateOf("") }
    var doctorExpanded by remember { mutableStateOf(false) }
    val filteredDoctors = doctors.filter { it.name.contains(doctorSearchQuery, ignoreCase = true) || it.doctorId.contains(doctorSearchQuery, ignoreCase = true) || it.displayId.contains(doctorSearchQuery, ignoreCase = true) }

    var selectedDepartmentId by remember { mutableStateOf("") }
    var departmentSearchQuery by remember { mutableStateOf("") }
    var departmentExpanded by remember { mutableStateOf(false) }
    val filteredDepartments = departments.filter { it.name.contains(departmentSearchQuery, ignoreCase = true) || it.departmentId.contains(departmentSearchQuery, ignoreCase = true) }

    var selectedBedId by remember { mutableStateOf("") }
    var bedSearchQuery by remember { mutableStateOf("") }
    var bedExpanded by remember { mutableStateOf(false) }
    val filteredBeds = beds.filter { it.roomNumber.contains(bedSearchQuery, ignoreCase = true) || it.bedNumber.contains(bedSearchQuery, ignoreCase = true) || it.ward.contains(bedSearchQuery, ignoreCase = true) || it.bedId.contains(bedSearchQuery, ignoreCase = true) }

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
                        value = patientSearchQuery,
                        onValueChange = {
                            patientSearchQuery = it
                            patientExpanded = true
                            selectedPatientId = ""
                        },
                        readOnly = false,
                        label = { Text("Patient") },
                        placeholder = { Text("Select or search patient") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = patientExpanded,
                        onDismissRequest = { patientExpanded = false }
                    ) {
                        filteredPatients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text("${patient.name} (${patient.displayId.ifEmpty { patient.userId }})") },
                                onClick = {
                                    selectedPatientId = patient.userId
                                    patientSearchQuery = "${patient.name} (${patient.displayId.ifEmpty { patient.userId }})"
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
                        value = doctorSearchQuery,
                        onValueChange = {
                            doctorSearchQuery = it
                            doctorExpanded = true
                            selectedDoctorId = ""
                        },
                        readOnly = false,
                        label = { Text("Doctor") },
                        placeholder = { Text("Select or search doctor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false }
                    ) {
                        filteredDoctors.forEach { doctor ->
                            DropdownMenuItem(
                                text = { Text("${doctor.name} (${doctor.displayId.ifEmpty { doctor.doctorId }})") },
                                onClick = {
                                    selectedDoctorId = doctor.doctorId
                                    doctorSearchQuery = "${doctor.name} (${doctor.displayId.ifEmpty { doctor.doctorId }})"
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
                        value = departmentSearchQuery,
                        onValueChange = {
                            departmentSearchQuery = it
                            departmentExpanded = true
                            selectedDepartmentId = ""
                        },
                        readOnly = false,
                        label = { Text("Department") },
                        placeholder = { Text("Select or search department") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        filteredDepartments.forEach { department ->
                            DropdownMenuItem(
                                text = { Text(department.name) },
                                onClick = {
                                    selectedDepartmentId = department.departmentId
                                    departmentSearchQuery = department.name
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
                        value = bedSearchQuery,
                        onValueChange = {
                            bedSearchQuery = it
                            bedExpanded = true
                            selectedBedId = ""
                        },
                        readOnly = false,
                        label = { Text("Bed") },
                        placeholder = { Text("Select or search bed") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bedExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = bedExpanded,
                        onDismissRequest = { bedExpanded = false }
                    ) {
                        if (filteredBeds.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No beds found") },
                                onClick = { bedExpanded = false }
                            )
                        } else {
                            filteredBeds.forEach { bed ->
                                val label = if (bed.bedNumber.isNotBlank()) "${bed.bedNumber} (Room ${bed.roomNumber} - ${bed.ward})" else "Room ${bed.roomNumber} - ${bed.ward}"
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedBedId = bed.bedId
                                        bedSearchQuery = label
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
