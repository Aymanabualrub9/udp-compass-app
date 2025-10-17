package com.example.udpcompass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class LocationUdpService : Service(), SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedClient: FusedLocationProviderClient
    private var socket: DatagramSocket? = null
    private var destAddr = "192.168.1.2"
    private var destPort = 5005
    private var sendIntervalMs = 200L
    private var currentHeadingDeg: Float? = null

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        socket = DatagramSocket()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        destAddr = intent?.getStringExtra("dest_ip") ?: destAddr
        destPort = intent?.getIntExtra("dest_port", destPort) ?: destPort
        startForegroundServiceWithNotification()
        startLoop()
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val chanId = "udp_compass_chan"
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(chanId, "UDP Compass", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(chan)
        }
        val note: Notification = NotificationCompat.Builder(this, chanId)
            .setContentTitle("UDP Compass Sender")
            .setContentText("Sending location to $destAddr:$destPort")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, note)
    }

    private fun startLoop() {
        scope.launch {
            while (isActive) {
                try {
                    requestLocationAndSend()
                } catch (t: Throwable) {
                    Log.e("LocationUdpService", "loop error", t)
                }
                delay(sendIntervalMs)
            }
        }
    }

    private fun requestLocationAndSend() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, sendIntervalMs).build()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    sendUdpPacket(loc.latitude, loc.longitude, loc.accuracy, currentHeadingDeg)
                }
            }
    }

    private fun sendUdpPacket(lat: Double, lon: Double, acc: Float, heading: Float?) {
        val json = JSONObject().apply {
            put("timestamp", System.currentTimeMillis() / 1000.0)
            put("device_id", android.os.Build.MODEL ?: "android")
            put("latitude", lat)
            put("longitude", lon)
            put("accuracy_m", acc)
            heading?.let { put("heading", it.toDouble()) }
        }
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        scope.launch {
            try {
                val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(destAddr), destPort)
                socket?.send(packet)
            } catch (e: Exception) {
                // log but ignore
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationVector = event.values
            val rotMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotMatrix, rotationVector)
            val remapped = FloatArray(9)
            SensorManager.remapCoordinateSystem(rotMatrix,
                SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped)
            val orientations = FloatArray(3)
            SensorManager.getOrientation(remapped, orientations)
            val azimuthRad = orientations[0].toDouble()
            val azimuthDeg = ((Math.toDegrees(azimuthRad) + 360.0) % 360.0).toFloat()
            currentHeadingDeg = azimuthDeg
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        socket?.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
