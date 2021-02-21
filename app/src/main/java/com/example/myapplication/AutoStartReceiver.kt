package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log


class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val intent = Intent(context, AutoStartService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context!!.startForegroundService(intent)
//        } else {
//            context!!.startService(intent)
//        }
        context!!.startService(intent)
        Log.i("AutoStartReceiver", "started")
    }
}