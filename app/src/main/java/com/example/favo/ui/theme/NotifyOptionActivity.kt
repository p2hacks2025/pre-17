package com.example.favo

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
class NotifyOptionActivity : ComponentActivity() {

    private lateinit var flashSwitch: Switch
    private lateinit var radioLover: RadioButton
    private lateinit var radioOshi: RadioButton
    private lateinit var soundGroup: RadioGroup
    private lateinit var iconPreview: ImageView

    private var mediaPlayer: MediaPlayer? = null
    private var selectedIconUri: Uri? = null

    private lateinit var originalEntry: String
    private lateinit var baseKey: String

    // ===============================
    // 画像選択
    // ===============================
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedIconUri = uri
                iconPreview.setImageURI(uri)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notify_option)

        // ----- View取得 -----
        flashSwitch = findViewById(R.id.flashSwitch)
        radioLover = findViewById(R.id.soundLover)
        radioOshi = findViewById(R.id.soundOshi)
        soundGroup = findViewById(R.id.soundGroup)
        iconPreview = findViewById(R.id.iconPreview)

        val iconSelectButton = findViewById<Button>(R.id.iconSelectButton)
        val deleteButton = findViewById<Button>(R.id.deleteAccountButton)

        // ----- 受け取ったアカウント情報 -----
        originalEntry = intent.getStringExtra("account_entry") ?: return finish()


        // entry例: X:Bob|content://xxx
        val parts = originalEntry.split("|")
        baseKey = parts[0]                 // X:Bob
        val iconUri = parts.getOrNull(1)

        if (!iconUri.isNullOrEmpty()) {
            try {
                selectedIconUri = Uri.parse(iconUri)
                iconPreview.setImageURI(selectedIconUri)
                iconPreview.clipToOutline = true
            } catch (e: SecurityException) {
                // 権限が切れていた場合の保険
                selectedIconUri = null
                iconPreview.setImageDrawable(null)
            }
        }

// ----- to item_account -----//
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // ----- フラッシュ -----
        flashSwitch.isChecked = prefs.getBoolean("flash_$baseKey", true)

        // ----- 通知音タイプ -----
        when (prefs.getString("sound_type_$baseKey", "lover")) {
            "oshi" -> radioOshi.isChecked = true
            else -> radioLover.isChecked = true
        }

        // ===============================
        // 通知音選択（2重再生防止）
        // ===============================
        soundGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.soundLover -> playSound(R.raw.notification_sound_lover)
                R.id.soundOshi -> playSound(R.raw.notification_sound_oshi)
            }
        }

        // ----- アイコン選択 -----
        iconSelectButton.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }


        // ----- 変更を保存 so to item_account ----- //
        findViewById<Button>(R.id.saveButton).setOnClickListener {

            val prefs = getSharedPreferences("settings", MODE_PRIVATE)

            // 現在のセット取得
            val set = prefs.getStringSet("target_accounts", mutableSetOf())
                ?.toMutableSet() ?: mutableSetOf()

            // 古い entry を削除
            set.remove(originalEntry)

            // 新しい entry を作る
            val newIconPart = selectedIconUri?.toString() ?: ""
            val newEntry = "$baseKey|$newIconPart"

            // 追加
            set.add(newEntry)

            // 設定保存
            prefs.edit()
                .putStringSet("target_accounts", set)
                .putString(
                    "sound_type_$baseKey",
                    if (radioOshi.isChecked) "oshi" else "lover"
                )
                .putBoolean("flash_$baseKey", flashSwitch.isChecked)
                .apply()

            finish()
        }




        // ----- 削除 -----
        deleteButton.setOnClickListener {
            deleteAccount()
        }
    }

    // ===============================
    // 音再生（必ず1つだけ）
    // ===============================
    private fun playSound(resId: Int) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.start()
    }

    // ===============================
    // アカウント削除
    // ===============================
    private fun deleteAccount() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val set = prefs.getStringSet("target_accounts", mutableSetOf())
            ?.toMutableSet() ?: mutableSetOf()

        set.remove(originalEntry)

        prefs.edit()
            .putStringSet("target_accounts", set)
            .remove("sound_type_$baseKey")
            .remove("flash_$baseKey")
            .apply()

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
