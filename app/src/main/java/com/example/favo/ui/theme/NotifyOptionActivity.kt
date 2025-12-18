package com.example.favo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class NotifyOptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notify_option)

        val flashSwitch = findViewById<Switch>(R.id.flashSwitch)
        val soundGroup = findViewById<RadioGroup>(R.id.soundGroup)
        val loverRadio = findViewById<RadioButton>(R.id.soundLover)
        val oshiRadio = findViewById<RadioButton>(R.id.soundOshi)
        val deleteButton = findViewById<Button>(R.id.deleteAccountButton)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // ← 戻る矢印を表示
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "通知設定"


        // --- 既存設定の反映 ---
        flashSwitch.isChecked = prefs.getBoolean("flash_enabled", true)

        when (prefs.getString("sound_type", "lover")) {
            "lover" -> loverRadio.isChecked = true
            "oshi" -> oshiRadio.isChecked = true
        }

        // フラッシュ ON/OFF
        flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("flash_enabled", isChecked).apply()
        }

        // 通知音選択
        soundGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.soundLover -> "lover"
                R.id.soundOshi -> "oshi"
                else -> "lover"
            }
            prefs.edit().putString("sound_type", type).apply()
        }

        // 🗑 登録削除
        deleteButton.setOnClickListener {

            prefs.edit()
                .remove("target_account")
                .apply()

            // MainActivity に戻る
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
    // ← が押されたときの処理
    override fun onSupportNavigateUp(): Boolean {
        finish() // MainActivity に戻る
        return true
    }
}


