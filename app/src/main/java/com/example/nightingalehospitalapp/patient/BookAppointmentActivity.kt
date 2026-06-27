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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.models.appointment.Slot
import com.example.nightingalehospitalapp.repository.user.DoctorWithUser
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
    val departments = listOf("All", "General", "Cardiology", "Neurology", "Orthopedics")

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(dateFormatter.format(Date())) }
    
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchDoctors()
    }

    // Slots are fetched explicitly when the user clicks the search button

    LaunchedEffect(bookingState) {
        when (bookingState) {
            is BookingViewModel.BookingState.Success -> {
                Toast.makeText(context, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetBookingState()
                onBack()
            }
            is BookingViewModel.BookingState.Error -> {
                val msg = (bookingState as BookingViewModel.BookingState.Error).message
                Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                viewModel.resetBookingState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (selectedDoctor == null) {
                Text("Select a Doctor", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Department Filter
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(departments) { dept ->
                        FilterChip(
                            selected = selectedDepartment == dept,
                            onClick = { viewModel.setDepartmentFilter(dept) },
                            label = { Text(dept) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                    items(doctors) { doctorWithUser ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { selectedDoctor = doctorWithUser },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Dr. ${doctorWithUser.user.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Specialization: ${doctorWithUser.doctor.specialization}")
                            }
                        }
                    }
                }
            } else {
                // Booking Form
                Text("Booking with Dr. ${selectedDoctor!!.user.name}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (selectedDoctor != null && selectedDate.isNotEmpty()) {
                            viewModel.fetchAvailableSlots(selectedDoctor!!.user.userId, selectedDate.trim())
                            selectedSlot = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Find Available Slots")
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
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes/Symptoms") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val auth = FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null && selectedSlot != null) {
                            viewModel.bookAppointment(
                                doctorId = selectedDoctor!!.user.userId,
                                patientId = currentUser.uid,
                                patientName = currentUser.displayName ?: "Patient",
                                date = selectedDate,
                                time = selectedSlot!!.time,
                                notes = notes,
                                slotId = selectedSlot!!.slotId
                            )
                        } else {
                            Toast.makeText(context, "Please select a valid time slot", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedSlot != null && bookingState !is BookingViewModel.BookingState.Loading
                ) {
                    if (bookingState is BookingViewModel.BookingState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confirm Booking")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { selectedDoctor = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Doctor")
                }
            }
        }
    }
}
