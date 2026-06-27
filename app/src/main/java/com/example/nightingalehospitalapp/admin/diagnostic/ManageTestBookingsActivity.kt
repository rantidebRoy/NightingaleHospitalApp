package com.example.nightingalehospitalapp.admin.diagnostic

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.nightingalehospitalapp.models.diagnostic.TestResultItem
import com.example.nightingalehospitalapp.models.enums.TestStatus
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.diagnostic.ManageTestBookingsViewModel
import com.example.nightingalehospitalapp.viewmodel.admin.diagnostic.TestBookingItem

class ManageTestBookingsActivity : ComponentActivity() {

    private val viewModel: ManageTestBookingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NightingaleHospitalAppTheme {
                ManageTestBookingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToBook = {
                        startActivity(Intent(this, BookTestActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchTestBookings()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTestBookingsScreen(
    viewModel: ManageTestBookingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBook: () -> Unit
) {
    val testBookings by viewModel.testBookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedBooking by remember { mutableStateOf<TestBookingItem?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostic Tests") },
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
            FloatingActionButton(onClick = onNavigateToBook) {
                Icon(Icons.Filled.Add, contentDescription = "Book Test")
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
            } else if (testBookings.isEmpty()) {
                Text(
                    text = "No diagnostic tests booked.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(testBookings) { booking ->
                        TestBookingCard(
                            booking = booking,
                            onStatusChange = { newStatus ->
                                viewModel.updateStatus(booking.bookingId, newStatus) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onUploadResult = {
                                selectedBooking = booking
                                showUploadDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showUploadDialog && selectedBooking != null) {
        UploadTestResultDialog(
            booking = selectedBooking!!,
            onDismiss = { showUploadDialog = false },
            onSubmit = { results ->
                viewModel.uploadTestResult(
                    bookingId = selectedBooking!!.bookingId,
                    patientId = selectedBooking!!.patientId,
                    results = results
                ) { success, msg ->
                    if (success) {
                        Toast.makeText(context, "Result Uploaded Successfully", Toast.LENGTH_SHORT).show()
                        showUploadDialog = false
                    } else {
                        Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestBookingCard(
    booking: TestBookingItem,
    onStatusChange: (TestStatus) -> Unit,
    onUploadResult: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = booking.testName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Patient: ${booking.patientName}")
            Text(text = "Doctor: ${booking.doctorName}")
            Text(text = "Date: ${booking.date}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = booking.status.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    TestStatus.values().forEach { statusOption ->
                        DropdownMenuItem(
                            text = { Text(statusOption.name) },
                            onClick = {
                                onStatusChange(statusOption)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            if (booking.status != TestStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onUploadResult,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Upload Result")
                }
            }
        }
    }
}

@Composable
fun UploadTestResultDialog(
    booking: TestBookingItem,
    onDismiss: () -> Unit,
    onSubmit: (List<TestResultItem>) -> Unit
) {
    var items by remember { mutableStateOf(listOf(TestResultItem("", ""))) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Upload Result: ${booking.testName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Patient: ${booking.patientName}", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = item.problem,
                                onValueChange = { newValue ->
                                    val newItems = items.toMutableList()
                                    newItems[index] = item.copy(problem = newValue)
                                    items = newItems
                                },
                                label = { Text("Problem (e.g., Blood Pressure)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = item.result,
                                onValueChange = { newValue ->
                                    val newItems = items.toMutableList()
                                    newItems[index] = item.copy(result = newValue)
                                    items = newItems
                                },
                                label = { Text("Result (e.g., 120/80)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { items = items + TestResultItem("", "") }) {
                    Text("+ Add Another Field")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            // Filter out empty rows
                            val validItems = items.filter { it.problem.isNotBlank() && it.result.isNotBlank() }
                            onSubmit(validItems) 
                        }
                    ) {
                        Text("Submit Result")
                    }
                }
            }
        }
    }
}
