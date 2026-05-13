package com.example.sgnatureraloy.core.notification

import android.util.Log
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

        // Verificar si el mensaje contiene datos
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val refresh = data["refresh"]
            if (refresh == "true") {
                Log.d("FIRMA", "FCM: Disparando actualización automática")
                RefreshEventBus.triggerRefresh()
            }
        }

        // Verificar si el mensaje contiene una notificación
        remoteMessage.notification?.let {
            Log.d("FIRMA", "FCM: Cuerpo de notificación: ${it.body}")
        }
    }
}
