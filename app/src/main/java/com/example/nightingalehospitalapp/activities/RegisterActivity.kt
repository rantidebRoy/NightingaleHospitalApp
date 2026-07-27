package com.example.nightingalehospitalapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.nightingalehospitalapp.models.user.Doctor
import com.example.nightingalehospitalapp.models.user.Patient
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.ui.components.NightingalePrimaryButton
import com.example.nightingalehospitalapp.ui.components.NightingaleTextField
import com.example.nightingalehospitalapp.ui.theme.NightingaleHospitalAppTheme
import com.example.nightingalehospitalapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

class RegisterActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightingaleHospitalAppTheme {
                viewModel = ViewModelProvider(this@RegisterActivity).get(AuthViewModel::class.java)
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { finish() },
                    onBackClick = {
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1 State
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    val roles = listOf("PATIENT", "DOCTOR")
    var roleExpanded by remember { mutableStateOf(false) }

    // Step 2 State - Patient
    var birthYear by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthDay by remember { mutableStateOf("") }

    var selectedGender by remember { mutableStateOf("") }
    val genders = listOf("MALE", "FEMALE", "OTHER")
    var genderExpanded by remember { mutableStateOf(false) }

    var selectedBloodGroup by remember { mutableStateOf("") }
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    var bloodGroupExpanded by remember { mutableStateOf(false) }

    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    // Step 2 State - Doctor
    var specialization by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var experienceYearsStr by remember { mutableStateOf("") }
    var selectedDeptName by remember { mutableStateOf("") }
    var selectedDeptId by remember { mutableStateOf("") }
    var deptExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentStep == 1) "Register (Step 1 of 2)"
                        else "Additional Details (Step 2 of 2)"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) {
                                currentStep = 1
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentStep == 1) {
                // STEP 1 UI
                NightingaleTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                NightingaleTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                NightingaleTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                NightingaleTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = it }
                ) {
                    NightingaleTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = "Role",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                NightingalePrimaryButton(
                    text = "Next",
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || selectedRole.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("All Step 1 fields are required")
                            }
                            return@NightingalePrimaryButton
                        }
                        if (password != confirmPassword) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Passwords do not match")
                            }
                            return@NightingalePrimaryButton
                        }

                        if (selectedRole == "DOCTOR") {
                            viewModel.fetchDepartments()
                        }
                        currentStep = 2
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // STEP 2 UI
                if (selectedRole == "PATIENT") {
                    Text(
                        text = "Birthday",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NightingaleTextField(
                            value = birthYear,
                            onValueChange = { if (it.length <= 4) birthYear = it.filter { char -> char.isDigit() } },
                            label = "Year (YYYY)",
                            modifier = Modifier.weight(1f)
                        )
                        NightingaleTextField(
                            value = birthMonth,
                            onValueChange = { if (it.length <= 2) birthMonth = it.filter { char -> char.isDigit() } },
                            label = "Month (MM)",
                            modifier = Modifier.weight(1f)
                        )
                        NightingaleTextField(
                            value = birthDay,
                            onValueChange = { if (it.length <= 2) birthDay = it.filter { char -> char.isDigit() } },
                            label = "Day (DD)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = it }
                    ) {
                        NightingaleTextField(
                            value = selectedGender,
                            onValueChange = {},
                            readOnly = true,
                            label = "Gender",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genders.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = {
                                        selectedGender = g
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = bloodGroupExpanded,
                        onExpandedChange = { bloodGroupExpanded = it }
                    ) {
                        NightingaleTextField(
                            value = selectedBloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = "Blood Group",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = bloodGroupExpanded,
                            onDismissRequest = { bloodGroupExpanded = false }
                        ) {
                            bloodGroups.forEach { bg ->
                                DropdownMenuItem(
                                    text = { Text(bg) },
                                    onClick = {
                                        selectedBloodGroup = bg
                                        bloodGroupExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    NightingaleTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Address",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NightingaleTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = "Emergency Contact Number",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (selectedRole == "DOCTOR") {
                    NightingaleTextField(
                        value = specialization,
                        onValueChange = { specialization = it },
                        label = "Specialization",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NightingaleTextField(
                        value = qualification,
                        onValueChange = { qualification = it },
                        label = "Qualification",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NightingaleTextField(
                        value = experienceYearsStr,
                        onValueChange = { experienceYearsStr = it.filter { char -> char.isDigit() } },
                        label = "Experience (Years)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = deptExpanded,
                        onExpandedChange = { deptExpanded = it }
                    ) {
                        NightingaleTextField(
                            value = selectedDeptName,
                            onValueChange = {},
                            readOnly = true,
                            label = "Department",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false },
                            modifier = Modifier.heightIn(max = 160.dp)
                        ) {
                            if (viewModel.departments.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No departments available") },
                                    onClick = { deptExpanded = false }
                                )
                            } else {
                                viewModel.departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept.name) },
                                        onClick = {
                                            selectedDeptName = dept.name
                                            selectedDeptId = dept.departmentId
                                            deptExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                NightingalePrimaryButton(
                    text = "Complete Registration",
                    onClick = {
                        val baseUser = User(
                            userId = "",
                            name = name,
                            email = email,
                            role = selectedRole,
                            approved = selectedRole != "DOCTOR"
                        )

                        if (selectedRole == "PATIENT") {
                            val dobError = validateBirthday(birthYear, birthMonth, birthDay)
                            if (dobError != null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(dobError)
                                }
                                return@NightingalePrimaryButton
                            }
                            if (selectedGender.isBlank() || selectedBloodGroup.isBlank() || address.isBlank() || emergencyContact.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("All Patient details are required")
                                }
                                return@NightingalePrimaryButton
                            }

                            val y = birthYear.toInt()
                            val m = birthMonth.toInt()
                            val d = birthDay.toInt()
                            val dobStr = String.format("%04d-%02d-%02d", y, m, d)
                            val computedAge = calculateAge(y, m, d)

                            val patient = Patient(
                                dob = dobStr,
                                age = computedAge,
                                gender = selectedGender,
                                bloodGroup = selectedBloodGroup,
                                address = address,
                                emergencyContact = emergencyContact
                            )

                            viewModel.registerPatient(baseUser, patient, password) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Patient registered successfully", Toast.LENGTH_LONG).show()
                                    onRegisterSuccess()
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(error ?: "Registration failed")
                                    }
                                }
                            }
                        } else if (selectedRole == "DOCTOR") {
                            val exp = experienceYearsStr.toIntOrNull()
                            if (specialization.isBlank() || qualification.isBlank() || exp == null || selectedDeptId.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("All Doctor details (including Department) are required")
                                }
                                return@NightingalePrimaryButton
                            }

                            val doctor = Doctor(
                                specialization = specialization,
                                qualification = qualification,
                                experienceYears = exp,
                                departmentId = selectedDeptId
                            )

                            viewModel.registerDoctor(baseUser, doctor, password) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Doctor registered successfully (Pending Approval)", Toast.LENGTH_LONG).show()
                                    onRegisterSuccess()
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(error ?: "Registration failed")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun validateBirthday(birthYearStr: String, birthMonthStr: String, birthDayStr: String): String? {
    val year = birthYearStr.toIntOrNull() ?: return "Year must be a valid number"
    val month = birthMonthStr.toIntOrNull() ?: return "Month must be a valid number"
    val day = birthDayStr.toIntOrNull() ?: return "Day must be a valid number"

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    if (year < 1900 || year > currentYear) {
        return "Invalid year (must be between 1900 and $currentYear)"
    }
    if (month < 1 || month > 12) {
        return "Invalid month (must be between 1 and 12)"
    }

    val maxDaysInMonth = when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    if (day < 1 || day > maxDaysInMonth) {
        return "Invalid day for selected month (max $maxDaysInMonth days)"
    }

    val birthCalendar = Calendar.getInstance()
    birthCalendar.set(year, month - 1, day)
    val today = Calendar.getInstance()
    if (birthCalendar.after(today)) {
        return "Birthday cannot be in the future"
    }

    return null
}

fun calculateAge(year: Int, month: Int, day: Int): Int {
    val today = Calendar.getInstance()
    val birthDate = Calendar.getInstance().apply { set(year, month - 1, day) }
    var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return if (age < 0) 0 else age
}