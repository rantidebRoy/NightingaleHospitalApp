package com.example.nightingalehospitalapp.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.Patient
import com.example.nightingalehospitalapp.ui.components.NightingaleElevatedCard
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                ProfileScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("Loading...") }
    var email by remember { mutableStateOf("Loading...") }
    var role by remember { mutableStateOf("Loading...") }
    var displayId by remember { mutableStateOf("") }

    // Patient specific fields
    var dob by remember { mutableStateOf("") }
    var age by remember { mutableIntStateOf(0) }
    var gender by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    // Doctor specific fields
    var specialization by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var experienceYears by remember { mutableIntStateOf(0) }
    var departmentName by remember { mutableStateOf("") }
    var isApproved by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            email = currentUser.email ?: "Unknown Email"
            FirebaseConfig.usersRef.document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        name = document.getString("name") ?: "Unknown Name"
                        role = document.getString("role") ?: "Unknown Role"
                        val id = document.getString("displayId")
                        displayId = if (!id.isNullOrEmpty()) id else currentUser.uid

                        if (role == "PATIENT") {
                            FirebaseConfig.patientsRef.document(currentUser.uid).get()
                                .addOnSuccessListener { pDoc ->
                                    if (pDoc.exists()) {
                                        val patientObj = pDoc.toObject(Patient::class.java)
                                        if (patientObj != null) {
                                            dob = patientObj.dob
                                            age = patientObj.age
                                            gender = patientObj.gender
                                            bloodGroup = patientObj.bloodGroup
                                            address = patientObj.address
                                            emergencyContact = patientObj.emergencyContact
                                        }
                                    }
                                }
                        } else if (role == "DOCTOR") {
                            FirebaseConfig.doctorsRef.document(currentUser.uid).get()
                                .addOnSuccessListener { dDoc ->
                                    if (dDoc.exists()) {
                                        val docObj = dDoc.toObject(Doctor::class.java)
                                        if (docObj != null) {
                                            specialization = docObj.specialization
                                            qualification = docObj.qualification
                                            experienceYears = docObj.experienceYears
                                            isApproved = docObj.isApproved

                                            if (docObj.departmentId.isNotEmpty()) {
                                                FirebaseConfig.departmentsRef.document(docObj.departmentId).get()
                                                    .addOnSuccessListener { deptDoc ->
                                                        departmentName = deptDoc.getString("name") ?: "N/A"
                                                    }
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
                .addOnFailureListener { error ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(error.message ?: "Error loading profile")
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            NightingaleElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Name: $name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (displayId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ID: $displayId", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: $email", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Role: $role", style = MaterialTheme.typography.bodyLarge)

                    if (role == "PATIENT") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        if (dob.isNotEmpty()) {
                            Text("Date of Birth: $dob", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (age > 0) {
                            Text("Age: $age Years", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (gender.isNotEmpty()) {
                            Text("Gender: $gender", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (bloodGroup.isNotEmpty()) {
                            Text("Blood Group: $bloodGroup", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (address.isNotEmpty()) {
                            Text("Address: $address", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (emergencyContact.isNotEmpty()) {
                            Text("Emergency Contact: $emergencyContact", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else if (role == "DOCTOR") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        if (departmentName.isNotEmpty()) {
                            Text("Department: $departmentName", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (specialization.isNotEmpty()) {
                            Text("Specialization: $specialization", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (qualification.isNotEmpty()) {
                            Text("Qualification: $qualification", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (experienceYears > 0) {
                            Text("Experience: $experienceYears Years", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = "Status: ${if (isApproved) "Approved" else "Pending Approval"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            NightingalePrimaryButton(
                text = "Logout",
                onClick = {
                    auth.signOut()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
