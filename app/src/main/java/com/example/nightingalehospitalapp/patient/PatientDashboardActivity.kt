package com.example.nightingalehospitalapp.patient

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.activities.ProfileActivity
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingaleUserScaffold
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.google.firebase.auth.FirebaseAuth

class PatientDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                PatientDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen() {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("Patient") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            FirebaseConfig.usersRef.document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("name") ?: "Patient"
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to load profile: ${e.message}"
                }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    NightingaleUserScaffold(
        title = "Nightingale",
        currentTab = 0,
        onTabSelected = { tabIndex ->
            when (tabIndex) {
                0 -> { /* Already on Home */ }
                1 -> context.startActivity(Intent(context, MyAppointmentsActivity::class.java))
                2 -> context.startActivity(Intent(context, ProfileActivity::class.java))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                text = "One App, Complete Healthcare Solution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            val dashboardItems = listOf(
                DashboardItem("Book Appointments", Icons.Filled.DateRange) {
                    context.startActivity(Intent(context, BookAppointmentActivity::class.java))
                },
                DashboardItem("View Prescriptions", Icons.Filled.Info) {
                    context.startActivity(Intent(context, ViewPrescriptionsActivity::class.java))
                },
                DashboardItem("View Medicine", Icons.Filled.Info) {
                    context.startActivity(Intent(context, ViewMedicineActivity::class.java))
                },
                DashboardItem("Test Results", Icons.Filled.Info) {
                    context.startActivity(Intent(context, ViewTestResultsActivity::class.java))
                },
                DashboardItem("Medical History", Icons.Filled.Favorite) {
                    val uid = currentUser?.uid.orEmpty()
                    if (uid.isNotBlank()) {
                        val intent = Intent(context, com.example.nightingalehospitalapp.doctor.PatientHistoryActivity::class.java).apply {
                            putExtra(com.example.nightingalehospitalapp.doctor.PatientHistoryActivity.EXTRA_PATIENT_ID, uid)
                            putExtra(com.example.nightingalehospitalapp.doctor.PatientHistoryActivity.EXTRA_PATIENT_NAME, userName)
                            putExtra(com.example.nightingalehospitalapp.doctor.PatientHistoryActivity.EXTRA_CAN_REDACT, false)
                        }
                        context.startActivity(intent)
                    }
                },
                DashboardItem("My Appointments", Icons.Filled.DateRange) {
                    context.startActivity(Intent(context, MyAppointmentsActivity::class.java))
                }
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

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {}
)

@Composable
fun DashboardCard(
    item: DashboardItem,
    onClick: () -> Unit = item.onClick
) {
    NightingaleElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}