package com.example.udpcompass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ipField = findViewById<EditText>(R.id.ipField)
        val portField = findViewById<EditText>(R.id.portField)
        val startBtn = findViewById<Button>(R.id.startBtn)
        val stopBtn = findViewById<Button>(R.id.stopBtn)

        startBtn.setOnClickListener {
            if (!hasLocationPermission()) {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                return@setOnClickListener
            }
            val ip = ipField.text.toString().ifBlank { "192.168.1.2" }
            val port = portField.text.toString().toIntOrNull() ?: 5005
            val intent = Intent(this, LocationUdpService::class.java)
            intent.putExtra("dest_ip", ip)
            intent.putExtra("dest_port", port)
            ContextCompat.startForegroundService(this, intent)
        }

        stopBtn.setOnClickListener {
            val intent = Intent(this, LocationUdpService::class.java)
            stopService(intent)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // no-op, user can press Start again
    }
}
