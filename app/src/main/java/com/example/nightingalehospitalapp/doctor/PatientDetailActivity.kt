package com.example.nightingalehospitalapp.doctor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.patient.DashboardCard
import com.example.nightingalehospitalapp.patient.DashboardItem
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme

class PatientDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val patientId = intent.getStringExtra(EXTRA_PATIENT_ID).orEmpty()
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME)
            ?: intent.getStringExtra(EXTRA_PATIENT_NAME_FALLBACK)
            ?: "Patient"

        setContent {
            NightingaleHospitalAppTheme {
                PatientDetailScreen(
                    patientId = patientId,
                    patientName = patientName
                )
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT_ID = "extra_patient_id"
        const val EXTRA_PATIENT_NAME = "extra_patient_name"
        // Fallback to the key PatientHistoryActivity already uses for the display name.
        const val EXTRA_PATIENT_NAME_FALLBACK = "extra_patient_display_name"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(patientId: String, patientName: String) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patientName) },
                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text(
                text = "Patient Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            val items = listOf(
                DashboardItem(
                    title = "View History",
                    icon = Icons.Filled.History,
                    onClick = {
                        val intent = Intent(context, PatientHistoryActivity::class.java).apply {
                            putExtra(PatientHistoryActivity.EXTRA_PATIENT_ID, patientId)
                            putExtra(PatientHistoryActivity.EXTRA_PATIENT_NAME, patientName)
                        }
                        context.startActivity(intent)
                    }
                ),
                DashboardItem(
                    title = "View Test Results",
                    icon = Icons.Filled.Info,
                    onClick = {
                        val intent = Intent(context, PatientTestResultsActivity::class.java).apply {
                            putExtra(PatientTestResultsActivity.EXTRA_PATIENT_ID, patientId)
                            putExtra(PatientTestResultsActivity.EXTRA_PATIENT_NAME, patientName)
                        }
                        context.startActivity(intent)
                    }
                ),
                DashboardItem(
                    title = "Write Prescription",
                    icon = Icons.Filled.Edit,
                    onClick = {
                        val intent = Intent(context, WritePrescriptionActivity::class.java).apply {
                            putExtra(WritePrescriptionActivity.EXTRA_PATIENT_ID, patientId)
                            putExtra(WritePrescriptionActivity.EXTRA_PATIENT_NAME, patientName)
                        }
                        context.startActivity(intent)
                    }
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    DashboardCard(item)
                }
            }
        }
    }
}