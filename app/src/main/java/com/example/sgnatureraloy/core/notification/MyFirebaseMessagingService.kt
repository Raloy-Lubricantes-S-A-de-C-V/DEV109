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
import com.example.sgnatureraloy.core.session.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FIRMA", "FCM: Nuevo token generado: $token")
        val sessionManager = SessionManager(this)
        sessionManager.saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FIRMA", "FCM: Mensaje recibido de: ${remoteMessage.from}")

        // 1. Vibración de 5 segundos
        vibrateDevice(5000)

        // 2. Verificar si el mensaje contiene datos
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val refresh = data["refresh"]
            if (refresh == "true") {
                Log.d("FIRMA", "FCM: Disparando actualización automática")
                RefreshEventBus.triggerRefresh()
            }
        }

        // 3. Verificar si el mensaje contiene una notificación
        remoteMessage.notification?.let {
            Log.d("FIRMA", "FCM: Cuerpo de notificación: ${it.body}")
            showNotification(it.title ?: "Firma Digital Raloy", it.body ?: "")
        } ?: run {
            // Si es un "data message" sin notificación, mostrar una genérica
            showNotification("Firma Digital Raloy", "Actualización de documentos recibida.")
        }
    }

    private fun vibrateDevice(duration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "FirmaNotificaciones"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal en Android 8.0 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Firma",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
