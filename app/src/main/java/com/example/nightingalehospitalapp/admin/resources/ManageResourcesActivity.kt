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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.models.diagnostic.DiagnosticTest
import com.example.nightingalehospitalapp.models.hospital.Bed
import com.example.nightingalehospitalapp.models.hospital.Department
import com.example.nightingalehospitalapp.models.hospital.OperationTheatre
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
                
                var showAddDialog by remember { mutableStateOf(false) }

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

@Composable
fun BedList(beds: List<Bed>, onDelete: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(beds) { bed ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Room: ${bed.roomNumber}", fontWeight = FontWeight.Bold)
                        Text("Ward: ${bed.ward}")
                    }
                    IconButton(onClick = { onDelete(bed.bedId) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun AddBedDialog(onDismiss: () -> Unit, onSave: (Bed) -> Unit) {
    var room by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room Number") })
                OutlinedTextField(value = ward, onValueChange = { ward = it }, label = { Text("Ward") })
            }
        },
        confirmButton = { Button(onClick = { onSave(Bed(roomNumber = room, ward = ward)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun OTList(theatres: List<OperationTheatre>, onDelete: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(theatres) { ot ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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

@Composable
fun AddOTDialog(onDismiss: () -> Unit, onSave: (OperationTheatre) -> Unit) {
    var room by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Operation Theatre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room Number") })
                OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Floor") })
            }
        },
        confirmButton = { Button(onClick = { onSave(OperationTheatre(roomNumber = room, floor = floor)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TestList(tests: List<DiagnosticTest>, onDelete: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tests) { test ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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

@Composable
fun AddTestDialog(onDismiss: () -> Unit, onSave: (DiagnosticTest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Test") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Test Name") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
            }
        },
        confirmButton = { Button(onClick = { onSave(DiagnosticTest(testName = name, price = price.toDoubleOrNull() ?: 0.0)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DepartmentList(departments: List<Department>, onDelete: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(departments) { dept ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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

@Composable
fun AddDepartmentDialog(onDismiss: () -> Unit, onSave: (Department) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Department") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Department Name") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        },
        confirmButton = { Button(onClick = { onSave(Department(name = name, description = description)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
