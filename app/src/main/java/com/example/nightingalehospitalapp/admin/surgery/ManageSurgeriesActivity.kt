package com.example.nightingalehospitalapp.admin.surgery

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
import com.example.nightingalehospitalapp.models.enums.SurgeryStatus
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.surgery.ManageSurgeriesViewModel
import com.example.nightingalehospitalapp.viewmodel.admin.surgery.SurgeryBookingItem

class ManageSurgeriesActivity : ComponentActivity() {

    private val viewModel: ManageSurgeriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NightingaleHospitalAppTheme {
                ManageSurgeriesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onNavigateToSchedule = {
                        startActivity(Intent(this, ScheduleSurgeryActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchSurgeries()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSurgeriesScreen(
    viewModel: ManageSurgeriesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val surgeries by viewModel.surgeries.collectAsState()
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
                title = { Text("Manage Surgeries") },
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
            FloatingActionButton(onClick = onNavigateToSchedule) {
                Icon(Icons.Filled.Add, contentDescription = "Schedule Surgery")
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
            } else if (surgeries.isEmpty()) {
                Text(
                    text = "No surgeries scheduled.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(surgeries) { surgery ->
                        SurgeryCard(
                            surgery = surgery,
                            onStatusChange = { newStatus ->
                                viewModel.updateSurgeryStatus(surgery.surgeryId, newStatus, surgery.otId) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgeryCard(
    surgery: SurgeryBookingItem,
    onStatusChange: (SurgeryStatus) -> Unit
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
            Text(text = surgery.surgeryType, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Patient: ${surgery.patientName}")
            Text(text = "Doctor: ${surgery.doctorName}")
            Text(text = "OT Room: ${surgery.otRoom}")
            Text(text = "Date: ${surgery.date} (${surgery.startTime} - ${surgery.endTime})")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = surgery.status.name,
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
                    SurgeryStatus.values().forEach { statusOption ->
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
        }
    }
}
