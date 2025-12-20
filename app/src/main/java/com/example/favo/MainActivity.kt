package com.example.favo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 13+ 通知権限
        requestNotificationPermission()


        val plusButton = findViewById<View>(R.id.plusButton)
        plusButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }



        // 通知アクセス権限
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        showAccounts()
    }

    // ===============================
    // アカウント一覧表示（安全版）
    // ===============================
    private fun showAccounts() {
        val container = findViewById<LinearLayout>(R.id.accountContainer)
        container.removeAllViews()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val accounts =
            prefs.getStringSet("target_accounts", emptySet()) ?: emptySet()

        accounts.forEach { entry ->
            // entry 例:
            // "X:Bob"
            // "X:Bob|content://media/xxx"

            val view = layoutInflater.inflate(
                R.layout.item_account,
                container,
                false
            )

            val nameText = view.findViewById<TextView>(R.id.accountNameText)
            val userIcon = view.findViewById<ImageView>(R.id.userIcon)
            val snsIcon = view.findViewById<ImageView>(R.id.snsIcon)

            // ---------- 分解（超安全） ----------
            val mainParts = entry.split("|")
            val base = mainParts[0]              // X:Bob
            val iconUri = mainParts.getOrNull(1) // null or String

            val baseParts = base.split(":")
            if (baseParts.size != 2) return@forEach

            val sns = baseParts[0]
            val name = baseParts[1]

            nameText.text = name

// SNSアイコン
            snsIcon.setImageResource(
                if (sns == "LINE") R.drawable.line else R.drawable.x
            )

// ユーザーアイコン（丸）
            if (!iconUri.isNullOrEmpty()) {
                try {
                    userIcon.visibility = View.VISIBLE
                    userIcon.setBackground(null)
                    userIcon.setImageURI(Uri.parse(iconUri))
                    userIcon.clipToOutline = true
                } catch (e: Exception) {
                    userIcon.visibility = View.GONE
                }
            } else {
                userIcon.visibility = View.GONE
            }


            // 行クリック → 通知設定画面
            view.setOnClickListener {
                val intent = Intent(this, NotifyOptionActivity::class.java)
                intent.putExtra("account_entry", entry)
                startActivity(intent)
            }

            container.addView(view)
        }
    }

    // ===============================
    // 通知アクセス権限確認
    // ===============================
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(packageName)
    }

    // ===============================
    // Android 13+ 通知権限
    // ===============================
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}











