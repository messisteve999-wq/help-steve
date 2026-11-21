package com.steve.helpsteve

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        root.addView(TextView(this).apply { text = "help-steve v2.1"; textSize = 24f })
        root.addView(Button(this).apply {
            text = "1. Autoriser l'affichage par-dessus"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        root.addView(Button(this).apply {
            text = "2. Accès aux notifications"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "3. Démarrer la bulle"
            setOnClickListener {
                val i = Intent(this@MainActivity, BubbleService::class.java)
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            }
        })
        setContentView(root)
    }
}
