package com.example.favo

import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class LineNotificationService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("X_NOTIFY", "NotificationService CREATED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // X（旧Twitter）以外は無視
        if (sbn.packageName != "com.twitter.android") return

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val targetName = prefs.getString("target_account", "")?.trim() ?: ""

        if (targetName.isEmpty()) {
            Log.d("X_NOTIFY", "targetName is empty")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")
            ?.toString()
            ?.trim()
            ?: ""

        Log.d("X_NOTIFY", "title='$title'")
        Log.d("X_NOTIFY", "targetName='$targetName'")

        if (!title.equals(targetName, ignoreCase = true)) return

        Log.d("X_NOTIFY", "MATCHED → SOUND & FLASH")

        // 🔊 音
        val mediaPlayer = MediaPlayer.create(this, R.raw.notification_sound3)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener { it.release() }

        // 🔦 フラッシュ
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


