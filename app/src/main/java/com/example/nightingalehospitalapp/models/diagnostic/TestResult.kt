package com.example.nightingalehospitalapp.models.diagnostic

data class TestResultItem(
    val problem: String = "",
    val result: String = ""
)

data class TestResult(
    val resultId: String = "",
    val bookingId: String = "",
    val patientId: String = "",
    val reportUrl: String = "",
    val resultDescription: String = "",
    val date: String = "",
    val results: List<TestResultItem> = emptyList()
)