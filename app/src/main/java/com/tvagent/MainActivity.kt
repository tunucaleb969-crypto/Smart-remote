package com.hikersremote

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : AppCompatActivity() {

    private var mqttClient: MqttClient? = null
    private lateinit var tvStatus: TextView
    private val brokerUri = "ssl://broker.hivemq.com:8883"
    private val topic = "hikersremote/kwame_ashaiman_7f3k/cmd"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnPower).setOnClickListener { send("POWER") }
        findViewById<Button>(R.id.btnVolUp).setOnClickListener { send("VOL_UP") }
        findViewById<Button>(R.id.btnVolDown).setOnClickListener { send("VOL_DOWN") }
        findViewById<Button>(R.id.btnHome).setOnClickListener { send("HOME") }
        findViewById<Button>(R.id.btnBack).setOnClickListener { send("BACK") }
        findViewById<Button>(R.id.btnRecents).setOnClickListener { send("RECENTS") }

        connect()
    }

    private fun connect() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val clientId = "phone-" + System.currentTimeMillis()
                    val client = MqttClient(brokerUri, clientId, MemoryPersistence())
                    val options = MqttConnectOptions()
                    options.isCleanSession = true
                    options.connectionTimeout = 10
                    client.connect(options)
                    mqttClient = client
                }
                tvStatus.text = "Connected — ready to send commands"
            } catch (e: Exception) {
                tvStatus.text = "Connection failed: ${e.message}"
            }
        }
    }

    private fun send(command: String) {
        val client = mqttClient
        if (client == null || !client.isConnected) {
            tvStatus.text = "Not connected — retrying..."
            connect()
            return
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    client.publish(topic, MqttMessage(command.toByteArray()))
                }
            } catch (e: Exception) {
                tvStatus.text = "Send failed: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { mqttClient?.disconnect() } catch (_: Exception) {}
    }
}
