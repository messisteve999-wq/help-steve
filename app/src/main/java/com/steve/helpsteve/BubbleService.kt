package com.steve.helpsteve

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: TextView
    private lateinit var chatWindow: FrameLayout
    private lateinit var messagesView: TextView
    private var chatVisible = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        createBubble()
        createChatWindow()
        MessageStore.onNew = { msg ->
            messagesView.append("\n${msg.app} · ${msg.sender} : ${msg.text}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        MessageStore.onNew = null
        scope.cancel()
        wm.removeView(bubble)
        wm.removeView(chatWindow)
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
        bubble = TextView(this).apply {
            text = "🤖"
            textSize = 26f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF1A1036.toInt())
                setStroke(4, 0xFF00E5FF.toInt())
            }
        }
        // Position libre : la bulle peut être placée n'importe où
        val lp = overlayParams(150, 150).apply {
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

    private fun createChatWindow() {
        val container = FrameLayout(this)
        container.addView(RobotBackgroundView(this), FrameLayout.LayoutParams(-1, -1))

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        column.addView(TextView(this).apply {
            text = "help-steve IA"
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
        })
        messagesView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            text = "En attente de messages..."
        }
        column.addView(
            ScrollView(this).apply { addView(messagesView) },
            LinearLayout.LayoutParams(-1, 0, 1f)
        )
        column.addView(Button(this).apply {
            text = "Suggestion IA"
            setOnClickListener { suggestForLast() }
        })
        container.addView(column, FrameLayout.LayoutParams(-1, -1))

        val metrics = resources.displayMetrics
        val lp = overlayParams((metrics.widthPixels * 0.9).toInt(), (metrics.heightPixels * 0.6).toInt()).apply {
            gravity = Gravity.CENTER
        }
        chatWindow = container
        chatWindow.visibility = View.GONE
        wm.addView(chatWindow, lp)
    }

    private fun toggleChat() {
        chatVisible = !chatVisible
        chatWindow.visibility = if (chatVisible) View.VISIBLE else View.GONE
    }

    private fun suggestForLast() {
        val last = MessageStore.messages.lastOrNull() ?: return
        scope.launch {
            val reply = withContext(Dispatchers.IO) { GeminiClient.suggest(last.text) }
            messagesView.append("\n\n🤖 Suggestion : $reply")
        }
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
