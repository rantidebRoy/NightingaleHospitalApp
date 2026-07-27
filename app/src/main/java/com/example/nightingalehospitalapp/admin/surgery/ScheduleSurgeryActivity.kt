package com.example.nightingalehospitalapp.admin.surgery

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.admin.surgery.ScheduleSurgeryViewModel
import java.util.Calendar

class ScheduleSurgeryActivity : ComponentActivity() {
    private val viewModel: ScheduleSurgeryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightingaleHospitalAppTheme {
                ScheduleSurgeryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onSuccess = {
                        Toast.makeText(this, "Surgery scheduled successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh patients/doctors/OTs so newly-added entries (e.g. an admin
        // creating a doctor or OT while this screen sat in the back stack)
        // are reflected when we return.
        viewModel.refresh()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSurgeryScreen(
    viewModel: ScheduleSurgeryViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val patients by viewModel.patients.collectAsState()
    val doctors by viewModel.doctors.collectAsState()
    val operationTheatres by viewModel.operationTheatres.collectAsState()
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

    var selectedOtId by remember { mutableStateOf("") }
    var otSearchQuery by remember { mutableStateOf("") }
    var otExpanded by remember { mutableStateOf(false) }
    val filteredOts = operationTheatres.filter { it.roomNumber.contains(otSearchQuery, ignoreCase = true) || it.otId.contains(otSearchQuery, ignoreCase = true) }

    var surgeryType by remember { mutableStateOf("") }

    // 3 Boxes for Date
    val todayCal = Calendar.getInstance()
    var yearStr by remember { mutableStateOf(todayCal.get(Calendar.YEAR).toString()) }
    var monthStr by remember { mutableStateOf(String.format("%02d", todayCal.get(Calendar.MONTH) + 1)) }
    var dayStr by remember { mutableStateOf(String.format("%02d", todayCal.get(Calendar.DAY_OF_MONTH))) }

    // 3 Boxes for Time
    var timeHour by remember { mutableStateOf("10") }
    var timeMinute by remember { mutableStateOf("00") }
    var selectedAmPm by remember { mutableStateOf("AM") }
    var amPmExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Surgery") },
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

                // OT Dropdown
                ExposedDropdownMenuBox(
                    expanded = otExpanded,
                    onExpandedChange = { otExpanded = !otExpanded }
                ) {
                    OutlinedTextField(
                        value = otSearchQuery,
                        onValueChange = {
                            otSearchQuery = it
                            otExpanded = true
                            selectedOtId = ""
                        },
                        readOnly = false,
                        label = { Text("Operation Theatre") },
                        placeholder = { Text("Select or search OT") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = otExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = otExpanded,
                        onDismissRequest = { otExpanded = false }
                    ) {
                        if (filteredOts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No OTs found") },
                                onClick = { otExpanded = false }
                            )
                        } else {
                            filteredOts.forEach { ot ->
                                DropdownMenuItem(
                                    text = { Text("Room ${ot.roomNumber} (Floor ${ot.floor})") },
                                    onClick = {
                                        selectedOtId = ot.otId
                                        otSearchQuery = "Room ${ot.roomNumber} (Floor ${ot.floor})"
                                        otExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = surgeryType,
                    onValueChange = { surgeryType = it },
                    label = { Text("Surgery Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Input (3 Boxes)
                Text("Surgery Date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { if (it.length <= 4) yearStr = it.filter { c -> c.isDigit() } },
                        label = { Text("YYYY") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthStr,
                        onValueChange = { if (it.length <= 2) monthStr = it.filter { c -> c.isDigit() } },
                        label = { Text("MM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dayStr,
                        onValueChange = { if (it.length <= 2) dayStr = it.filter { c -> c.isDigit() } },
                        label = { Text("DD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Time Input (3 Boxes: Hour, Minute, AM/PM)
                Text("Surgery Time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = timeHour,
                        onValueChange = { if (it.length <= 2) timeHour = it.filter { c -> c.isDigit() } },
                        label = { Text("Hour (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeMinute,
                        onValueChange = { if (it.length <= 2) timeMinute = it.filter { c -> c.isDigit() } },
                        label = { Text("Minute (00-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = amPmExpanded,
                        onExpandedChange = { amPmExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedAmPm,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("AM/PM") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = amPmExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = amPmExpanded,
                            onDismissRequest = { amPmExpanded = false }
                        ) {
                            listOf("AM", "PM").forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period) },
                                    onClick = {
                                        selectedAmPm = period
                                        amPmExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val y = yearStr.toIntOrNull()
                        val m = monthStr.toIntOrNull()
                        val d = dayStr.toIntOrNull()
                        val formattedDate = if (y != null && m != null && d != null) String.format("%04d-%02d-%02d", y, m, d) else ""

                        val h = timeHour.toIntOrNull()
                        val min = timeMinute.toIntOrNull()
                        val formattedTime = if (h != null && min != null && h in 1..12 && min in 0..59) String.format("%02d:%02d %s", h, min, selectedAmPm) else ""

                        viewModel.submitSurgery(
                            selectedPatientId,
                            selectedDoctorId,
                            selectedOtId,
                            surgeryType,
                            formattedDate,
                            formattedTime,
                            ""
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
                    Text("Schedule Surgery")
                }
            }
        }
    }
}
