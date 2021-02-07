package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.decodeHex

object EchoWebSocketListener : WebSocketListener() {
    private const val NORMAL_CLOSURE_STATUS = 1000
    val channel = BroadcastChannel<String>(1)

    suspend fun someMethod(text: String) {
        channel.send(text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("Hello, it's SSaurel !")
        webSocket.send("What's up ?")
        webSocket.send("deadbeef".decodeHex())
        webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        output("Receiving : " + text!!)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        output("Receiving bytes : " + bytes!!.hex())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket!!.close(NORMAL_CLOSURE_STATUS, null)
        output("Closing : $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        output("Error : " + t.message)
    }

    private fun output(txt: String) {
        Log.v("WSS", txt)
        runBlocking {
            someMethod(txt)
        }
    }
}