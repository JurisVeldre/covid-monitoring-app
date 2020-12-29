package com.example.covid_monitoring_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.covid_monitoring_app.activities.RangingActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageInstance: FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        val intent = Intent(applicationContext, RangingActivity.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val title = p0.data["title"].toString()
        val text = p0.data["body"].toString()
        val channelId = "Default"

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.danger_icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "High Channel", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = builder.build()
        manager.notify(0, notification)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.i("RangingActivity", p0.toString())
    }
}