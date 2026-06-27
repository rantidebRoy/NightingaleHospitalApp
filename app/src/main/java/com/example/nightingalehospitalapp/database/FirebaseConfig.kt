package com.example.nightingalehospitalapp.database

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseConfig {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val usersRef: CollectionReference = firestore.collection("users")

    val doctorsRef: CollectionReference = firestore.collection("doctors")

    val patientsRef: CollectionReference = firestore.collection("patients")

    val departmentsRef: CollectionReference = firestore.collection("departments")

    val bedsRef: CollectionReference = firestore.collection("beds")

    val operationTheatresRef: CollectionReference = firestore.collection("operation_theatres")

    val appointmentsRef: CollectionReference = firestore.collection("appointments")

    val prescriptionsRef: CollectionReference = firestore.collection("prescriptions")

    val medicinesRef: CollectionReference = firestore.collection("medicine")

    val testBookingsRef: CollectionReference = firestore.collection("test_bookings")

    val testResultsRef: CollectionReference = firestore.collection("test_results")

    val surgeryBookingsRef: CollectionReference = firestore.collection("surgery_bookings")

    val notificationsRef: CollectionReference = firestore.collection("notifications")

    val admissionsRef: CollectionReference = firestore.collection("admissions")
    
    val diagnosticTestsRef: CollectionReference = firestore.collection("diagnostic_tests")

}