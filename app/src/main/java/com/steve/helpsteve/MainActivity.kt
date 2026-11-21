package com.steve.helpsteve

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tokenInput: EditText
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeminiClient.initialize(applicationContext)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
        }

        content.addView(TextView(this).apply {
            text = "HELP-STEVE V3.0"
            textSize = 26f
        })

        content.addView(TextView(this).apply {
            text = "Serveur IA : http://127.0.0.1:8787"
            textSize = 14f
            setPadding(0, 12, 0, 20)
        })

        content.addView(TextView(this).apply {
            text = "CONFIGURATION DU SERVEUR"
            textSize = 16f
        })

        tokenInput = EditText(this).apply {
            hint = "Coller le jeton du serveur"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            maxLines = 1
        }
        content.addView(tokenInput)

        content.addView(Button(this).apply {
            text = "Enregistrer le jeton de façon sécurisée"
            setOnClickListener {
                val token = tokenInput.text.toString()
                if (SecureTokenStore.save(this@MainActivity, token)) {
                    tokenInput.text.clear()
                    updateStatus()
                    Toast.makeText(
                        this@MainActivity,
                        "Jeton enregistré et chiffré",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Jeton vide ou erreur de stockage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        content.addView(Button(this).apply {
            text = "Supprimer le jeton enregistré"
            setOnClickListener {
                SecureTokenStore.clear(this@MainActivity)
                tokenInput.text.clear()
                updateStatus()
                Toast.makeText(
                    this@MainActivity,
                    "Jeton supprimé",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        statusView = TextView(this).apply {
            textSize = 14f
            setPadding(0, 12, 0, 20)
        }
        content.addView(statusView)
        updateStatus()

        content.addView(Button(this).apply {
            text = "1. Autoriser l'affichage par-dessus"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })

        content.addView(Button(this).apply {
            text = "2. Accès aux notifications"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        content.addView(Button(this).apply {
            text = "3. Démarrer la bulle flottante"
            setOnClickListener {
                val intent = Intent(this@MainActivity, BubbleService::class.java)
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        })

        val scroll = ScrollView(this).apply {
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        setContentView(scroll)
    }

    private fun updateStatus() {
        statusView.text = if (SecureTokenStore.isConfigured(this)) {
            "● Jeton enregistré sur cet appareil"
        } else {
            "○ Jeton non configuré"
        }
    }
}
