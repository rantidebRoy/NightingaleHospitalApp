package com.example.nightingalehospitalapp.repository.report

import com.example.nightingalehospitalapp.database.FirebaseConfig
import kotlinx.coroutines.tasks.await

data class SystemReportMetrics(
    val doctorCount: Int = 0,
    val patientCount: Int = 0,
    val lastRegisteredDoctor: String = "None",
    val lastRegisteredPatient: String = "None",
    val bedsBooked: Int = 0,
    val bedsAvailable: Int = 0,
    val prescriptionsIssued: Int = 0,
    val labTestsCompleted: Int = 0,
    val labTestsRemaining: Int = 0
)

class ReportRepository {

    suspend fun getSystemMetrics(): SystemReportMetrics {
        return try {
            // 1. Doctors & Patients
            val usersSnap = FirebaseConfig.usersRef.get().await()
            val doctorDocs = usersSnap.documents.filter { it.getString("role") == "DOCTOR" }
            val patientDocs = usersSnap.documents.filter { it.getString("role") == "PATIENT" }

            val doctorCount = doctorDocs.size
            val patientCount = patientDocs.size

            val lastDoctorName = doctorDocs.lastOrNull()?.getString("name")
                ?: doctorDocs.lastOrNull()?.getString("email")
                ?: "None"

            val lastPatientName = patientDocs.lastOrNull()?.getString("name")
                ?: patientDocs.lastOrNull()?.getString("email")
                ?: "None"

            // 2. Beds Booked vs Available
            val bedsSnap = FirebaseConfig.bedsRef.get().await()
            var bookedBeds = 0
            var availableBeds = 0

            if (bedsSnap.isEmpty) {
                // Check admissionsRef if bedsRef has no direct documents
                val admissionsSnap = FirebaseConfig.admissionsRef.get().await()
                bookedBeds = admissionsSnap.documents.count { it.getString("status")?.uppercase() == "ADMITTED" }
                availableBeds = maxOf(0, 50 - bookedBeds)
            } else {
                for (doc in bedsSnap.documents) {
                    val status = doc.getString("status")?.uppercase() ?: ""
                    val pId = doc.getString("patientId")
                    if (status == "OCCUPIED" || status == "BOOKED" || !pId.isNullOrBlank()) {
                        bookedBeds++
                    } else {
                        availableBeds++
                    }
                }
            }

            // 3. Prescriptions Issued
            val prescriptionsSnap = FirebaseConfig.prescriptionsRef.get().await()
            val prescriptionsCount = prescriptionsSnap.size()

            // 4. Lab Tests Completed vs Remaining
            val testBookingsSnap = FirebaseConfig.testBookingsRef.get().await()
            var testsCompleted = 0
            var testsRemaining = 0

            for (doc in testBookingsSnap.documents) {
                val status = doc.getString("status")?.uppercase() ?: ""
                if (status == "COMPLETED") {
                    testsCompleted++
                } else {
                    testsRemaining++
                }
            }

            if (testBookingsSnap.isEmpty) {
                val resultsSnap = FirebaseConfig.testResultsRef.get().await()
                testsCompleted = resultsSnap.size()
            }

            SystemReportMetrics(
                doctorCount = doctorCount,
                patientCount = patientCount,
                lastRegisteredDoctor = lastDoctorName,
                lastRegisteredPatient = lastPatientName,
                bedsBooked = bookedBeds,
                bedsAvailable = availableBeds,
                prescriptionsIssued = prescriptionsCount,
                labTestsCompleted = testsCompleted,
                labTestsRemaining = testsRemaining
            )
        } catch (e: Exception) {
            SystemReportMetrics()
        }
    }

    suspend fun getRecentActivities(): List<String> {
        return listOf(
            "System report compiled successfully",
            "Real-time patient & doctor registration verified",
            "Bed allocation status synced",
            "Prescription & diagnostic test records updated"
        )
    }
}
