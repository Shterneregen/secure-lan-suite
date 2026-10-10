package com.shterneregen.securelan.androidclient.call

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.shterneregen.securelan.androidclient.MainActivity
import com.shterneregen.securelan.androidclient.R

/** A normal notification may announce an offer in the background; media waits for a visible Activity. */
class IncomingCallNotification(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    fun update(call: CallState) {
        if (call.phase != CallPhase.INCOMING) { clear(); return }
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.call_incoming), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build())
        })
        val open = PendingIntent.getActivity(context, ID, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(ID, Notification.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(context.getString(R.string.call_incoming)).setContentText(call.peer)
            .setContentIntent(open).setCategory(Notification.CATEGORY_CALL).setOngoing(true)
            .setTimeoutAfter(45_000).build())
    }
    fun clear() { manager.cancel(ID) }
    companion object { private const val CHANNEL = "secure_lan_incoming_calls"; private const val ID = 5053 }
}
