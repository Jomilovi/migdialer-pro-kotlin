package com.migdialer.pro.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.migdialer.pro.R
import com.migdialer.pro.ui.incall.InCallActivity
import com.migdialer.pro.ui.dialer.IncomingCallActivity

class MigDialerConnectionService : InCallService() {

    companion object {
        private const val TAG = "MigDialerConn"
        private const val NOTIFICATION_CHANNEL_ID = "migdialer_calls"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded: state=${call.state}")
        CallStateManager.onCallAdded(call)

        val uiIntent: Intent = when (call.state) {
            Call.STATE_RINGING -> {
                Intent(this, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }
            else -> {
                Intent(this, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
        }

        try {
            startActivity(uiIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call UI: ${e.message}")
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved")
        CallStateManager.onCallRemoved(call)
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Llamadas activas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de llamadas en curso"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun buildCallNotification(callerName: String, isIncoming: Boolean): Notification {
        val tapIntent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (isIncoming) "Llamada entrante" else "En llamada"
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(title)
            .setContentText(callerName)
            .setContentIntent(tapPending)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_CALL)
            .build()
    }
}
