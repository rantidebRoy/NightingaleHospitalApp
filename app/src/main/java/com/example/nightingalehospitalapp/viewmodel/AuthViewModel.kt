package com.example.nightingalehospitalapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nightingalehospitalapp.models.user.User
import com.example.nightingalehospitalapp.repository.auth.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    fun registerUser(
        user: User,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        repository.registerUser(user, password, callback)
    }

    fun loginUser(
        email: String,
        password: String,
        callback: (String?, String?) -> Unit
    ) {
        repository.loginUser(email, password, callback)
    }

    fun checkSession(callback: (String?, String?) -> Unit) {
        repository.checkSession(callback)
    }

    fun logoutUser() {
        repository.logoutUser()
    }
}