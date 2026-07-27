package com.example.nightingalehospitalapp.admin.resources

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.models.diagnostic.DiagnosticTest
import com.example.nightingalehospitalapp.models.hospital.Bed
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.hospital.OperationTheatre
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingaleEmptyState
import com.example.nightingalehospitalapp.ui.components.NightingaleListShimmer
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.components.NightingaleTextButton
import com.example.nightingalehospitalapp.ui.components.NightingaleTextField
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.resources.ManageResourcesViewModel

class ManageResourcesActivity : ComponentActivity() {
    private val viewModel: ManageResourcesViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf("Beds", "Operation Theatres", "Tests", "Departments")
                
                val beds by viewModel.beds.collectAsState()
                val theatres by viewModel.theatres.collectAsState()
                val tests by viewModel.tests.collectAsState()
                val departments by viewModel.departments.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                
                var showAddDialog by remember { mutableStateOf(false) }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearError()
                    }
                }

                if (showAddDialog) {
                    when (selectedTabIndex) {
                        0 -> AddBedDialog(onDismiss = { showAddDialog = false }, onSave = { viewModel.addBed(it); showAddDialog = false })
                        1 -> AddOTDialog(onDismiss = { showAddDialog = false }, onSave = { viewModel.addOperationTheatre(it); showAddDialog = false })
                        2 -> AddTestDialog(onDismiss = { showAddDialog = false }, onSave = { viewModel.addDiagnosticTest(it); showAddDialog = false })
                        3 -> AddDepartmentDialog(onDismiss = { showAddDialog = false }, onSave = { viewModel.addDepartment(it); showAddDialog = false })
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Manage Resources") },
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
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                        
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            if (isLoading) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(4) { NightingaleListShimmer() }
                                }
                            } else {
                                when (selectedTabIndex) {
                                    0 -> BedList(beds = beds, onDelete = { viewModel.removeBed(it) })
                                    1 -> OTList(theatres = theatres, onDelete = { viewModel.removeOperationTheatre(it) })
                                    2 -> TestList(tests = tests, onDelete = { viewModel.removeDiagnosticTest(it) })
                                    3 -> DepartmentList(departments = departments, onDelete = { viewModel.removeDepartment(it) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BedList(beds: List<Bed>, onDelete: (String) -> Unit) {
    if (beds.isEmpty()) {
        NightingaleEmptyState(title = "No Beds", message = "Add beds to manage patient rooms.", icon = Icons.Filled.Info)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(beds) { bed ->
                NightingaleElevatedCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                text = if (bed.bedNumber.isNotBlank()) "${bed.bedNumber} (Room ${bed.roomNumber})" else "Room: ${bed.roomNumber}",
                                fontWeight = FontWeight.Bold
                            )
                            Text("Ward: ${bed.ward}")
                            Text("Status: ${bed.status.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { onDelete(bed.bedId) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBedDialog(onDismiss: () -> Unit, onSave: (Bed) -> Unit) {
    var bedNumber by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NightingaleTextField(value = bedNumber, onValueChange = { bedNumber = it }, label = "Bed Number / Identifier (e.g. Bed A, Bed 1)")
                NightingaleTextField(value = room, onValueChange = { room = it }, label = "Room Number")
                NightingaleTextField(value = ward, onValueChange = { ward = it }, label = "Ward")
            }
        },
        confirmButton = { NightingalePrimaryButton(onClick = { onSave(Bed(bedNumber = bedNumber.trim(), roomNumber = room.trim(), ward = ward.trim())) }, text = "Save") },
        dismissButton = { NightingaleTextButton(onClick = onDismiss, text = "Cancel") }
    )
}

@Composable
fun OTList(theatres: List<OperationTheatre>, onDelete: (String) -> Unit) {
    if (theatres.isEmpty()) {
        NightingaleEmptyState(title = "No Operation Theatres", message = "Add operation theatres to schedule surgeries.", icon = Icons.Filled.Info)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(theatres) { ot ->
                NightingaleElevatedCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Room: ${ot.roomNumber}", fontWeight = FontWeight.Bold)
                            Text("Floor: ${ot.floor}")
                        }
                        IconButton(onClick = { onDelete(ot.otId) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun AddOTDialog(onDismiss: () -> Unit, onSave: (OperationTheatre) -> Unit) {
    var room by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Operation Theatre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NightingaleTextField(value = room, onValueChange = { room = it }, label = "Room Number")
                NightingaleTextField(value = floor, onValueChange = { floor = it }, label = "Floor")
            }
        },
        confirmButton = { NightingalePrimaryButton(onClick = { onSave(OperationTheatre(roomNumber = room, floor = floor)) }, text = "Save") },
        dismissButton = { NightingaleTextButton(onClick = onDismiss, text = "Cancel") }
    )
}

@Composable
fun TestList(tests: List<DiagnosticTest>, onDelete: (String) -> Unit) {
    if (tests.isEmpty()) {
        NightingaleEmptyState(title = "No Tests", message = "Add diagnostic tests to the catalog.", icon = Icons.Filled.Info)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tests) { test ->
                NightingaleElevatedCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(test.testName, fontWeight = FontWeight.Bold)
                            Text("Price: $${test.price}")
                        }
                        IconButton(onClick = { onDelete(test.testId) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTestDialog(onDismiss: () -> Unit, onSave: (DiagnosticTest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Test") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NightingaleTextField(value = name, onValueChange = { name = it }, label = "Test Name")
                NightingaleTextField(value = price, onValueChange = { price = it }, label = "Price")
            }
        },
        confirmButton = { NightingalePrimaryButton(onClick = { onSave(DiagnosticTest(testName = name, price = price.toDoubleOrNull() ?: 0.0)) }, text = "Save") },
        dismissButton = { NightingaleTextButton(onClick = onDismiss, text = "Cancel") }
    )
}

@Composable
fun DepartmentList(departments: List<Department>, onDelete: (String) -> Unit) {
    if (departments.isEmpty()) {
        NightingaleEmptyState(title = "No Departments", message = "Add hospital departments to organize doctors.", icon = Icons.Filled.Info)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(departments) { dept ->
                NightingaleElevatedCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(dept.name, fontWeight = FontWeight.Bold)
                            Text(dept.description)
                        }
                        IconButton(onClick = { onDelete(dept.departmentId) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun AddDepartmentDialog(onDismiss: () -> Unit, onSave: (Department) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Department") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NightingaleTextField(value = name, onValueChange = { name = it }, label = "Department Name")
                NightingaleTextField(value = description, onValueChange = { description = it }, label = "Description")
            }
        },
        confirmButton = { NightingalePrimaryButton(onClick = { onSave(Department(name = name, description = description)) }, text = "Save") },
        dismissButton = { NightingaleTextButton(onClick = onDismiss, text = "Cancel") }
    )
}
