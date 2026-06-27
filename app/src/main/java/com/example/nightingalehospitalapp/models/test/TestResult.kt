package com.example.nightingalehospitalapp.models.test

data class TestResult(
    val testId: String = "",
    val patientId: String = "",
    val date: String = "",
    val results: List<TestEntry> = emptyList()
)

data class TestEntry(
    val problem: String = "",
    val result: String = ""
)
