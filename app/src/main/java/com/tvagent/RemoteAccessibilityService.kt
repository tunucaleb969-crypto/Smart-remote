package com.tvagent

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class RemoteAccessibilityService : AccessibilityService() {

    private var mqttClient: MqttClient? = null
    private val brokerUri = "ssl://broker.hivemq.com:8883"
    private val topic = "hikersremote/kwame_ashaiman_7f3k/cmd"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Thread { connectMqtt() }.start()
    }

    private fun connectMqtt() {
        try {
            val clientId = "tvagent-" + System.currentTimeMillis()
            val client = MqttClient(brokerUri, clientId, MemoryPersistence())
            val options = MqttConnectOptions()
            options.isAutomaticReconnect = true
            options.isCleanSession = true
            options.connectionTimeout = 10

            client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w("TVAgent", "Connection lost: ${cause?.message}")
                }

                override fun messageArrived(t: String?, message: MqttMessage?) {
                    val command = message?.toString()?.trim() ?: return
                    handleCommand(command)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client.connect(options)
            client.subscribe(topic, 1)
            mqttClient = client
            Log.i("TVAgent", "Connected, subscribed to $topic")
        } catch (e: Exception) {
            Log.e("TVAgent", "Connect failed: ${e.message}")
        }
    }

    private fun handleCommand(command: String) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (command) {
            "POWER" -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "VOL_UP" -> audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "VOL_DOWN" -> audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            else -> Log.w("TVAgent", "Unknown command: $command")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
