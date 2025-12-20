package com.example.favo

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupSplashScreen(splashScreen)
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

    private fun setupSplashScreen(splashScreen: SplashScreen) {
        splashScreen.setKeepOnScreenCondition { isLoading }
        // NOTE: This is for demo purposes. In a real app, you'd wait for data to load.
        Handler(Looper.getMainLooper()).postDelayed({ isLoading = false }, 1500)

        splashScreen.setOnExitAnimationListener { splashScreenProvider ->
            val splashView = splashScreenProvider.view
            val contentView = findViewById<View>(android.R.id.content)

            val fadeOut = ObjectAnimator.ofFloat(splashView, "alpha", 1f, 0f).apply {
                interpolator = AccelerateInterpolator()
                duration = 400L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenProvider.remove()
                    }
                })
            }

            contentView.alpha = 0f
            val fadeIn = ObjectAnimator.ofFloat(contentView, "alpha", 0f, 1f).apply {
                interpolator = AccelerateInterpolator()
                duration = 400L
            }

            fadeOut.start()
            fadeIn.start()
        }
    }

    override fun onResume() {
        super.onResume()
        showAccounts()
    }

    // ===============================
    // アカウント一覧表示
    // ===============================
    private fun showAccounts() {
        val containerTop = findViewById<LinearLayout>(R.id.accountContainerTop)
        val containerBottom = findViewById<LinearLayout>(R.id.accountContainerBottom)
        containerTop.removeAllViews()
        containerBottom.removeAllViews()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val accounts = prefs.getStringSet("target_accounts", emptySet()) ?: emptySet()
        val accountList = accounts.toList()

        if (accounts.size >= 4) {
            // 2列のグリッド表示
            val numRows = (accountList.size + 1) / 2

            for (i in 0 until numRows) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // 左のアイテム
                createAccountView(accountList[i * 2], rowLayout)?.let { view ->
                    view.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    rowLayout.addView(view)
                }

                // 右のアイテム
                if (i * 2 + 1 < accountList.size) {
                    createAccountView(accountList[i * 2 + 1], rowLayout)?.let { view ->
                        view.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        rowLayout.addView(view)
                    }
                } else {
                    // 奇数個の場合、右側にダミーのビューを追加してレイアウトを揃える
                    val ghostView = layoutInflater.inflate(R.layout.item_account, rowLayout, false)
                    ghostView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    ghostView.visibility = View.INVISIBLE
                    rowLayout.addView(ghostView)
                }

                if (accountList.size <= 6) {
                    containerTop.addView(rowLayout)
                } else {
                    // 6 items = 3 rows.
                    if (i < 3) {
                        containerTop.addView(rowLayout)
                    } else {
                        containerBottom.addView(rowLayout)
                    }
                }
            }
        } else {
            // 1列のリスト表示
            accountList.forEachIndexed { index, entry ->
                val container = if (index < 6) containerTop else containerBottom
                createAccountView(entry, container)?.let { view ->
                    container.addView(view)
                }
            }
        }
    }

    private fun createAccountView(entry: String, parent: ViewGroup): View? {
        val view = layoutInflater.inflate(R.layout.item_account, parent, false)

        val nameText = view.findViewById<TextView>(R.id.accountNameText)
        val userIcon = view.findViewById<ImageView>(R.id.userIcon)
        val snsIcon = view.findViewById<ImageView>(R.id.snsIcon)

        val mainParts = entry.split("|")
        val base = mainParts[0]
        val iconUri = mainParts.getOrNull(1)

        val baseParts = base.split(":")
        if (baseParts.size != 2) return null

        val sns = baseParts[0]
        val name = baseParts[1]

        nameText.text = name

        snsIcon.setImageResource(
            if (sns == "LINE") R.drawable.line else R.drawable.x
        )

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

        view.setOnClickListener {
            val intent = Intent(this, NotifyOptionActivity::class.java)
            intent.putExtra("account_entry", entry)
            startActivity(intent)
        }
        return view
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
