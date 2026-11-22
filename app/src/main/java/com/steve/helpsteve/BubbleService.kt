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
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class BubbleService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bubbleContainer: FrameLayout
    private lateinit var bubble: ImageView
    private lateinit var sentBadge: TextView
    private lateinit var chatRoot: FrameLayout
    private lateinit var chat: ChatWindow

    private var chatVisible = false
    private var autoSentCount = 0

    override fun onCreate() {
        super.onCreate()

        GeminiClient.initialize(applicationContext)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startAsForeground()

        val metrics = resources.displayMetrics

        val chatWidth = (metrics.widthPixels * 0.68).toInt()
        val chatHeight = (metrics.heightPixels * 0.46).toInt()

        val windowPrefs = getSharedPreferences(
            "help_steve_window", Context.MODE_PRIVATE
        )

        val chatParams = overlayParams(chatWidth, chatHeight).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowPrefs.getInt(
                "x", (metrics.widthPixels - chatWidth) / 2
            ).coerceIn(0, (metrics.widthPixels - chatWidth).coerceAtLeast(0))
            y = windowPrefs.getInt(
                "y", (metrics.heightPixels - chatHeight) / 2
            ).coerceIn(0, (metrics.heightPixels - chatHeight).coerceAtLeast(0))
        }

        chatRoot = FrameLayout(this).apply {
            visibility = View.GONE
        }

        chat = ChatWindow(this, wm, chatParams, chatRoot)
        wm.addView(chatRoot, chatParams)

        createBubble()

        MessageStore.onIncoming = { msg ->
            chat.onIncoming(msg)
        }

        AutoReplier.onSent = { msg ->
            // Ce callback est déclenché uniquement après un envoi réussi.
            sentBadge.post {
                autoSentCount++
                updateSentBadge()

                Toast.makeText(
                    this,
                    "HELP-STEVE a répondu automatiquement à ${msg.sender}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        MessageStore.onIncoming = null
        AutoReplier.onSent = null
        AutoReplier.onUpdate = null

        if (::bubbleContainer.isInitialized) {
            wm.removeView(bubbleContainer)
        }

        if (::chatRoot.isInitialized) {
            wm.removeView(chatRoot)
        }

        super.onDestroy()
    }

    private fun overlayParams(
        w: Int,
        h: Int,
        focusable: Boolean = false
    ): WindowManager.LayoutParams {

        val flags = if (focusable) {
            0
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        return WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun createBubble() {

        bubbleContainer = FrameLayout(this)

        bubble = ImageView(this).apply {
            setImageResource(R.drawable.ic_robot_bubble)
            scaleType = ImageView.ScaleType.FIT_XY
            setPadding(0, 0, 0, 0)

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF071226.toInt())
                setStroke(2, 0xFF00E5FF.toInt())
            }

            clipToOutline = true
            elevation = 12f
        }

        val imageParams = FrameLayout.LayoutParams(
            62,
            62,
            Gravity.CENTER
        )

        bubbleContainer.addView(bubble, imageParams)

        sentBadge = TextView(this).apply {
            text = ""
            textSize = 10f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(5, 2, 5, 2)
            visibility = View.GONE

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(0xFF16A34A.toInt())
                setStroke(2, 0xFFFFFFFF.toInt())
            }

            elevation = 20f
        }

        val badgeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            20,
            Gravity.TOP or Gravity.END
        )

        bubbleContainer.addView(sentBadge, badgeParams)

        val lp = overlayParams(72, 72).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubbleContainer.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()

                    if (abs(dx) > 10 || abs(dy) > 10) {
                        moved = true
                    }

                    lp.x = startX + dx
                    lp.y = startY + dy

                    wm.updateViewLayout(bubbleContainer, lp)
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        toggleChat()
                    }
                }
            }

            true
        }

        wm.addView(bubbleContainer, lp)
    }

    private fun updateSentBadge() {

        if (!::sentBadge.isInitialized) return

        val countText = if (autoSentCount > 99) {
            "✓99+"
        } else {
            "✓$autoSentCount"
        }

        sentBadge.text = countText
        sentBadge.visibility = View.VISIBLE

        sentBadge.scaleX = 1.25f
        sentBadge.scaleY = 1.25f

        sentBadge.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }

    private fun toggleChat() {
        chatVisible = !chatVisible
        chatRoot.visibility = if (chatVisible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun startAsForeground() {

        val channelId = "helpsteve_bubble"

        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                "help-steve",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val notif = Notification.Builder(this, channelId)
            .setContentTitle("help-steve actif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notif)
    }
}
