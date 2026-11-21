package com.steve.helpsteve

data class IncomingMessage(
    val app: String,
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    var aiReplied: Boolean = false
)

object MessageStore {
    val messages = mutableListOf<IncomingMessage>()
    var onNew: ((IncomingMessage) -> Unit)? = null

    fun add(msg: IncomingMessage) {
        messages.add(msg)
        onNew?.invoke(msg)
    }
}
