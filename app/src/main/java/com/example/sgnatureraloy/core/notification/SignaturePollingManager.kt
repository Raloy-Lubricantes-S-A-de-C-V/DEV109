package com.example.sgnatureraloy.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sgnatureraloy.MainActivity
import com.example.sgnatureraloy.core.events.RefreshEventBus
import com.example.sgnatureraloy.core.network.ApiService
import com.example.sgnatureraloy.core.session.SessionManager
import com.example.sgnatureraloy.data.model.EmailRequest
import kotlinx.coroutines.*

class SignaturePollingManager(
    private val context: Context,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    
    companion object {
        private const val BASE_POLL_INTERVAL_MS = 60000L // 1 minuto base
        private const val MAX_RETRY_INTERVAL_MS = 300000L // Máximo 5 minutos en caso de error persistente
        private const val CHANNEL_ID = "signatures_polling_notifications"
        private const val CHANNEL_NAME = "Notificaciones de Firma"
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        
        Log.d("FIRMA", "--- [SISTEMA RESILIENTE INICIADO] ---")
        
        pollingJob = scope.launch {
            var currentInterval = BASE_POLL_INTERVAL_MS
            var cycleCount = 1

            while (isActive) {
                if (!isNetworkAvailable()) {
                    Log.w("FIRMA", "⚠️ [MODO AHORRO] Sin internet. Esperando conexión...")
                    delay(BASE_POLL_INTERVAL_MS)
                    continue
                }

                val email = sessionManager.getUserEmail()
                if (email != null && email.isNotEmpty() && email != "null") {
                    try {
                        val startTime = System.currentTimeMillis()
                        Log.d("FIRMA", ">>>> [CICLO #$cycleCount] Consultando a DEV108 para: $email")
                        
                        val response = apiService.checkNotifications(EmailRequest(email))
                        val duration = System.currentTimeMillis() - startTime
                        
                        if (response.isSuccessful) {
                            // Resetear intervalo si la conexión fue exitosa
                            currentInterval = BASE_POLL_INTERVAL_MS
                            val body = response.body()
                            Log.d("FIRMA", "<<<< [EXITO] DEV108 respondió en ${duration}ms. Novedades: ${body?.hasNewSignature}")
                            
                            if (body?.hasNewSignature == true) {
                                Log.d("FIRMA", "🚨 [ALERTA] ¡Acción requerida! Disparando protocolo de urgencia.")
                                triggerAlert()
                                RefreshEventBus.triggerRefresh()
                            }
                        } else {
                            Log.e("FIRMA", "⚠️ [ERROR SERVIDOR] Código: ${response.code()}. Reintentando en ciclo normal.")
                        }
                    } catch (e: Exception) {
                        Log.e("FIRMA", "❌ [FALLA DE RED] ${e.message}. Aplicando reintento exponencial.")
                        // Aumentar intervalo si hay fallas persistentes (evitar drenar batería)
                        currentInterval = (currentInterval * 1.5).toLong().coerceAtMost(MAX_RETRY_INTERVAL_MS)
                    }
                }
                
                cycleCount++
                Log.d("FIRMA", "⏳ [PRÓXIMA CONSULTA] En ${currentInterval / 1000}s")
                delay(currentInterval)
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
