package com.shterneregen.securelan.androidclient.call

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.*
import androidx.core.content.ContextCompat
import com.shterneregen.securelan.androidclient.MainActivity
import com.shterneregen.securelan.androidclient.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/** Promotion is acknowledged before the coordinator opens microphone/camera. Not restarted after process death. */
class CallForegroundService : Service() {
    private var wake: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null
    private var owned: Request? = null
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = pending
        if (request == null || intent?.getStringExtra("id") != request.id) {
            if (owned == null) stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            "end" -> request.end()
            "mute" -> { request.mute(); showNotification(request) }
            else -> {
                owned = request
                try {
                    showNotification(request)
                    if (wake == null) {
                        wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SecureLan:Call").apply { acquire(4 * 60 * 60 * 1000L) }
                        @Suppress("DEPRECATION")
                        wifi = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SecureLan:Call").apply { acquire() }
                    }
                    request.ready.complete(Unit)
                } catch (error: Exception) { request.ready.completeExceptionally(error); stopSelf() }
            }
        }
        return START_NOT_STICKY
    }
    private fun showNotification(request: Request) {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.call_channel), NotificationManager.IMPORTANCE_LOW))
        fun action(name: String, code: Int) = PendingIntent.getService(this, code,
            Intent(this, CallForegroundService::class.java).setAction(name).putExtra("id", request.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 40, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(getString(R.string.call_channel)).setContentText(request.peer)
            .setContentIntent(open).setOngoing(true).setCategory(Notification.CATEGORY_CALL)
            .addAction(Notification.Action.Builder(null, getString(R.string.call_mute), action("mute", 41)).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.call_end), action("end", 42)).build()).build()
        if (Build.VERSION.SDK_INT >= 30) startForeground(ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or if (request.video) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0)
        else startForeground(ID, notification)
    }
    override fun onDestroy() {
        if (wake?.isHeld == true) wake?.release()
        if (wifi?.isHeld == true) wifi?.release()
        val request = owned
        if (request != null && pending === request) {
            pending = null
            request.ready.completeExceptionally(IllegalStateException("Call service stopped"))
            request.end()
        }
        owned = null
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL = "secure_lan_calls"
        private const val ID = 5052
        private data class Request(val id: String, val peer: String, val video: Boolean, val end: () -> Unit,
            val mute: () -> Unit, val ready: CompletableDeferred<Unit> = CompletableDeferred())
        private var pending: Request? = null
        suspend fun start(context: Context, id: String, peer: String, video: Boolean, end: () -> Unit, mute: () -> Unit) {
            val request = Request(id, peer, video, end, mute)
            pending = request
            try {
                ContextCompat.startForegroundService(context, Intent(context, CallForegroundService::class.java).putExtra("id", id))
                withTimeout(8_000) { request.ready.await() }
            } catch (error: Exception) { if (pending === request) stop(context); throw error }
        }
        fun stop(context: Context) {
            pending = null
            context.stopService(Intent(context, CallForegroundService::class.java))
        }
    }
}
