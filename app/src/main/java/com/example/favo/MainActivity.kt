package com.example.favo

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var accountListText: TextView
    private lateinit var plusButton: ImageButton
    private lateinit var settingButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accountListText = findViewById(R.id.accountListText)
        plusButton = findViewById(R.id.plusButton)
        settingButton = findViewById(R.id.settingButton)

        // ＋ → 初回登録
        plusButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ⚙ → 通知設定
        settingButton.setOnClickListener {
            startActivity(Intent(this, NotifyOptionActivity::class.java))
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

        if (account.isNullOrEmpty()) {
            // 未登録
            accountListText.text = getString(R.string.no_account)
            plusButton.visibility = View.VISIBLE
            settingButton.visibility = View.GONE
        } else {
            // 登録済み
            accountListText.text = getString(R.string.registered_account, account)
            plusButton.visibility = View.GONE
            settingButton.visibility = View.VISIBLE
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







