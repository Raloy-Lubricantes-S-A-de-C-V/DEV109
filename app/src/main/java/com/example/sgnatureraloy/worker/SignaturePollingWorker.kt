package com.example.sgnatureraloy.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sgnatureraloy.core.network.RetrofitClient
import com.example.sgnatureraloy.core.session.SessionManager
import com.example.sgnatureraloy.data.model.EmailRequest
import com.example.sgnatureraloy.utils.NotificationUtils

class SignaturePollingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val email = sessionManager.getUserEmail() ?: return Result.failure()
        
        val apiService = RetrofitClient.getApiService(applicationContext)
        
        return try {
            val response = apiService.getPendingSignatures(EmailRequest(email))
            if (response.isSuccessful) {
                val pending = response.body()?.data ?: emptyList()
                
                // Simplified logic: if there are any pending, notify.
                // In a real app, we would compare with a local database to notify only for NEW ones.
                if (pending.isNotEmpty()) {
                    NotificationUtils.showNotification(
                        applicationContext,
                        "Firmas Pendientes",
                        "Tienes ${pending.size} documento(s) esperando tu firma."
                    )
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
