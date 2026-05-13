package com.example.sgnatureraloy.data.repository

import com.example.sgnatureraloy.core.network.ApiService
import com.example.sgnatureraloy.data.model.AuthRequest
import com.example.sgnatureraloy.data.model.AuthResponse
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {

    suspend fun login(user: String, pass: String): Response<AuthResponse> {
        return apiService.authenticate(AuthRequest(user, pass))
    }
}
