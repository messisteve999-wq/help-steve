package com.steve.helpsteve

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

object AutoReplier {

    class Pending(
        val msg: IncomingMessage,
        val parts: List<String>,
        val sendAt: Long,
        val job: Job
    ) {
        val text: String
            get() = parts.joinToString("\n\n")
    }

    val pending = LinkedHashMap<Long, Pending>()

    var onUpdate: (() -> Unit)? = null
    var onSent: ((IncomingMessage) -> Unit)? = null

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun canAutoReply(msg: IncomingMessage): Boolean {
        val settings = MessageStore.settingsFor(msg.app, msg.sender)

        return settings.mode == Mode.AUTO &&
            settings.personality.autoAllowed
    }

    fun schedule(ctx: Context, msg: IncomingMessage) {

        if (pending.containsKey(msg.id)) return
        if (!canAutoReply(msg)) return

        scope.launch {

            val parts = withContext(Dispatchers.IO) {
                AiEngine.replyParts(msg).take(1)
            }

            if (!canAutoReply(msg)) {
                onUpdate?.invoke()
                return@launch
            }

            if (parts.isEmpty() || parts.any { it.startsWith("Erreur") }) {
                onUpdate?.invoke()
                return@launch
            }

            val delayMs = Random.nextLong(30_000L, 90_001L)

            val job = scope.launch {

                delay(delayMs)

                if (!pending.containsKey(msg.id)) {
                    return@launch
                }

                if (!canAutoReply(msg)) {
                    pending.remove(msg.id)
                    onUpdate?.invoke()
                    return@launch
                }

                val sent = ReplySender.send(ctx, msg, parts.first())

                pending.remove(msg.id)

                if (sent) {
                    onSent?.invoke(msg)
                }

                onUpdate?.invoke()
            }

            pending[msg.id] = Pending(
                msg,
                parts,
                System.currentTimeMillis() + delayMs,
                job
            )

            onUpdate?.invoke()
        }
    }

    fun cancel(id: Long) {
        pending.remove(id)?.job?.cancel()
        onUpdate?.invoke()
    }
}
