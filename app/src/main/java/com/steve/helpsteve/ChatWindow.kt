package com.steve.helpsteve

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatWindow(
    private val ctx: Context,
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val root: FrameLayout
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private var app: String? = null
    private var sender: String? = null
    private var selectedId: Long? = null
    private var suggestion: String? = null
    private var pendingId: Long? = null

    private val appRow = LinearLayout(ctx)
    private val contactsBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    private val homePane = ScrollView(ctx)
    private val convPane = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
    private val titleView = tv("", 16f, 0xFF00E5FF.toInt())
    private val modeBtn = btn("") { toggleMode() }
    private val langBtn = btn("") { cycleLanguage() }
    private val personaBtn = btn("") { cyclePersona() }
    private val pendingRow = LinearLayout(ctx).apply { visibility = View.GONE }
    private val pendingLabel = tv("", 12f, 0xFFFFD166.toInt())
    private val messagesBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    private val suggestionLabel = tv("", 13f, 0xFFB26BFF.toInt())
    private val input = EditText(ctx).apply {
        hint = "Message..."
        setTextColor(Color.WHITE)
        setHintTextColor(0x88FFFFFF.toInt())
        textSize = 14f
    }

    init {
        root.addView(RobotBackgroundView(ctx), FrameLayout.LayoutParams(-1, -1))
        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16) }

        column.addView(HorizontalScrollView(ctx).apply { addView(appRow) })
        homePane.addView(contactsBox)
        column.addView(homePane, LinearLayout.LayoutParams(-1, 0, 1f))

        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(btn("←") { showHome() })
        header.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(modeBtn)
        convPane.addView(header)

        val settingsRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        settingsRow.addView(langBtn)
        settingsRow.addView(personaBtn)
        convPane.addView(settingsRow)

        pendingRow.orientation = LinearLayout.HORIZONTAL
        pendingRow.addView(pendingLabel, LinearLayout.LayoutParams(0, -2, 1f))
        pendingRow.addView(btn("Annuler") { pendingId?.let { AutoReplier.cancel(it) } })
        convPane.addView(pendingRow)

        convPane.addView(ScrollView(ctx).apply { addView(messagesBox) }, LinearLayout.LayoutParams(-1, 0, 1f))
        convPane.addView(suggestionLabel)

        val actions = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(btn("Utiliser") { suggestion?.let { input.setText(it) } })
        actions.addView(btn("🔄 Changer") { generateSuggestion(suggestion) })
        actions.addView(btn("Coller") { pasteIntoInput() })
        convPane.addView(actions)

        val sendRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        sendRow.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        sendRow.addView(btn("✈ Envoyer") { sendInput() })
        convPane.addView(sendRow)

        column.addView(convPane, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        // Le clavier n'est possible que si le champ de saisie a le focus
        input.setOnFocusChangeListener { _, hasFocus ->
            params.flags = if (hasFocus) {
                params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            wm.updateViewLayout(root, params)
        }

        AutoReplier.onUpdate = { refresh() }
        showHome()
    }

    fun onIncoming(msg: IncomingMessage) {
        if (MessageStore.settingsFor(msg.app, msg.sender).mode == Mode.AUTO) {
            AutoReplier.schedule(ctx, msg)
        } else if (app == msg.app && sender == msg.sender) {
            generateSuggestion()
        }
        refresh()
    }

    private fun refresh() {
        if (homePane.visibility == View.VISIBLE) refreshHome() else refreshConversation()
    }

    private fun showHome() {
        convPane.visibility = View.GONE
        homePane.visibility = View.VISIBLE
        refreshHome()
    }

    private fun refreshHome() {
        if (app == null) app = MessageStore.apps().firstOrNull()
        appRow.removeAllViews()
        MessageStore.apps().forEach { a ->
            appRow.addView(btn((if (a == app) "● " else "") + a) {
                app = a
                showHome()
            })
        }
        contactsBox.removeAllViews()
        val current = app ?: return
        MessageStore.contacts(current).forEach { c ->
            val last = MessageStore.history(current, c).last()
            val mode = if (MessageStore.settingsFor(current, c).mode == Mode.AUTO) "AUTO" else "SUGGESTION"
            contactsBox.addView(btn("$c  ·  $mode\n${last.text.take(60)}") { openConversation(current, c) })
        }
    }

    private fun openConversation(a: String, c: String) {
        app = a
        sender = c
        selectedId = null
        suggestion = null
        homePane.visibility = View.GONE
        convPane.visibility = View.VISIBLE
        if (settings().mode == Mode.SUGGESTION) generateSuggestion()
        refreshConversation()
    }

    private fun settings() = MessageStore.settingsFor(app!!, sender!!)

    private fun lastIncoming(): IncomingMessage? =
        MessageStore.history(app ?: return null, sender ?: return null).lastOrNull { !it.fromMe }

    private fun refreshConversation() {
        val a = app ?: return
        val c = sender ?: return
        val s = settings()
        titleView.text = "$c · $a"
        modeBtn.text = if (s.mode == Mode.AUTO) "⚡ Auto" else "💬 Suggestion"
        langBtn.text = "🌐 ${s.language.label}"
        personaBtn.text = "🎭 ${s.personality.label}"
        suggestionLabel.visibility = if (s.mode == Mode.SUGGESTION) View.VISIBLE else View.GONE

        val p = AutoReplier.pending.values.firstOrNull { it.msg.app == a && it.msg.sender == c }
        pendingId = p?.msg?.id
        if (p != null) {
            val secs = ((p.sendAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            pendingLabel.text = "⏳ Envoi dans ${secs}s : ${p.text}"
            pendingRow.visibility = View.VISIBLE
        } else {
            pendingRow.visibility = View.GONE
        }

        messagesBox.removeAllViews()
        MessageStore.history(a, c).forEach { messagesBox.addView(messageCell(it)) }
    }

    private fun messageCell(m: IncomingMessage): View {
        val who = if (m.fromMe) "Steve" else m.sender
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            background = bubbleBg(if (m.fromMe) 0x3300E5FF else 0x33B26BFF)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 0) }
        }
        box.addView(tv("$who : ${m.text}", 13f).apply {
            setOnClickListener {
                selectedId = if (selectedId == m.id) null else m.id
                refreshConversation()
            }
        })
        MessageStore.translations[m.id]?.let { box.addView(tv("🌐 $it", 12f, 0xFFB0FFD0.toInt())) }

        val row = LinearLayout(ctx).apply { gravity = Gravity.END }
        row.addView(btn("🌐 FR") { translate(m) })
        box.addView(row)

        if (selectedId == m.id) {
            val actions = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(btn("Copier") {
                clipboard.setPrimaryClip(ClipData.newPlainText("help-steve", m.text))
            })
            actions.addView(btn("Coller") { pasteIntoInput() })
            actions.addView(btn("Supprimer") {
                MessageStore.delete(m.id)
                selectedId = null
                refreshConversation()
            })
            box.addView(actions)
        }
        return box
    }

    private fun translate(m: IncomingMessage) {
        scope.launch {
            val t = withContext(Dispatchers.IO) { AiEngine.translate(m.text) }
            MessageStore.translations[m.id] = t
            refreshConversation()
        }
    }

    private fun generateSuggestion(avoid: String? = null) {
        val last = lastIncoming() ?: return
        suggestionLabel.text = "✨ Génération..."
        scope.launch {
            val s = withContext(Dispatchers.IO) { AiEngine.reply(last, avoid) }
            suggestion = s
            suggestionLabel.text = "✨ $s"
        }
    }

    private fun toggleMode() {
        val s = settings()
        if (s.mode == Mode.SUGGESTION) {
            if (!s.personality.autoAllowed) {
                toast("Ce mode reste en suggestions pour la personnalité Drague")
                return
            }
            s.mode = Mode.AUTO
            lastIncoming()?.let { AutoReplier.schedule(ctx, it) }
        } else {
            s.mode = Mode.SUGGESTION
            AutoReplier.pending.values
                .filter { it.msg.app == app && it.msg.sender == sender }
                .toList()
                .forEach { AutoReplier.cancel(it.msg.id) }
            generateSuggestion()
        }
        refreshConversation()
    }

    private fun cycleLanguage() {
        val s = settings()
        val all = Language.values()
        s.language = all[(s.language.ordinal + 1) % all.size]
        refreshConversation()
    }

    private fun cyclePersona() {
        val s = settings()
        val all = Personality.values()
        s.personality = all[(s.personality.ordinal + 1) % all.size]
        if (!s.personality.autoAllowed && s.mode == Mode.AUTO) {
            s.mode = Mode.SUGGESTION
            toast("Drague : retour en mode suggestions")
        }
        refreshConversation()
    }

    private fun sendInput() {
        val last = lastIncoming() ?: return toast("Aucun message auquel répondre")
        val text = input.text.toString().trim()
        if (ReplySender.send(ctx, last, text)) {
            input.setText("")
            suggestionLabel.text = "✅ Envoyé"
            refreshConversation()
        } else {
            toast("Envoi impossible pour cette notification")
        }
    }

    private fun pasteIntoInput() {
        val clip = clipboard.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: return
        input.setText(clip)
    }

    private fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

    private fun tv(t: String, size: Float = 14f, color: Int = Color.WHITE) = TextView(ctx).apply {
        text = t
        textSize = size
        setTextColor(color)
        setPadding(8, 6, 8, 6)
    }

    private fun btn(t: String, onClick: () -> Unit) = Button(ctx).apply {
        text = t
        textSize = 12f
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun bubbleBg(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = 20f
    }
}
