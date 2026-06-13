package com.autoconfirm.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.autoconfirm.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val prefs get() = getSharedPreferences("autoconfirm", MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadSettings()
        setupListeners()
        checkPermissions()
        startForegroundService()
    }

    private fun loadSettings() {
        binding.etServerUrl.setText(prefs.getString("server_url", "http://84.46.255.250:3000"))
        binding.etToken.setText(prefs.getString("token", ""))
        updateStatus()
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            val token = binding.etToken.text.toString().trim()
            if (url.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "Remplis tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("server_url", url).putString("token", token).apply()
            Toast.makeText(this, "Parametres sauvegardes", Toast.LENGTH_SHORT).show()
            updateStatus()
        }
        binding.btnSmsPermission.setOnClickListener { requestSmsPermission() }
        binding.btnNotifPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        binding.btnTest.setOnClickListener { testConnection() }
        binding.btnKeepAlive.setOnClickListener {
            startForegroundService()
            Toast.makeText(this, "Service actif - ecran peut s'eteindre", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions() {
        val smsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val notifEnabled = isNotificationListenerEnabled()
        binding.tvSmsStatus.text = if (smsGranted) "SMS autorise" else "SMS non autorise"
        binding.tvNotifStatus.text = if (notifEnabled) "Notifications autorisees" else "Notifications non autorisees"
        updateStatus()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = packageName + "/" + NotificationService::class.java.name
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn) == true
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), 101)
    }

    private fun testConnection() {
        val url = prefs.getString("server_url", "") ?: ""
        val token = prefs.getString("token", "") ?: ""
        if (url.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Configure d'abord les parametres", Toast.LENGTH_SHORT).show()
            return
        }
        ApiClient.sendSms(url, token, "test", "Test AutoConfirm") { success, message ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Connexion reussie!", Toast.LENGTH_SHORT).show()
                    binding.tvConnectionStatus.text = "Connecte"
                } else {
                    Toast.makeText(this, "Erreur: $message", Toast.LENGTH_SHORT).show()
                    binding.tvConnectionStatus.text = "Erreur connexion"
                }
            }
        }
    }

    private fun updateStatus() {
        val url = prefs.getString("server_url", "")
        val token = prefs.getString("token", "")
        binding.tvConnectionStatus.text = if (!url.isNullOrEmpty() && !token.isNullOrEmpty()) "Configure" else "Non configure"
    }

    private fun startForegroundService() {
        val intent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
}
