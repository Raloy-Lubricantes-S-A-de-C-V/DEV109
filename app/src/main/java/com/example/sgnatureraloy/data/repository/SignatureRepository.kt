package com.example.sgnatureraloy.data.repository

import android.util.Log
import com.example.sgnatureraloy.core.network.ApiService
import com.example.sgnatureraloy.data.model.EmailRequest
import com.example.sgnatureraloy.data.model.SignatureProcess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SignatureRepository(private val apiService: ApiService) {

    fun getSignatures(email: String): Flow<List<SignatureProcess>> = flow {
        Log.d("FIRMA", "SignatureRepository: Solicitando firmas para $email")
        
        val pendingRes = apiService.getPendingSignatures(EmailRequest(email))
        Log.d("FIRMA", "SignatureRepository: Respuesta pendientes: ${pendingRes.code()}")
        
        val createdRes = apiService.getCreatedSignatures(EmailRequest(email))
        Log.d("FIRMA", "SignatureRepository: Respuesta creadas: ${createdRes.code()}")

        val allSignatures = mutableListOf<SignatureProcess>()

        if (pendingRes.isSuccessful) {
            val pending = pendingRes.body()?.data ?: emptyList()
            Log.d("FIRMA", "SignatureRepository: ${pending.size} pendientes recibidas")
            allSignatures.addAll(pending)
        } else {
            Log.e("FIRMA", "SignatureRepository: Error en pendientes: ${pendingRes.errorBody()?.string()}")
        }

        if (createdRes.isSuccessful) {
            val list = createdRes.body()?.data ?: emptyList()
            Log.d("FIRMA", "SignatureRepository: ${list.size} creadas recibidas")
            // Avoid duplicates if any
            val existingIds = allSignatures.map { s -> s.referenceId }.toSet()
            allSignatures.addAll(list.filter { s -> s.referenceId !in existingIds })
        } else {
            Log.e("FIRMA", "SignatureRepository: Error en creadas: ${createdRes.errorBody()?.string()}")
        }

        // Sort: COMPLETED (CERRADO) at the end, then by date descending
        val sorted = allSignatures.sortedWith(compareBy<SignatureProcess> {
            it.status == "COMPLETED" || it.status == "CERRADO"
        }.thenByDescending { it.createdAt })

        Log.d("FIRMA", "SignatureRepository: Total procesado: ${sorted.size} firmas")
        emit(sorted)
    }
}
