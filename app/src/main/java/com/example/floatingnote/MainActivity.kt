package com.example.floatingnote

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }, 1)
        } else startService()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Settings.canDrawOverlays(this)) startService()
        finish()
    }

    private fun startService() {
        startForegroundService(Intent(this, FloatingNoteService::class.java))
    }
}
