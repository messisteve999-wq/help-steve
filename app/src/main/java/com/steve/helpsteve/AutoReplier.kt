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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun schedule(ctx: Context, msg: IncomingMessage) {
        // Évite de programmer plusieurs réponses pour le même message.
        if (pending.containsKey(msg.id)) return

        scope.launch {
            val parts = withContext(Dispatchers.IO) {
                AiEngine.replyParts(msg)
            }

            if (parts.isEmpty() || parts.any { it.startsWith("Erreur") }) {
                onUpdate?.invoke()
                return@launch
            }

            val delayMs = Random.nextLong(30_000L, 90_001L)

            val job = scope.launch {
                delay(delayMs)

                // Vérifie que la réponse est toujours en attente.
                if (!pending.containsKey(msg.id)) return@launch

                var allSent = true
                for ((index, part) in parts.withIndex()) {
                    if (!pending.containsKey(msg.id)) return@launch

                    if (!ReplySender.send(ctx, msg, part)) {
                        allSent = false
                        break
                    }

                    // Petite séparation entre les messages indépendants.
                    if (index < parts.lastIndex) delay(1_200L)
                }

                pending.remove(msg.id)
                if (allSent) onSent?.invoke(msg)
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
