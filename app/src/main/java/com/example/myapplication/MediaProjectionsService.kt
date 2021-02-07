package com.example.myapplication

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder


class MediaProjectionsService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}