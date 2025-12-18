package com.example.favo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES

class LineNotificationService : NotificationListenerService() {

    private var lastNotificationKey: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("X_NOTIFY", "NotificationService CREATED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // ===== 重複防止 =====
        if (sbn.key == lastNotificationKey) return
        lastNotificationKey = sbn.key

        // X（旧Twitter）以外は無視
        if (sbn.packageName != "com.twitter.android") return

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val targetName = prefs.getString("target_account", "")?.trim() ?: ""
        val flashEnabled = prefs.getBoolean("flash_enabled", true)
        val soundType = prefs.getString("sound_type", "lover") ?: "lover"

        if (targetName.isEmpty()) {
            Log.d("X_NOTIFY", "targetName is empty")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim() ?: ""

        Log.d("X_NOTIFY", "title='$title'")
        Log.d("X_NOTIFY", "targetName='$targetName'")

        // 表示名一致チェック
        if (!title.equals(targetName, ignoreCase = true)) return

        Log.d("X_NOTIFY", "MATCHED → FAVO NOTIFY")

        // ========= 🔔 Favoとして通知 =========
        sendFavoNotification(
            targetName = targetName,
            soundType = soundType
        )

        // ========= 🔊 通知音 =========
        val soundRes = when (soundType) {
            "oshi" -> R.raw.notification_sound_oshi
            else -> R.raw.notification_sound_lover
        }

        MediaPlayer.create(this, soundRes)?.apply {
            start()
            setOnCompletionListener { it.release() }
        }

        // ========= 🔦 フラッシュ =========
        if (flashEnabled) {
            try {
                val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList.firstOrNull()

                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, true)
                    Thread {
                        Thread.sleep(500)
                        cameraManager.setTorchMode(cameraId, false)
                    }.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ===============================
    // Favo名義で通知を出す処理
    // ===============================
    private fun sendFavoNotification(
        targetName: String,
        soundType: String
    ) {

        val channelId = "favo_notify"

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (SDK_INT >= VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Favo通知",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }


        val iconRes = when (soundType) {
            "oshi" -> R.drawable.ic_notify_star   // 🌟
            else -> R.drawable.ic_notify_heart    // 💖
        }

        val titleEmoji = if (soundType == "oshi") "🌟" else "💖"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconRes)
            .setColor(
                if (soundType == "oshi") 0xFFFFD700.toInt()
                else 0xFFE91E63.toInt()
            )
            .setContentTitle("Favo $titleEmoji")
            .setContentText("$targetName から通知があります $titleEmoji")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}





