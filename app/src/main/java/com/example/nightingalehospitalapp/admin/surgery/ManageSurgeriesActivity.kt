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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.models.enums.SurgeryStatus
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.surgery.ManageSurgeriesViewModel
import com.example.nightingalehospitalapp.viewmodel.admin.surgery.SurgeryBookingItem
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingaleEmptyState
import com.example.nightingalehospitalapp.ui.components.NightingaleListShimmer
import androidx.compose.material.icons.filled.Info

/**
 * Lists surgery bookings.
 *
 * • Default (admin) call — no extras — shows every booking in the system
 *   and exposes the per-card status dropdown plus a FAB to schedule new
 *   surgeries.
 *
 * • `EXTRA_DOCTOR_ID` set — list is filtered to that doctor's surgeries.
 *   The doctor can update the status of their own surgeries but the FAB
 *   (and the Schedule Surgery navigation) is hidden so they cannot add
 *   new ones.
 */
class ManageSurgeriesActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DOCTOR_ID = "extra_doctor_id"
    }

    private val viewModel: ManageSurgeriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If launched by a doctor, bind the VM to that doctor's scope once.
        // bindDoctor() also flips [isDoctorScope], which the screen collects
        // reactively so the FAB / title recompose as soon as scope is bound.
        intent?.getStringExtra(EXTRA_DOCTOR_ID)?.let { doctorId ->
            if (doctorId.isNotBlank()) viewModel.bindDoctor(doctorId)
        }

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
    // Collected reactively so the FAB / title recompose when bindDoctor()
    // flips scope (doctors arrive via intent extra after onCreate).
    val isDoctorScope by viewModel.isDoctorScope.collectAsState()
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
                title = {
                    Text(
                        if (isDoctorScope) "My Surgeries" else "Manage Surgeries"
                    )
                },
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
        // Admins get a FAB to schedule new surgeries; doctors do not.
        floatingActionButton = {
            if (!isDoctorScope) {
                FloatingActionButton(onClick = onNavigateToSchedule) {
                    Icon(Icons.Filled.Add, contentDescription = "Schedule Surgery")
                }
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
            } else if (surgeries.isEmpty()) {
                NightingaleEmptyState(
                    title = "No Surgeries",
                    message = if (isDoctorScope) "No surgeries have been assigned to you yet." else "No surgeries scheduled.",
                    icon = Icons.Filled.Info,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(surgeries) { surgery ->
                        SurgeryCard(
                            surgery = surgery,
                            showStatusEditor = true,
                            onStatusChange = { newStatus ->
                                viewModel.updateSurgeryStatus(
                                    surgery.surgeryId, newStatus, surgery.otId
                                ) { success, msg ->
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "Status Updated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error: $msg",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
    showStatusEditor: Boolean,
    onStatusChange: (SurgeryStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    NightingaleElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = surgery.surgeryType,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Patient: ${surgery.patientName}")
            Text(text = "Doctor: ${surgery.doctorName}")
            Text(text = "OT Room: ${surgery.otRoom}")
            Text(text = if (surgery.startTime.isNotBlank()) "Date: ${surgery.date} (${surgery.startTime})" else "Date: ${surgery.date}")
            Spacer(modifier = Modifier.height(8.dp))

            // Surgeries persist in the scheduled list regardless of status —
            // they only truly "leave" once the doctor marks them CANCELLED.
            // The dropdown is the single status control; a separate badge would
            // duplicate its label and obscure whether the status is editable.
            if (showStatusEditor) {
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = surgery.status.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Update Status") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SurgeryStatus.values().filter { it != SurgeryStatus.AVAILABLE }.forEach { statusOption ->
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
}
