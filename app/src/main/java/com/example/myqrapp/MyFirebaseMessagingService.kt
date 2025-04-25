package com.example.myqrapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

const val channelId = "notification_channel"
const val channelName = "nu_conteaza_nume_channel"
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        if (remoteMessage.getNotification() != null) {
            //null safe
            generateNotification(
                remoteMessage.notification!!.title!!,
                remoteMessage.notification!!.body!!,
                Context.NOTIFICATION_SERVICE
            )
        }
    }

    private fun getRemoteView(title: String, description: String) : RemoteViews {

        val remoteView = RemoteViews("com.example.myqrapp", R.layout.notification)
        remoteView.setTextViewText(R.id.notification_title, title)
        remoteView.setTextViewText(R.id.description, description)
        return remoteView
    }

    fun generateNotification(title: String, description: String, notificationService: String): Boolean {
        val intent = Intent(this, MainActivity::class.java)
        //CLEAR all the activity stck and put it on top
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        // channel id si channel name
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.qr_photo)
            .setVibrate(longArrayOf(1000,1000,1000,1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder.setContent(getRemoteView(title, description))

        val notificationManager = getSystemService(notificationService) as NotificationManager

        // Check if the Android Version is greater than Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(0, builder.build())

        return true
    }
}