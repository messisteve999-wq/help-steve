package com.steve.helpsteve

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: View? = null
    private var historyText: TextView? = null

    private val settings by lazy {
        getSharedPreferences("help_steve_settings", MODE_PRIVATE)
    }

    private var personality = "Amical"
    private var responseMode = "Suggestions"
    private var language = "Français"
    private var selectedApp = "WhatsApp"

    private var contactPersonality = "Amical"
    private var contactMode = "Suggestions"
    private var contactLanguage = "Français"

    private lateinit var contactName: EditText
    private lateinit var contactAppSpinner: Spinner
    private lateinit var contactPersonalitySpinner: Spinner
    private lateinit var contactModeSpinner: Spinner
    private lateinit var contactLanguageSpinner: Spinner

    private val apps = arrayOf(
        "WhatsApp",
        "WhatsApp Business",
        "Messenger",
        "Telegram",
        "Gmail"
    )

    private val personalities = arrayOf("Amical", "Amoureux", "Drague")
    private val modes = arrayOf("Suggestions", "Automatique")
    private val languages = arrayOf("Français", "English", "Español", "中文", "日本語")

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

        fun makeSpinner(
            title: String,
            options: Array<String>,
            selected: String,
            callback: (String) -> Unit
        ): Spinner {
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
                        callback(options[position])
                    }
                }

            root.addView(spinner)
            return spinner
        }

        fun saveGeneralSetting(key: String, value: String) {
            settings.edit().putString(key, value).apply()
        }

        label("✦ HELP-STEVE AI")
        label("PARAMÈTRES GÉNÉRAUX")

        makeSpinner("Application", apps, selectedApp) {
            selectedApp = it
            saveGeneralSetting("selectedApp", it)
        }

        makeSpinner("Personnalité générale", personalities, personality) {
            personality = it
            saveGeneralSetting("personality", it)
        }

        makeSpinner("Mode général", modes, responseMode) {
            responseMode = it
            saveGeneralSetting("responseMode", it)
        }

        makeSpinner("Langue générale", languages, language) {
            language = it
            saveGeneralSetting("language", it)
        }

        label("──────────────")
        label("CONTACTS PERSONNALISÉS")

        label("Nom du contact")

        contactName = EditText(this).apply {
            hint = "Exemple : Alice"
            textSize = 15f
            setSingleLine(true)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFAAAAAA.toInt())
        }
        root.addView(contactName)

        contactAppSpinner = makeSpinner(
            "Application du contact",
            apps,
            selectedApp
        ) { }

        contactPersonalitySpinner = makeSpinner(
            "Personnalité du contact",
            personalities,
            contactPersonality
        ) {
            contactPersonality = it
        }

        contactModeSpinner = makeSpinner(
            "Mode de réponse du contact",
            modes,
            contactMode
        ) {
            contactMode = it
        }

        contactLanguageSpinner = makeSpinner(
            "Langue du contact",
            languages,
            contactLanguage
        ) {
            contactLanguage = it
        }

        root.addView(Button(this).apply {
            text = "ENREGISTRER LES RÉGLAGES DU CONTACT"
            setOnClickListener {
                val name = contactName.text.toString().trim()

                if (name.isBlank()) {
                    Toast.makeText(
                        this@FloatingService,
                        "Entre d'abord le nom du contact",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                ContactSettingsStore.save(
                    applicationContext,
                    apps[contactAppSpinner.selectedItemPosition],
                    name,
                    ContactSettings(
                        personality = contactPersonality,
                        responseMode = contactMode,
                        language = contactLanguage
                    )
                )

                Toast.makeText(
                    this@FloatingService,
                    "Réglages de $name enregistrés",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        root.addView(Button(this).apply {
            text = "CHARGER LES RÉGLAGES DU CONTACT"
            setOnClickListener {
                val name = contactName.text.toString().trim()

                if (name.isBlank()) {
                    Toast.makeText(
                        this@FloatingService,
                        "Entre le nom du contact",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val app = apps[contactAppSpinner.selectedItemPosition]

                val saved = ContactSettingsStore.get(
                    applicationContext,
                    app,
                    name
                )

                contactPersonality = saved.personality
                contactMode = saved.responseMode
                contactLanguage = saved.language

                contactPersonalitySpinner.setSelection(
                    personalities.indexOf(contactPersonality).coerceAtLeast(0)
                )
                contactModeSpinner.setSelection(
                    modes.indexOf(contactMode).coerceAtLeast(0)
                )
                contactLanguageSpinner.setSelection(
                    languages.indexOf(contactLanguage).coerceAtLeast(0)
                )

                Toast.makeText(
                    this@FloatingService,
                    "Réglages de $name chargés",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        root.addView(Button(this).apply {
            text = "SUPPRIMER LES RÉGLAGES DU CONTACT"
            setOnClickListener {
                val name = contactName.text.toString().trim()

                if (name.isBlank()) {
                    Toast.makeText(
                        this@FloatingService,
                        "Entre le nom du contact",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                ContactSettingsStore.delete(
                    applicationContext,
                    apps[contactAppSpinner.selectedItemPosition],
                    name
                )

                Toast.makeText(
                    this@FloatingService,
                    "Réglages de $name supprimés",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        label("──────────────")
        label("HISTORIQUE DES NOTIFICATIONS")

        historyText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 8, 8, 8)
            text = "Chargement..."
        }

        root.addView(historyText)

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

        val scroll = ScrollView(this).apply {
            setPadding(8, 8, 8, 8)
            addView(root)
        }

        panel = scroll

        wm.addView(
            scroll,
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
