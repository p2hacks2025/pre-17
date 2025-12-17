package com.example.favo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    // commit
    private lateinit var accountListText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accountListText = findViewById(R.id.accountListText)

        // ＋ボタン → 設定画面
        val plusButton = findViewById<ImageButton>(R.id.plusButton)
        plusButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 通知アクセス確認
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val account = prefs.getString("target_account", "")

        accountListText.text =
            if (account.isNullOrEmpty()) {
                "登録アカウント：なし"
            } else {
                "登録アカウント：$account"
            }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(packageName)
    }
}






