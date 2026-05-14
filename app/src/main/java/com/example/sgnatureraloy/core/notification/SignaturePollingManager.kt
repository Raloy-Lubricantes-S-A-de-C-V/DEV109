package com.example.sgnatureraloy.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sgnatureraloy.MainActivity
import com.example.sgnatureraloy.R
import com.example.sgnatureraloy.core.events.RefreshEventBus
import com.example.sgnatureraloy.core.network.ApiService
import com.example.sgnatureraloy.core.session.SessionManager
import com.example.sgnatureraloy.data.model.EmailRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class SignaturePollingManager(
    private val context: Context,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    
    companion object {
        private const val POLL_INTERVAL_MS = 60000L // 1 minuto
        private const val CHANNEL_ID = "signatures_polling_notifications"
        private const val CHANNEL_NAME = "Notificaciones de Firma"
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        
        Log.d("FIRMA", "PollingManager: Iniciando polling cada 1 min")
        pollingJob = scope.launch {
            while (isActive) {
                val email = sessionManager.getUserEmail()
                if (email != null && email.isNotEmpty() && email != "null") {
                    try {
                        Log.d("FIRMA", "PollingManager: Consultando notificaciones para $email")
                        val response = apiService.checkNotifications(EmailRequest(email))
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body?.hasNewSignature == true) {
                                Log.d("FIRMA", "PollingManager: ¡Nueva firma detectada!")
                                triggerAlert()
                                RefreshEventBus.triggerRefresh()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FIRMA", "PollingManager: Error en polling: ${e.message}")
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        Log.d("FIRMA", "PollingManager: Deteniendo polling")
        pollingJob?.cancel()
    }

    private fun triggerAlert() {
        vibrateDevice(5000)
        showNotification(
            "Nueva Solicitud de Firma",
            "Tienes un nuevo documento pendiente por firmar."
        )
    }

    private fun vibrateDevice(duration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
