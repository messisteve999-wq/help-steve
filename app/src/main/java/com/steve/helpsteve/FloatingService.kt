package com.steve.helpsteve

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.*

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null

    private var personality = "Amical"
    private var responseMode = "Suggestions"
    private var language = "Français"
    private var selectedApp = "WhatsApp"

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (android.os.Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun createBubble() {
        bubble = TextView(this).apply {
            text = "✦"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    0xFF6748FF.toInt(),
                    0xFF00D9FF.toInt()
                )
            }

            setOnClickListener {
                if (panel == null) openPanel()
                else closePanel()
            }
        }

        wm.addView(
            bubble,
            params(65, 65).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                x = 15
            }
        )
    }

    private fun openPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background = GradientDrawable().apply {
                setColor(0xFF101A30.toInt())
                cornerRadius = 30f
                setStroke(2, 0xFF6759FF.toInt())
            }
        }

        fun label(text: String) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 10, 0, 10)
            })
        }

        fun selector(
            title: String,
            options: Array<String>,
            selected: String,
            callback: (String) -> Unit
        ) {
            label(title)
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options
            )
            spinner.setSelection(options.indexOf(selected).coerceAtLeast(0))
            spinner.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: android.view.View?,
                        position: Int,
                        id: Long
                    ) {
                        callback(options[position])
                    }
                }
            root.addView(spinner)
        }

        label("✦ HELP-STEVE AI")
        label("Messagerie : $selectedApp")

        selector(
            "Application",
            arrayOf("WhatsApp", "Messenger", "Telegram", "Gmail"),
            selectedApp
        ) { selectedApp = it }

        selector(
            "Personnalité",
            arrayOf("Amical", "Amoureux", "Drague"),
            personality
        ) { personality = it }

        selector(
            "Mode de réponse",
            arrayOf("Suggestions", "Automatique"),
            responseMode
        ) { responseMode = it }

        selector(
            "Langue",
            arrayOf("Français", "English", "Español", "中文", "日本語"),
            language
        ) { language = it }

        label("Profil IA : Steve • Biologie • 28 ans • Yaoundé")

        root.addView(Button(this).apply {
            text = "Fermer la fenêtre"
            setOnClickListener { closePanel() }
        })

        panel = root

        wm.addView(
            root,
            params(340, 650).apply {
                gravity = Gravity.CENTER
                flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            }
        )
    }

    private fun closePanel() {
        panel?.let {
            wm.removeView(it)
        }
        panel = null
    }

    override fun onDestroy() {
        closePanel()
        bubble?.let { wm.removeView(it) }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
