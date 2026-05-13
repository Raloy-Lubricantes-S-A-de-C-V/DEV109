package com.example.sgnatureraloy.data.model

import com.google.gson.annotations.SerializedName

data class SignatureProcess(
    @SerializedName("reference_id") val referenceId: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("owner_email") val ownerEmail: String?,
    @SerializedName("token_acceso") val tokenAcceso: String?,
    @SerializedName("firmantes") val firmantes: List<Signer>? = null,
    @SerializedName("token_firmante") val tokenFirmante: String? = null,
    @SerializedName("requires_my_signature") val requiresMySignature: Boolean = false
)

data class Signer(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("status") val status: String? = null,
    @SerializedName("fecha_firma") val fechaFirma: String? = null,
    @SerializedName("token_firmante") val tokenFirmante: String? = null
)

data class SignatureListResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("msj") val msj: String,
    @SerializedName("count") val count: Int,
    @SerializedName("data") val data: List<SignatureProcess>,
    @SerializedName("status") val status: Int
)

data class AuthRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val authData: AuthData?
)

data class AuthData(
    @SerializedName("error") val error: Boolean,
    @SerializedName("msj") val msj: String,
    @SerializedName("key") val key: String?
)

data class EmailRequest(
    @SerializedName("email") val email: String
)
