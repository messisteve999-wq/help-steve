package com.steve.helpsteve

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
            setBackgroundColor(0xFF0B1020.toInt())
        }

        val title = TextView(this).apply {
            text = "✦ HELP-STEVE"
            textSize = 30f
            setTextColor(0xFF64DDF5.toInt())
        }

        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "ASSISTANT INTELLIGENT MULTI-MESSAGERIES\nGemini 3.8 • Futuristic AI"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 20, 0, 30)
        }

        layout.addView(subtitle)

        fun addButton(label: String, action: () -> Unit) {
            val button = Button(this).apply {
                text = label
                setOnClickListener { action() }
            }
            layout.addView(button)
        }

        addButton("ACTIVER LES NOTIFICATIONS") {
            startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            )
        }

        addButton("AUTORISER LA BULLE FLOTTANTE") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        addButton("LANCER HELP-STEVE") {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingService::class.java))
            } else {
                Toast.makeText(
                    this,
                    "Autorisez d'abord la superposition",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setContentView(layout)
    }
}
