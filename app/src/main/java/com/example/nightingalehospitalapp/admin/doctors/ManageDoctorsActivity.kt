package com.example.nightingalehospitalapp.admin.doctors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.doctors.ManageDoctorsViewModel

class ManageDoctorsActivity : ComponentActivity() {
    private val viewModel: ManageDoctorsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                val approvedDoctors by viewModel.approvedDoctors.collectAsState()
                val pendingDoctors by viewModel.pendingDoctors.collectAsState()
                val departments by viewModel.departments.collectAsState()
                
                var showDialog by remember { mutableStateOf(false) }
                var editingDoctor by remember { mutableStateOf<Doctor?>(null) }
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                if (showDialog) {
                    DoctorDialog(
                        initialDoctor = editingDoctor,
                        departments = departments,
                        onDismiss = {
                            showDialog = false
                            editingDoctor = null
                        },
                        onSave = { doc ->
                            viewModel.updateDoctor(doc)
                            showDialog = false
                            editingDoctor = null
                        }
                    )
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Manage Doctors") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    ) {
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Approved") })
                            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Pending") })
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (selectedTabIndex == 0) {
                                items(approvedDoctors) { doctor ->
                                    DoctorProfileCard(
                                        doctor = doctor,
                                        departments = departments,
                                        onEdit = {
                                            editingDoctor = doctor
                                            showDialog = true
                                        },
                                        onDelete = { viewModel.removeDoctor(doctor.doctorId) }
                                    )
                                }
                                if (approvedDoctors.isEmpty()) {
                                    item { Text("No approved doctors found", modifier = Modifier.padding(16.dp)) }
                                }
                            } else {
                                items(pendingDoctors) { user ->
                                    PendingDoctorCard(
                                        user = user,
                                        onApprove = { viewModel.approveDoctor(user) },
                                        onReject = { viewModel.rejectDoctor(user) }
                                    )
                                }
                                if (pendingDoctors.isEmpty()) {
                                    item { Text("No pending doctors found", modifier = Modifier.padding(16.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDialog(
    initialDoctor: Doctor?,
    departments: List<Department>,
    onDismiss: () -> Unit,
    onSave: (Doctor) -> Unit
) {
    var specialization by remember { mutableStateOf(initialDoctor?.specialization ?: "") }
    var qualification by remember { mutableStateOf(initialDoctor?.qualification ?: "") }
    var experience by remember { mutableStateOf(initialDoctor?.experienceYears?.toString() ?: "0") }
    
    var selectedDepartmentId by remember { mutableStateOf(initialDoctor?.departmentId ?: "") }
    var departmentExpanded by remember { mutableStateOf(false) }
    
    val selectedDepartmentName = departments.find { it.departmentId == selectedDepartmentId }?.name ?: "Select Department"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = initialDoctor?.userId ?: "",
                    onValueChange = { },
                    label = { Text("User ID") },
                    singleLine = true,
                    enabled = false 
                )
                OutlinedTextField(
                    value = specialization,
                    onValueChange = { specialization = it },
                    label = { Text("Specialization") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification") },
                    singleLine = true
                )
                OutlinedTextField(
                        value = experience,
                        onValueChange = { experience = it },
                        label = { Text("Experience (Years)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
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
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept.name) },
                                onClick = {
                                    selectedDepartmentId = dept.departmentId
                                    departmentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (initialDoctor != null) {
                    val doc = initialDoctor.copy(
                        specialization = specialization,
                        qualification = qualification,
                        experienceYears = experience.toIntOrNull() ?: 0,
                        departmentId = selectedDepartmentId
                    )
                    onSave(doc)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DoctorProfileCard(
    doctor: Doctor,
    departments: List<Department>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val departmentName = departments.find { it.departmentId == doctor.departmentId }?.name ?: "N/A"
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = doctor.name.ifEmpty { "Name not provided" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "Email: ${doctor.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Department: $departmentName", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Specialty: ${doctor.specialization.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = "Qualification: ${doctor.qualification.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Experience: ${doctor.experienceYears} Years", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { /* Schedule Logic */ }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Manage Schedule", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove Profile", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PendingDoctorCard(
    user: User,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "Email: ${user.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onApprove) {
                    Icon(Icons.Filled.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onReject) {
                    Icon(Icons.Filled.Clear, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
