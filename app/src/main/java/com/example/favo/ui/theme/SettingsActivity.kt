package com.example.favo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {

    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        editText = findViewById(R.id.accountEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val accountName = editText.text.toString().trim()

            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            prefs.edit()
                .putString("target_account", accountName)
                .apply()

            Log.d("X_NOTIFY", "SAVED target_account='$accountName'")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val saved = prefs.getString("target_account", "")
        editText.setText(saved)

        Log.d("X_NOTIFY", "LOADED target_account='$saved'")
    }
}

