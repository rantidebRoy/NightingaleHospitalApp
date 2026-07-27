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
import androidx.compose.material.icons.filled.Info
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
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingaleTextField
import com.example.nightingalehospitalapp.ui.components.NightingaleTextButton
import com.example.nightingalehospitalapp.ui.components.DoctorCardShimmer
import com.example.nightingalehospitalapp.ui.components.NightingaleEmptyState
import androidx.compose.animation.Crossfade
import com.example.nightingalehospitalapp.viewmodel.admin.doctors.PendingDoctorWithDetails

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
                val snackbarHostState = remember { SnackbarHostState() }
                val errorMessage by viewModel.errorMessage.collectAsState()

                LaunchedEffect(errorMessage) {
                    errorMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearError()
                    }
                }

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
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        val isLoading by viewModel.isLoading.collectAsState()

                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Approved") })
                            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Pending") })
                        }
                        
                        Crossfade(targetState = selectedTabIndex, label = "tabTransition") { tabIndex ->
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (isLoading) {
                                    items(3) {
                                        DoctorCardShimmer()
                                    }
                                } else if (tabIndex == 0) {
                                    if (approvedDoctors.isEmpty()) {
                                        item { 
                                            NightingaleEmptyState(
                                                title = "No Approved Doctors",
                                                message = "There are currently no approved doctors in the system.",
                                                icon = Icons.Filled.Info
                                            )
                                        }
                                    } else {
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
                                    }
                                } else {
                                    if (pendingDoctors.isEmpty()) {
                                        item { 
                                            NightingaleEmptyState(
                                                title = "No Pending Doctors",
                                                message = "All doctors have been approved or rejected.",
                                                icon = Icons.Filled.Check
                                            )
                                        }
                                    } else {
                                        items(pendingDoctors) { pending ->
                                            PendingDoctorCard(
                                                pending = pending,
                                                departments = departments,
                                                onApprove = { viewModel.approveDoctor(pending.user) },
                                                onReject = { viewModel.rejectDoctor(pending.user) }
                                            )
                                        }
                                    }
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
    var departmentSearchQuery by remember { mutableStateOf(departments.find { it.departmentId == initialDoctor?.departmentId }?.name ?: "") }
    var departmentExpanded by remember { mutableStateOf(false) }
    val filteredDepartments = departments.filter { it.name.contains(departmentSearchQuery, ignoreCase = true) || it.departmentId.contains(departmentSearchQuery, ignoreCase = true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = initialDoctor?.userId ?: "",
                    onValueChange = { },
                    label = { Text("User ID") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                NightingaleTextField(
                    value = specialization,
                    onValueChange = { specialization = it },
                    label = "Specialization"
                )
                NightingaleTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = "Qualification"
                )
                NightingaleTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = "Experience (Years)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                    
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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false }
                    ) {
                        filteredDepartments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept.name) },
                                onClick = {
                                    selectedDepartmentId = dept.departmentId
                                    departmentSearchQuery = dept.name
                                    departmentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            NightingaleTextButton(
                text = "Save",
                onClick = {
                    if (initialDoctor != null) {
                        val doc = initialDoctor.copy(
                            specialization = specialization,
                            qualification = qualification,
                            experienceYears = experience.toIntOrNull() ?: 0,
                            departmentId = selectedDepartmentId
                        )
                        onSave(doc)
                    }
                }
            )
        },
        dismissButton = {
            NightingaleTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
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
    NightingaleElevatedCard {
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
@Composable
fun PendingDoctorCard(
    pending: PendingDoctorWithDetails,
    departments: List<Department>,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val doctor = pending.doctor
    val departmentName = departments.find { it.departmentId == doctor?.departmentId }?.name ?: "N/A"

    NightingaleElevatedCard {
        Text(text = pending.user.name.ifEmpty { "Name not provided" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (pending.user.displayId.isNotEmpty()) {
            Text(text = "ID: ${pending.user.displayId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(text = "Email: ${pending.user.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Department: $departmentName", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Specialty: ${doctor?.specialization?.ifEmpty { "N/A" } ?: "N/A"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = "Qualification: ${doctor?.qualification?.ifEmpty { "N/A" } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Experience: ${doctor?.experienceYears ?: 0} Years", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onApprove) {
                Icon(Icons.Filled.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onReject) {
                Icon(Icons.Filled.Clear, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
