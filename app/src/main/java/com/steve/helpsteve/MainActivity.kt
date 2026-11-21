package com.steve.helpsteve

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val bg = Color.rgb(7, 12, 27)
    private val panel = Color.rgb(17, 26, 48)
    private val cyan = Color.rgb(0, 220, 255)
    private val violet = Color.rgb(164, 100, 255)
    private val muted = Color.rgb(160, 177, 205)

    private lateinit var pageHost: LinearLayout
    private lateinit var statusLine: TextView
    private lateinit var tokenInput: EditText
    private var currentPage = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeminiClient.initialize(applicationContext)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(18), dp(16), dp(8))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(16))
        }

        header.addView(label("HELP-STEVE", 25f, cyan, true))
        header.addView(label("V3.1  •  ASSISTANT INTELLIGENT", 10f, muted, true))
        statusLine = label("", 12f, Color.WHITE)
        statusLine.setPadding(0, dp(12), 0, 0)
        header.addView(statusLine)
        root.addView(header)

        pageHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(pageHost, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }
        listOf(
            "home" to "⌂\nAccueil",
            "chats" to "◉\nChats",
            "ai" to "✧\nIA",
            "server" to "⚙\nServeur"
        ).forEach { item ->
            val b = TextView(this).apply {
                text = item.second
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(if (item.first == currentPage) cyan else muted)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener { showPage(item.first) }
            }
            nav.addView(b, LinearLayout.LayoutParams(0, dp(54), 1f))
        }
        root.addView(nav)
        setContentView(root)
        showPage("home")
    }

    private fun showPage(page: String) {
        currentPage = page
        pageHost.removeAllViews()
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(18))
        }
        scroll.addView(content)
        pageHost.addView(scroll, LinearLayout.LayoutParams(-1, -1))

        when (page) {
            "home" -> homePage(content)
            "chats" -> chatsPage(content)
            "ai" -> aiPage(content)
            "server" -> serverPage(content)
        }
    }

    private fun homePage(box: LinearLayout) {
        box.addView(label("Bonjour 👋", 24f, Color.WHITE, true))
        box.addView(label("Votre assistant personnel est prêt.", 13f, muted))
        space(box, 18)

        val hero = card(box)
        hero.addView(label("✧  STEVE AI", 13f, cyan, true))
        hero.addView(label("Une intelligence à portée de main", 20f, Color.WHITE, true))
        hero.addView(label("WhatsApp • WhatsApp Business • Suggestions intelligentes", 12f, muted))
        space(hero, 16)
        action(hero, "DÉMARRER LA BULLE  ↗", cyan) { startBubble() }

        space(box, 14)
        box.addView(label("CENTRE DE CONTRÔLE", 12f, violet, true))
        space(box, 8)
        val status = if (SecureTokenStore.isConfigured(this))
            "Jeton sécurisé enregistré"
        else "Jeton du serveur à configurer"
        infoCard(box, "Sécurité", status)
        infoCard(box, "Fenêtre flottante", if (Settings.canDrawOverlays(this)) "Autorisation accordée" else "Autorisation requise")
        infoCard(box, "Notifications", "Accès à vérifier dans les paramètres Android")
        action(box, "CONFIGURER LE SERVEUR", violet) { showPage("server") }
    }

    private fun chatsPage(box: LinearLayout) {
        box.addView(label("Conversations", 23f, Color.WHITE, true))
        box.addView(label("Contacts détectés dans les notifications", 12f, muted))
        space(box, 14)

        val apps = MessageStore.apps()
        if (apps.isEmpty()) {
            infoCard(box, "Aucune conversation", "Les contacts apparaîtront ici après réception de notifications compatibles.")
        } else {
            apps.forEach { app ->
                box.addView(label(app, 14f, cyan, true))
                MessageStore.contacts(app).forEach { contact ->
                    val history = MessageStore.history(app, contact)
                    val last = history.lastOrNull()?.text ?: "Aucun message"
                    val item = card(box)
                    item.addView(label(contact, 16f, Color.WHITE, true))
                    item.addView(label(last.take(100), 12f, muted))
                    item.addView(label("${history.size} message(s)", 10f, violet))
                    action(item, "OUVRIR LA FENÊTRE FLOTTANTE", cyan) {
                        startBubble()
                        Toast.makeText(this, "Touchez le robot puis choisissez ce contact.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        space(box, 10)
        action(box, "ACTIVER LA BULLE", violet) { startBubble() }
    }

    private fun aiPage(box: LinearLayout) {
        box.addView(label("Intelligence artificielle", 22f, Color.WHITE, true))
        box.addView(label("Réglages et comportement de votre assistant", 12f, muted))
        space(box, 14)

        infoCard(box, "Mode suggestion", "L'IA propose une réponse que vous pouvez vérifier et envoyer.")
        infoCard(box, "Réponse automatique", "Désactivée par défaut. Activation explicite par contact. Envoi uniquement si Android fournit une action RemoteInput compatible.")
        infoCard(box, "Personnalité et langue", "Les préférences par contact se règlent dans la conversation flottante.")
        infoCard(box, "Traduction", "Utilisez l'action de traduction dans la fenêtre de discussion.")
        action(box, "OUVRIR L'ASSISTANT FLOTTANT", cyan) { startBubble() }
    }

    private fun serverPage(box: LinearLayout) {
        box.addView(label("Serveur & sécurité", 22f, Color.WHITE, true))
        box.addView(label("Connexion locale HELP-STEVE-SERVER", 12f, muted))
        space(box, 14)

        val panelView = card(box)
        panelView.addView(label("ADRESSE DU SERVEUR", 11f, cyan, true))
        panelView.addView(label("http://127.0.0.1:8787", 15f, Color.WHITE, true))
        space(panelView, 14)
        panelView.addView(label("JETON D'ACCÈS", 11f, violet, true))

        tokenInput = EditText(this).apply {
            hint = "Coller le jeton du serveur"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            maxLines = 1
            background = rounded(Color.rgb(29, 41, 67), 12, cyan)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        panelView.addView(tokenInput, LinearLayout.LayoutParams(-1, dp(52)))
        space(panelView, 10)

        action(panelView, "ENREGISTRER DE FAÇON SÉCURISÉE", cyan) {
            val token = tokenInput.text.toString()
            if (SecureTokenStore.save(this, token)) {
                tokenInput.text.clear()
                updateStatus()
                Toast.makeText(this, "Jeton enregistré et chiffré", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Jeton vide ou erreur de stockage", Toast.LENGTH_LONG).show()
            }
        }
        action(panelView, "SUPPRIMER LE JETON", violet) {
            SecureTokenStore.clear(this)
            tokenInput.text.clear()
            updateStatus()
            Toast.makeText(this, "Jeton supprimé", Toast.LENGTH_SHORT).show()
        }
        infoCard(box, "État du jeton", if (SecureTokenStore.isConfigured(this)) "Jeton enregistré sur cet appareil" else "Aucun jeton enregistré")
        infoCard(box, "Protection", "Le jeton est chiffré sur l'appareil avec Android Keystore.")
        action(box, "AUTORISATION FENÊTRE FLOTTANTE", cyan) { requestOverlay() }
        action(box, "ACCÈS AUX NOTIFICATIONS", violet) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        action(box, "DÉMARRER LE SERVEUR TERMUX", cyan) {
            Toast.makeText(this, "Démarre HELP-STEVE-SERVER dans Termux.", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatus() {
        statusLine.text = if (SecureTokenStore.isConfigured(this))
            "●  Jeton sécurisé configuré"
        else "○  Serveur à configurer"
    }

    private fun startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlay()
            Toast.makeText(this, "Autorise l'affichage par-dessus les autres applications.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(this, BubbleService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Assistant flottant démarré", Toast.LENGTH_SHORT).show()
    }

    private fun requestOverlay() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun card(parent: LinearLayout): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(panel, 20, Color.rgb(39, 65, 100))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(12)
        parent.addView(c, lp)
        return c
    }

    private fun infoCard(parent: LinearLayout, title: String, description: String) {
        val c = card(parent)
        c.addView(label(title, 14f, Color.WHITE, true))
        c.addView(label(description, 12f, muted))
    }

    private fun action(parent: LinearLayout, title: String, accent: Int, click: () -> Unit) {
        val b = TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = rounded(accent, 14, accent)
            setOnClickListener { click() }
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(8)
        parent.addView(b, lp)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun rounded(color: Int, radius: Int, stroke: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
