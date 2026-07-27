package com.example.nightingalehospitalapp.patient

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.models.appointment.Slot
import com.example.nightingalehospitalapp.repository.user.DoctorWithUser
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.components.NightingaleTextField
import com.example.nightingalehospitalapp.ui.components.NightingaleUserScaffold
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.BookingViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentActivity : ComponentActivity() {

    private val bookingViewModel: BookingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                BookAppointmentScreen(bookingViewModel) { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(viewModel: BookingViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val doctors by viewModel.doctors.collectAsState()
    val slots by viewModel.availableSlots.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()

    var selectedDoctor by remember { mutableStateOf<DoctorWithUser?>(null) }
    
    val selectedDepartment by viewModel.selectedDepartment.collectAsState()
    val departmentsList by viewModel.departments.collectAsState()
    var departmentExpanded by remember { mutableStateOf(false) }

    val departmentOptions = remember(departmentsList) {
        listOf("All Departments") + departmentsList.map { it.name }
    }

    // 3 Boxes for Date
    val todayCal = Calendar.getInstance()
    var yearStr by remember { mutableStateOf(todayCal.get(Calendar.YEAR).toString()) }
    var monthStr by remember { mutableStateOf(String.format("%02d", todayCal.get(Calendar.MONTH) + 1)) }
    var dayStr by remember { mutableStateOf(String.format("%02d", todayCal.get(Calendar.DAY_OF_MONTH))) }

    val formattedDate = remember(yearStr, monthStr, dayStr) {
        val y = yearStr.toIntOrNull()
        val m = monthStr.toIntOrNull()
        val d = dayStr.toIntOrNull()
        if (y != null && m != null && d != null) {
            String.format("%04d-%02d-%02d", y, m, d)
        } else ""
    }
    
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var notes by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.fetchDoctors()
        viewModel.fetchDepartments()
    }

    // Auto-fetch slots whenever doctor or date changes
    LaunchedEffect(selectedDoctor, formattedDate) {
        val doc = selectedDoctor
        if (doc != null && formattedDate.isNotBlank()) {
            viewModel.fetchAvailableSlots(doc.user.userId, formattedDate)
            selectedSlot = null
        }
    }

    LaunchedEffect(bookingState) {
        when (bookingState) {
            is BookingViewModel.BookingState.Success -> {
                snackbarHostState.showSnackbar("Appointment booked successfully!")
                viewModel.resetBookingState()
                onBack()
            }
            is BookingViewModel.BookingState.Error -> {
                val msg = (bookingState as BookingViewModel.BookingState.Error).message
                snackbarHostState.showSnackbar("Error: $msg")
                viewModel.resetBookingState()
            }
            else -> {}
        }
    }

    NightingaleUserScaffold(
        title = "Book Appointment",
        showBottomBar = false,
        onNavigateBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (selectedDoctor == null) {
                Text("Select a Doctor", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Department Dropdown Filter
                ExposedDropdownMenuBox(
                    expanded = departmentExpanded,
                    onExpandedChange = { departmentExpanded = !departmentExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDepartment,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Department") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = departmentExpanded,
                        onDismissRequest = { departmentExpanded = false },
                        modifier = Modifier.heightIn(max = 160.dp)
                    ) {
                        departmentOptions.forEach { deptName ->
                            DropdownMenuItem(
                                text = { Text(deptName) },
                                onClick = {
                                    viewModel.setDepartmentFilter(deptName)
                                    departmentExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                    items(doctors) { doctorWithUser ->
                        NightingaleElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { selectedDoctor = doctorWithUser }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Dr. ${doctorWithUser.user.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Department: ${viewModel.getDepartmentNameForDoctor(doctorWithUser.doctor)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (doctorWithUser.doctor.specialization.isNotBlank()) {
                                    Text("Specialization: ${doctorWithUser.doctor.specialization}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            } else {
                // Booking Form
                Text("Booking with Dr. ${selectedDoctor!!.user.name}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                // 3 Boxes for Date
                Text("Appointment Date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
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
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Available Slots", style = MaterialTheme.typography.titleMedium)
                if (slots.isEmpty()) {
                    Text("No slots available for this date.", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(slots) { slot ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedSlot = slot }
                            ) {
                                RadioButton(
                                    selected = (selectedSlot == slot),
                                    onClick = { selectedSlot = slot }
                                )
                                Text(slot.time)
                            }
                        }
                    }
                }
                
                if (selectedSlot != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NightingaleTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes / Symptoms (optional)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    NightingalePrimaryButton(
                        text = if (bookingState is BookingViewModel.BookingState.Loading) "Loading..." else "Confirm Booking",
                        onClick = {
                            val auth = FirebaseAuth.getInstance()
                            val currentUser = auth.currentUser
                            if (currentUser != null && selectedSlot != null && formattedDate.isNotBlank()) {
                                viewModel.bookAppointment(
                                    doctorId = selectedDoctor!!.user.userId,
                                    patientId = currentUser.uid,
                                    patientName = currentUser.displayName ?: "Patient",
                                    date = formattedDate,
                                    time = selectedSlot!!.time,
                                    notes = notes,
                                    slotId = selectedSlot!!.slotId
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = bookingState !is BookingViewModel.BookingState.Loading
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        selectedDoctor = null
                        selectedSlot = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Doctor")
                }
            }
        }
    }
}
