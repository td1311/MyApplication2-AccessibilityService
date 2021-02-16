package com.example.myapplication

import android.content.Context
import android.provider.Settings

object Utils {
    fun isAccessServiceEnabled(context: Context): Boolean {
        val prefString =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
//        return prefString.contains("${context.packageName}/${context.packageName}.GlobalActionBarService")
        return prefString != null && prefString.contains("${context.packageName}/${context.packageName}.GlobalActionBarService")
    }
}