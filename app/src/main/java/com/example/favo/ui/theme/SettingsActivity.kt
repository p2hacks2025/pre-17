package com.example.favo

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class SettingsActivity : ComponentActivity() {

    private lateinit var accountEditText: EditText
    private lateinit var radioX: RadioButton
    private lateinit var radioLine: RadioButton
    private lateinit var radioLover: RadioButton
    private lateinit var radioOshi: RadioButton
    private lateinit var flashSwitch: Switch
    private lateinit var iconPreview: ImageView

    private var selectedIconUri: Uri? = null
    private var mediaPlayer: MediaPlayer? = null

    // 画像選択（永続アクセス可）
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                // 🔐 永続アクセス権を保持（超重要）
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedIconUri = uri
                iconPreview.setImageURI(uri)
                iconPreview.clipToOutline = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        accountEditText = findViewById(R.id.accountEditText)
        radioX = findViewById(R.id.radioX)
        radioLine = findViewById(R.id.radioLine)
        radioLover = findViewById(R.id.radioLover)
        radioOshi = findViewById(R.id.radioOshi)
        flashSwitch = findViewById(R.id.flashSwitch)
        iconPreview = findViewById(R.id.iconPreview)

        val iconSelectButton = findViewById<Button>(R.id.iconSelectButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // -----------------------------
        // アイコン選択
        // -----------------------------
        iconSelectButton.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        // -----------------------------
        // 通知タイプ試聴
        // -----------------------------
        radioLover.setOnClickListener {
            playSound(R.raw.notification_sound_lover)
        }

        radioOshi.setOnClickListener {
            playSound(R.raw.notification_sound_oshi)
        }

        // -----------------------------
        // 保存
        // -----------------------------
        saveButton.setOnClickListener {
            saveAccount()
        }
    }

    // =============================
    // 音を鳴らす（二重再生防止）
    // =============================
    private fun playSound(resId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.start()
    }

    // =============================
    // アカウント保存
    // =============================
    private fun saveAccount() {
        val name = accountEditText.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "アカウント名を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        val sns = if (radioLine.isChecked) "LINE" else "X"
        val notifyType = if (radioOshi.isChecked) "oshi" else "lover"
        val flashEnabled = flashSwitch.isChecked
        val iconPart = selectedIconUri?.toString() ?: ""

        val entry = "$sns:$name|$iconPart"

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val set = prefs.getStringSet("target_accounts", emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        set.add(entry)

        prefs.edit()
            .putStringSet("target_accounts", set)
            .putString("sound_type_$sns:$name", notifyType)
            .putBoolean("flash_$sns:$name", flashEnabled)
            .apply()

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}



