package com.lia.ai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lia.ai.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = SecurePrefs.get(this)
        if (!prefs.getString(SecurePrefs.KEY_API, null).isNullOrEmpty()) {
            binding.etApiKey.hint = "Key saved ✓ (naya daalne par replace hogi)"
        }

        binding.btnSaveKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "API Key khaali hai", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putString(SecurePrefs.KEY_API, key).apply()
                binding.etApiKey.setText("")
                Toast.makeText(this, "Key securely save ho gayi", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}
