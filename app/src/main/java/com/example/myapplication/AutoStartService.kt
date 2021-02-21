package com.example.myapplication

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AutoStartService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("AutoStartService", "started")
        buildSocket()
//        makeStatusNotification("Blurring image", applicationContext)
        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    private fun buildSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/websession")
            .build()
        val wsListener = EchoWebSocketListener
        val webSocket = client.newWebSocket(request, wsListener)
    }
}