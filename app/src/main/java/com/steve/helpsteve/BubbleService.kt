package com.steve.helpsteve

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable

import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import kotlin.math.abs

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: ImageView
    private lateinit var chatRoot: FrameLayout
    private lateinit var chat: ChatWindow
    private var chatVisible = false

    override fun onCreate() {
        super.onCreate()
        GeminiClient.initialize(applicationContext)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()

        val metrics = resources.displayMetrics
        val chatParams = overlayParams(
            (metrics.widthPixels * 0.92).toInt(),
            (metrics.heightPixels * 0.72).toInt()
        ).apply { gravity = Gravity.CENTER }
        chatRoot = FrameLayout(this).apply { visibility = View.GONE }
        chat = ChatWindow(this, wm, chatParams, chatRoot)
        wm.addView(chatRoot, chatParams)

        createBubble()

        MessageStore.onIncoming = { msg -> chat.onIncoming(msg) }
        AutoReplier.onSent = { msg ->
            // La bulle signale qu'une réponse automatique vient d'être envoyée
            bubble.setImageResource(android.R.drawable.checkbox_on_background)
            bubble.postDelayed({ bubble.setImageResource(R.drawable.ic_robot_bubble) }, 5000)
            Toast.makeText(this, "help-steve a répondu à ${msg.sender}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        MessageStore.onIncoming = null
        AutoReplier.onSent = null
        AutoReplier.onUpdate = null
        wm.removeView(bubble)
        wm.removeView(chatRoot)
        super.onDestroy()
    }

    private fun overlayParams(w: Int, h: Int, focusable: Boolean = false): WindowManager.LayoutParams {
        val flags = if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun createBubble() {
        bubble = ImageView(this).apply {
            setImageResource(R.drawable.ic_robot_bubble)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(7, 7, 7, 7)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF071226.toInt())
                setStroke(2, 0xFF00E5FF.toInt())
            }
            elevation = 12f
        }
        val lp = overlayParams(50, 50).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = e.rawX; touchY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    lp.x = startX + dx
                    lp.y = startY + dy
                    wm.updateViewLayout(bubble, lp)
                }
                MotionEvent.ACTION_UP -> if (!moved) toggleChat()
            }
            true
        }
        wm.addView(bubble, lp)
    }

    private fun toggleChat() {
        chatVisible = !chatVisible
        chatRoot.visibility = if (chatVisible) View.VISIBLE else View.GONE
    }

    private fun startAsForeground() {
        val channelId = "helpsteve_bubble"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "help-steve", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = Notification.Builder(this, channelId)
            .setContentTitle("help-steve actif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notif)
    }
}
