package com.example.nightingalehospitalapp.doctor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.activities.ProfileActivity
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.patient.DashboardCard
import com.example.nightingalehospitalapp.patient.DashboardItem
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.google.firebase.auth.FirebaseAuth

class DoctorDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                DoctorDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen() {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("Doctor") }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            FirebaseConfig.usersRef.document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("name") ?: "Doctor"
                    }
                }
                .addOnFailureListener {
                    // Handle error
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nightingale") },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                text = "Welcome Back",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Doctor Portal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            val doctorId = currentUser?.uid.orEmpty()

            val openAppointments = {
                val intent = Intent(context, MyAppointmentsActivity::class.java)
                    .putExtra(MyAppointmentsActivity.EXTRA_DOCTOR_ID, doctorId)
                context.startActivity(intent)
            }

            val openPatientHistory = {
                val intent = Intent(context, PatientsListActivity::class.java)
                    .putExtra(PatientsListActivity.EXTRA_DOCTOR_ID, doctorId)
                context.startActivity(intent)
            }

            val dashboardItems = listOf(
                DashboardItem(
                    title = "My Appointments",
                    icon = Icons.Filled.DateRange,
                    onClick = openAppointments
                ),
                DashboardItem(
                    title = "Write Prescription",
                    icon = Icons.Filled.Edit
                ),
                DashboardItem(
                    title = "View Patients",
                    icon = Icons.Filled.Person,
                    onClick = openPatientHistory
                ),
                DashboardItem(
                    title = "Schedule Surgery",
                    icon = Icons.Filled.DateRange
                ),
                DashboardItem(
                    title = "View Test Results",
                    icon = Icons.Filled.Info
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dashboardItems) { item ->
                    DashboardCard(item)
                }
            }
        }
    }
}