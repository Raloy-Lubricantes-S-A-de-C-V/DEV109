package com.example.sgnatureraloy.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sgnatureraloy.core.session.SessionManager
import com.example.sgnatureraloy.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _otpState = MutableStateFlow<String?>(null)
    val otpState: StateFlow<String?> = _otpState.asStateFlow()

    fun login(user: String, pass: String) {
        Log.d("FIRMA", "LoginViewModel: Intentando login para usuario: $user")
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val fcmToken = sessionManager.getFcmToken()
                val response = repository.login(user, pass, fcmToken)
                val body = response.body()
                Log.d("FIRMA", "LoginViewModel: Respuesta recibida. Status: ${response.code()}")
                if (response.isSuccessful && body?.authData?.error == false) {
                    val key = body.authData?.key
                    if (key != null) {
                        Log.d("FIRMA", "LoginViewModel: Login exitoso. Guardando token y email.")
                        sessionManager.saveToken(key)
                        sessionManager.saveUserEmail(user)
                        _loginState.value = LoginUiState.Success
                    } else {
                        Log.e("FIRMA", "LoginViewModel: Error - No se recibió token (key)")
                        _loginState.value = LoginUiState.Error("No se recibió token de acceso")
                    }
                } else {
                    val errorMsj = body?.authData?.msj ?: "Error al iniciar sesión"
                    Log.e("FIRMA", "LoginViewModel: Error en login: $errorMsj")
                    _loginState.value = LoginUiState.Error(errorMsj)
                }
            } catch (e: Exception) {
                Log.e("FIRMA", "LoginViewModel: Excepción en login: ${e.message}", e)
                _loginState.value = LoginUiState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    fun requestOtp(email: String) {
        // Not supported in Signature API
    }

    fun resetOtpState() {
        _otpState.value = null
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
