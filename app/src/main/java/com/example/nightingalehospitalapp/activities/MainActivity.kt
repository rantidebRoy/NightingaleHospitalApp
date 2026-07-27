package com.example.nightingalehospitalapp.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nightingalehospitalapp.admin.AdminDashboardActivity
import com.example.nightingalehospitalapp.database.FirebaseConfig
import com.example.nightingalehospitalapp.doctor.DoctorDashboardActivity
import com.example.nightingalehospitalapp.patient.PatientDashboardActivity
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = this
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val fromOnboarding = intent.getBooleanExtra("from_onboarding", false)

        if (currentUser == null && !fromOnboarding) {
            val intent = Intent(context, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                MainScreen(
                    onNavigateToLogin = { startActivity(Intent(this, LoginActivity::class.java)) },
                    onNavigateToRegister = { startActivity(Intent(this, RegisterActivity::class.java)) },
                    onNavigateToAdmin = { 
                        val intent = Intent(this, AdminDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    },
                    onNavigateToDoctor = {
                        val intent = Intent(this, DoctorDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    },
                    onNavigateToPatient = {
                        val intent = Intent(this, PatientDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToDoctor: () -> Unit,
    onNavigateToPatient: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            FirebaseConfig.usersRef.document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        val approved = document.getBoolean("approved")

                        if (role == "DOCTOR" && approved == false) {
                            // Doctor not approved yet, sign out and show login/landing
                            auth.signOut()
                            isLoading = false
                        } else {
                            when (role) {
                                "ADMIN" -> onNavigateToAdmin()
                                "DOCTOR" -> onNavigateToDoctor()
                                "PATIENT" -> onNavigateToPatient()
                                else -> isLoading = false
                            }
                        }
                    } else {
                        auth.signOut()
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    auth.signOut()
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LaunchingDashboard(
            onLoginClick = onNavigateToLogin,
            onRegisterClick = onNavigateToRegister
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchingDashboard(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nightingale Hospital") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Nightingale Hospital App",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            NightingalePrimaryButton(
                text = "Login",
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            NightingalePrimaryButton(
                text = "Register",
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LaunchingDashboardPreview() {
    NightingaleHospitalAppTheme {
        LaunchingDashboard(onLoginClick = {}, onRegisterClick = {})
    }
}