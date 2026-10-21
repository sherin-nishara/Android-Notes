package com.example.floatingnote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager


class FloatingNoteService : Service() {
    private lateinit var wm: WindowManager
    private var view: FloatingNoteView? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        showOverlay()
    }

    private fun showOverlay() {
        val v = FloatingNoteView(this)
        view = v
        val p = WindowManager.LayoutParams(
            NoteRepository.loadW(this), NoteRepository.loadH(this),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = NoteRepository.loadX(this@FloatingNoteService)
            y = NoteRepository.loadY(this@FloatingNoteService)
        }
        wm.addView(v, p)
    }

    private fun buildNotification(): Notification {
        val ch = "note"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(ch, "Floating Note", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c)
        }
        return Notification.Builder(this, ch)
            .setContentTitle("Floating Note")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        view?.let { wm.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 1
    }
}
