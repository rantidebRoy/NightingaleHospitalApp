package com.example.nightingalehospitalapp.doctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.components.NightingaleTextField
import com.example.nightingalehospitalapp.ui.components.NightingaleUserScaffold
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.ManageSlotsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class ManageSlotsActivity : ComponentActivity() {

    private val manageSlotsViewModel: ManageSlotsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID) ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        setContent {
            NightingaleHospitalAppTheme {
                ManageSlotsScreen(manageSlotsViewModel, targetDoctorId = doctorId) { finish() }
            }
        }
    }

    companion object {
        const val EXTRA_DOCTOR_ID = "extra_doctor_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSlotsScreen(viewModel: ManageSlotsViewModel, targetDoctorId: String = "", onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val doctorId = targetDoctorId.ifBlank { auth.currentUser?.uid.orEmpty() }

    // 3 Boxes for Date
    val todayCal = Calendar.getInstance()
    var slotYear by remember { mutableStateOf(todayCal.get(Calendar.YEAR).toString()) }
    var slotMonth by remember { mutableStateOf(String.format("%02d", todayCal.get(Calendar.MONTH) + 1)) }
    var slotDay by remember { mutableStateOf(String.format("%02d", todayCal.get(Calendar.DAY_OF_MONTH))) }

    // 3 Boxes for Time
    var timeHour by remember { mutableStateOf("") }
    var timeMinute by remember { mutableStateOf("") }
    var selectedAmPm by remember { mutableStateOf("AM") }
    var amPmExpanded by remember { mutableStateOf(false) }

    val slots by viewModel.slots.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val dateError = remember(slotYear, slotMonth, slotDay) {
        validateSlotDate(slotYear, slotMonth, slotDay)
    }

    val formattedDate = remember(slotYear, slotMonth, slotDay, dateError) {
        if (dateError == null) {
            String.format("%04d-%02d-%02d", slotYear.toInt(), slotMonth.toInt(), slotDay.toInt())
        } else ""
    }

    LaunchedEffect(formattedDate, doctorId) {
        if (formattedDate.isNotEmpty() && doctorId.isNotEmpty()) {
            viewModel.observeSlots(doctorId, formattedDate)
        }
    }

    NightingaleUserScaffold(
        title = "Manage Schedule",
        showBottomBar = false,
        onNavigateBack = onBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Select Date", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Date Input (3 Boxes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NightingaleTextField(
                    value = slotYear,
                    onValueChange = { if (it.length <= 4) slotYear = it.filter { c -> c.isDigit() } },
                    label = "Year (YYYY)",
                    modifier = Modifier.weight(1f)
                )
                NightingaleTextField(
                    value = slotMonth,
                    onValueChange = { if (it.length <= 2) slotMonth = it.filter { c -> c.isDigit() } },
                    label = "Month (MM)",
                    modifier = Modifier.weight(1f)
                )
                NightingaleTextField(
                    value = slotDay,
                    onValueChange = { if (it.length <= 2) slotDay = it.filter { c -> c.isDigit() } },
                    label = "Day (DD)",
                    modifier = Modifier.weight(1f)
                )
            }

            if (dateError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dateError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (dateError == null && formattedDate.isNotEmpty()) {
                // Add slot section
                Text("Add a New Slot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                // Time Input (3 Boxes: Hour, Minute, AM/PM)
                Text("Slot Time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NightingaleTextField(
                        value = timeHour,
                        onValueChange = { if (it.length <= 2) timeHour = it.filter { c -> c.isDigit() } },
                        label = "Hour (1-12)",
                        modifier = Modifier.weight(1f)
                    )
                    NightingaleTextField(
                        value = timeMinute,
                        onValueChange = { if (it.length <= 2) timeMinute = it.filter { c -> c.isDigit() } },
                        label = "Minute (00-59)",
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = amPmExpanded,
                        onExpandedChange = { amPmExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        NightingaleTextField(
                            value = selectedAmPm,
                            onValueChange = {},
                            readOnly = true,
                            label = "AM/PM",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = amPmExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Add Presets
                Text("Or Select Preset:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("09:00 AM", "10:00 AM", "11:00 AM", "02:00 PM", "03:00 PM", "04:00 PM").forEach { preset ->
                        AssistChip(
                            onClick = {
                                val parts = preset.split(" ")
                                val timeParts = parts[0].split(":")
                                timeHour = timeParts[0]
                                timeMinute = timeParts[1]
                                selectedAmPm = parts[1]
                            },
                            label = { Text(preset, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NightingalePrimaryButton(
                    text = "Add Slot",
                    onClick = {
                        val timeError = validateSlotTime(timeHour, timeMinute, selectedAmPm)
                        if (timeError != null) {
                            coroutineScope.launch { snackbarHostState.showSnackbar(timeError) }
                            return@NightingalePrimaryButton
                        }
                        val formattedTime = String.format("%02d:%02d %s", timeHour.toInt(), timeMinute.toInt(), selectedAmPm)
                        viewModel.addSlot(doctorId, formattedDate, formattedTime)
                        coroutineScope.launch { snackbarHostState.showSnackbar("Slot added for $formattedTime") }
                        timeHour = ""
                        timeMinute = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Existing slots list
                Text("Slots for $formattedDate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                if (slots.isEmpty()) {
                    Text("No slots created for this date yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slots.forEach { slot ->
                            NightingaleElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(slot.time, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            if (slot.booked) "Booked by: ${slot.patientName.ifEmpty { "Patient" }}" else "Available",
                                            color = if (slot.booked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                     var slotToDelete by remember { mutableStateOf<com.example.nightingalehospitalapp.models.appointment.Slot?>(null) }

                                     IconButton(onClick = {
                                         if (slot.booked) {
                                             slotToDelete = slot
                                         } else {
                                             viewModel.deleteSlot(slot.slotId)
                                             coroutineScope.launch { snackbarHostState.showSnackbar("Slot deleted") }
                                         }
                                     }) {
                                         Icon(
                                             Icons.Filled.Delete,
                                             contentDescription = "Delete Slot",
                                             tint = MaterialTheme.colorScheme.error
                                         )
                                     }

                                     val targetToDelete = slotToDelete
                                     if (targetToDelete != null) {
                                         AlertDialog(
                                             onDismissRequest = { slotToDelete = null },
                                             title = { Text("Delete Booked Slot?") },
                                             text = {
                                                 Text("This slot is currently booked by ${targetToDelete.patientName.ifBlank { "a patient" }}. Deleting this slot will automatically cancel their appointment in the system. Do you want to proceed?")
                                             },
                                             confirmButton = {
                                                 TextButton(
                                                     onClick = {
                                                         viewModel.deleteBookedSlot(targetToDelete) { success ->
                                                             if (success) {
                                                                 coroutineScope.launch { snackbarHostState.showSnackbar("Slot deleted & appointment cancelled") }
                                                             } else {
                                                                 coroutineScope.launch { snackbarHostState.showSnackbar("Failed to delete slot") }
                                                             }
                                                         }
                                                         slotToDelete = null
                                                     }
                                                 ) {
                                                     Text("Delete & Cancel Appointment", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                 }
                                             },
                                             dismissButton = {
                                                 TextButton(onClick = { slotToDelete = null }) {
                                                     Text("Cancel")
                                                 }
                                             }
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

fun validateSlotDate(yearStr: String, monthStr: String, dayStr: String): String? {
    val year = yearStr.toIntOrNull() ?: return "Year must be a number"
    val month = monthStr.toIntOrNull() ?: return "Month must be a number"
    val day = dayStr.toIntOrNull() ?: return "Day must be a number"

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    if (year < currentYear || year > currentYear + 5) {
        return "Please enter a valid year ($currentYear to ${currentYear + 5})"
    }
    if (month < 1 || month > 12) {
        return "Please enter a valid month (1-12)"
    }

    val maxDaysInMonth = when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    if (day < 1 || day > maxDaysInMonth) {
        return "Invalid day for selected month (max $maxDaysInMonth days)"
    }

    return null
}

fun validateSlotTime(hourStr: String, minuteStr: String, amPm: String): String? {
    val h = hourStr.toIntOrNull() ?: return "Hour must be a valid number (1-12)"
    val m = minuteStr.toIntOrNull() ?: return "Minute must be a valid number (0-59)"

    if (h < 1 || h > 12) {
        return "Hour must be between 1 and 12"
    }
    if (m < 0 || m > 59) {
        return "Minute must be between 0 and 59"
    }
    if (amPm != "AM" && amPm != "PM") {
        return "Select AM or PM"
    }
    return null
}
