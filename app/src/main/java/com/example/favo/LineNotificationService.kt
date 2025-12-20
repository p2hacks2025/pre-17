package com.example.favo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat

class LineNotificationService : NotificationListenerService() {

    // ===== 重複防止用 =====
    private val recentNotifications = mutableMapOf<String, Long>()
    private val DUPLICATE_INTERVAL = 1500L // 1.5秒

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // ===== グループサマリー無視 =====
        if (
            sbn.notification.flags and
            Notification.FLAG_GROUP_SUMMARY != 0
        ) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // ===== 通知識別キー =====
        val uniqueKey = "${sbn.packageName}|$title|$text"

        val now = System.currentTimeMillis()
        val lastTime = recentNotifications[uniqueKey]

        if (lastTime != null && now - lastTime < DUPLICATE_INTERVAL) {
            // 🚫 短時間の同一通知 → 無視
            return
        }

        recentNotifications[uniqueKey] = now

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val targets = prefs.getStringSet("target_accounts", emptySet()) ?: return

        for (entry in targets) {

            // entry: "X:Bob|uri"
            val base = entry.split("|")[0]
            val parts = base.split(":")
            if (parts.size != 2) continue

            val sns = parts[0]
            val name = parts[1]

            when (sns) {

                "X" -> {
                    if (sbn.packageName != "com.twitter.android") continue
                    if (!title.equals(name, ignoreCase = true)) continue
                }

                "LINE" -> {
                    if (sbn.packageName != "jp.naver.line.android") continue
                    if (
                        !title.contains(name, ignoreCase = true) &&
                        !text.contains(name, ignoreCase = true)
                    ) continue
                }

                else -> continue
            }

            Log.d("FAVO", "MATCHED $sns : $name")

            val soundType =
                prefs.getString("sound_type_$base", "lover") ?: "lover"
            val flashEnabled =
                prefs.getBoolean("flash_$base", true)

            sendFavoNotification(sns)
            playSound(soundType)
            if (flashEnabled) flash()

            break
        }
    }

    // ===============================
    // Favo通知
    // ===============================
    private fun sendFavoNotification(sns: String) {

        val channelId = "favo_notify"
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Favo通知",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = when (sns) {
            "LINE" -> Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://line.me")
                setPackage("jp.naver.line.android")
            }
            else -> Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://x.com")
                setPackage("com.twitter.android")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notify_heart)
            .setContentTitle("Favo")
            .setContentText("通知があります")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ===============================
    // 音
    // ===============================
    private fun playSound(type: String) {
        val res =
            if (type == "oshi")
                R.raw.notification_sound_oshi
            else
                R.raw.notification_sound_lover

        MediaPlayer.create(this, res)?.apply {
            start()
            setOnCompletionListener { release() }
        }
    }

    // ===============================
    // フラッシュ
    // ===============================
    private fun flash() {
        try {
            val cameraManager =
                getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return

            cameraManager.setTorchMode(cameraId, true)
            Thread.sleep(400)
            cameraManager.setTorchMode(cameraId, false)
        } catch (_: Exception) {
        }
    }
}
