package com.steve.helpsteve

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var historyText: TextView? = null

    private val settings by lazy {
        getSharedPreferences("help_steve_settings", MODE_PRIVATE)
    }

    private var personality = "Amical"
    private var responseMode = "Suggestions"
    private var language = "Français"
    private var selectedApp = "WhatsApp"

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        personality = settings.getString("personality", "Amical") ?: "Amical"
        responseMode = settings.getString("responseMode", "Suggestions") ?: "Suggestions"
        language = settings.getString("language", "Français") ?: "Français"
        selectedApp = settings.getString("selectedApp", "WhatsApp") ?: "WhatsApp"

        createBubble()
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (android.os.Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
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
                if (panel == null) openPanel() else closePanel()
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

        fun saveSetting(key: String, value: String) {
            settings.edit().putString(key, value).apply()
        }

        fun selector(
            title: String,
            options: Array<String>,
            selected: String,
            key: String,
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
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val value = options[position]
                        saveSetting(key, value)
                        callback(value)
                    }
                }

            root.addView(spinner)
        }

        label("✦ HELP-STEVE AI")
        label("Paramètres de l'assistant")

        selector(
            "Application",
            arrayOf(
                "WhatsApp",
                "WhatsApp Business",
                "Messenger",
                "Telegram",
                "Gmail"
            ),
            selectedApp,
            "selectedApp"
        ) { selectedApp = it }

        selector(
            "Personnalité",
            arrayOf("Amical", "Amoureux", "Drague"),
            personality,
            "personality"
        ) { personality = it }

        selector(
            "Mode de réponse",
            arrayOf("Suggestions", "Automatique"),
            responseMode,
            "responseMode"
        ) { responseMode = it }

        selector(
            "Langue",
            arrayOf("Français", "English", "Español", "中文", "日本語"),
            language,
            "language"
        ) { language = it }

        label("Historique des notifications")

        historyText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 8, 8, 8)
            text = "Chargement de l'historique..."
        }

        val scroll = ScrollView(this).apply {
            addView(historyText)
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                180
            )
        )

        root.addView(Button(this).apply {
            text = "ACTUALISER L'HISTORIQUE"
            setOnClickListener { refreshHistory() }
        })

        root.addView(Button(this).apply {
            text = "EFFACER L'HISTORIQUE"
            setOnClickListener {
                MessageStore.clear(applicationContext)
                refreshHistory()
                Toast.makeText(
                    this@FloatingService,
                    "Historique effacé",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        label("Profil IA : Steve • 28 ans • Yaoundé")

        root.addView(Button(this).apply {
            text = "FERMER"
            setOnClickListener { closePanel() }
        })

        panel = root

        wm.addView(
            root,
            params(340, 650).apply {
                gravity = Gravity.CENTER
                flags = flags and
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            }
        )

        refreshHistory()
    }

    private fun refreshHistory() {
        val messages = MessageStore.getAll(applicationContext)

        val content = if (messages.isEmpty()) {
            "Aucun message enregistré pour le moment."
        } else {
            messages.joinToString("\n\n") { item ->
                val appName = when (item.packageName) {
                    "com.whatsapp" -> "WhatsApp"
                    "com.whatsapp.w4b" -> "WhatsApp Business"
                    "com.facebook.orca" -> "Messenger"
                    "org.telegram.messenger" -> "Telegram"
                    "com.google.android.gm" -> "Gmail"
                    else -> item.packageName
                }

                val date = SimpleDateFormat(
                    "dd/MM HH:mm",
                    Locale.getDefault()
                ).format(Date(item.timestamp))

                "[$appName] $date\n${item.title}\n${item.message}"
            }
        }

        historyText?.text = content
    }

    private fun closePanel() {
        panel?.let {
            if (it.isAttachedToWindow) wm.removeView(it)
        }
        panel = null
        historyText = null
    }

    override fun onDestroy() {
        closePanel()

        bubble?.let {
            if (it.isAttachedToWindow) wm.removeView(it)
        }

        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
