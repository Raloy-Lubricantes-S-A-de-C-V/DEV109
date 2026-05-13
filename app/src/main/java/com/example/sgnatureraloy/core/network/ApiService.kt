package com.example.sgnatureraloy.core.network

import com.example.sgnatureraloy.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/v1/autenticate")
    suspend fun authenticate(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/v1/notificaciones")
    suspend fun getPendingSignatures(@Body request: EmailRequest): Response<SignatureListResponse>

    @POST("api/v1/creadas")
    suspend fun getCreatedSignatures(@Body request: EmailRequest): Response<SignatureListResponse>
}
