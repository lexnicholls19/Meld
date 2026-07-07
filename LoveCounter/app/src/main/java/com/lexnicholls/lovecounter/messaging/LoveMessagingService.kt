package com.lexnicholls.lovecounter.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import com.lexnicholls.lovecounter.MainActivity
import com.lexnicholls.lovecounter.R

class LoveMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Optionally send token to your server if not using topics
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("LoveFCM", "--- NUEVO MENSAJE RECIBIDO ---")
        Log.d("LoveFCM", "De: ${remoteMessage.from}")
        
        val notification = remoteMessage.notification
        if (notification != null) {
            Log.d("LoveFCM", "¡AVISO! El mensaje contiene un objeto 'notification' del sistema.")
            Log.d("LoveFCM", "Título Notif: ${notification.title}, Cuerpo: ${notification.body}")
        }

        // Retrieve local info for filtering
        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val localUserName = sharedPrefs.getString("user_name", "")?.trim()
        val localDeviceId = sharedPrefs.getString("device_id", "")?.trim()

        // Extract data
        val data = remoteMessage.data
        Log.d("LoveFCM", "Payload de Datos COMPLETO: $data")
        Log.d("LoveFCM", "Todas las llaves disponibles: ${data.keys.joinToString(", ")}")

        val senderName = (data["senderName"] ?: data["userName"] ?: data["sender_name"] ?: data["user"])?.trim()
        val senderId = (data["senderId"] ?: data["deviceId"] ?: data["sender_id"] ?: data["uid"])?.trim()
        val senderUid = data["senderUid"]?.trim()
        
        val localUid = FirebaseAuth.getInstance().currentUser?.uid
        
        Log.d("LoveFCM", "Comparando:")
        Log.d("LoveFCM", "  > SenderName: '$senderName' vs LocalUser: '$localUserName'")
        Log.d("LoveFCM", "  > SenderId: '$senderId' vs LocalId: '$localDeviceId'")
        Log.d("LoveFCM", "  > SenderUid: '$senderUid' vs LocalUid: '$localUid'")

        // 1. Filter: Ignore our own actions (by Name, Device ID or UID)
        val isOwnNotificationByName = !senderName.isNullOrBlank() && !localUserName.isNullOrBlank() && 
                senderName.equals(localUserName, ignoreCase = true)
        
        val isOwnNotificationById = !senderId.isNullOrBlank() && !localDeviceId.isNullOrBlank() && 
                senderId.equals(localDeviceId, ignoreCase = true)
        
        val isOwnNotificationByUid = !senderUid.isNullOrBlank() && !localUid.isNullOrBlank() &&
                senderUid == localUid

        if (isOwnNotificationByName || isOwnNotificationById || isOwnNotificationByUid) {
            Log.d("LoveFCM", "IGNORANDO: Es una notificación propia (Match Nombre: $isOwnNotificationByName, Match ID: $isOwnNotificationById, Match UID: $isOwnNotificationByUid)")
            return
        }

        // 2. Determine title and body
        val title = data["name"] ?: remoteMessage.notification?.title
        val body = data["value"] ?: remoteMessage.notification?.body

        if (title != null && body != null) {
            Log.d("LoveFCM", "MOSTRANDO: $title - $body")
            sendNotification(title, body)
        } else {
            Log.d("LoveFCM", "ERROR: Notificación sin título o cuerpo, ignorando")
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "reminders_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android 8.0+
        val channel = NotificationChannel(
            channelId,
            "Recordatorios",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal para recordatorios de amor"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_heart) // Using a heart icon
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Match channel importance
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        Log.d("FCM", "Mostrando notificación: $title - $message")
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
