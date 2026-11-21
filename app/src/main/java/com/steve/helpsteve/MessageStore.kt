package com.steve.helpsteve

import android.app.PendingIntent
import android.app.RemoteInput

enum class Mode { SUGGESTION, AUTO }

data class IncomingMessage(
    val id: Long,
    val app: String,
    val sender: String,
    val text: String,
    val fromMe: Boolean = false,
    val time: Long = System.currentTimeMillis()
)

// Action de réponse fournie par la notification (RemoteInput), légitime et prévue par Android
class ReplyTarget(val pending: PendingIntent, val inputs: Array<RemoteInput>)

class ContactSettings(
    var language: Language = Language.FR,
    var personality: Personality = Personality.FRIENDLY,
    var mode: Mode = Mode.SUGGESTION
)

object MessageStore {
    val messages = mutableListOf<IncomingMessage>()
    val replyTargets = HashMap<Long, ReplyTarget>()
    val translations = HashMap<Long, String>()
    val settings = HashMap<String, ContactSettings>()
    val sentByAi = HashSet<String>()
    private var nextId = 1L

    var onIncoming: ((IncomingMessage) -> Unit)? = null

    fun settingsFor(app: String, sender: String): ContactSettings =
        settings.getOrPut("$app|$sender") { ContactSettings() }

    fun apps(): List<String> = messages.map { it.app }.distinct()

    fun contacts(app: String): List<String> =
        messages.filter { it.app == app }.map { it.sender }.distinct()

    fun history(app: String, sender: String): List<IncomingMessage> =
        messages.filter { it.app == app && it.sender == sender }

    fun add(app: String, sender: String, text: String, fromMe: Boolean = false, target: ReplyTarget? = null): IncomingMessage {
        val msg = IncomingMessage(nextId++, app, sender, text, fromMe)
        messages.add(msg)
        if (target != null) replyTargets[msg.id] = target
        if (!fromMe) onIncoming?.invoke(msg)
        return msg
    }

    fun delete(id: Long) {
        messages.removeAll { it.id == id }
        replyTargets.remove(id)
        translations.remove(id)
    }
}
