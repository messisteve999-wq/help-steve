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

// Mode automatique : délai aléatoire de 30 à 90 secondes, annulable
object AutoReplier {
    class Pending(val msg: IncomingMessage, val text: String, val sendAt: Long, val job: Job)

    val pending = LinkedHashMap<Long, Pending>()
    var onUpdate: (() -> Unit)? = null
    var onSent: ((IncomingMessage) -> Unit)? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun schedule(ctx: Context, msg: IncomingMessage) {
        scope.launch {
            val text = withContext(Dispatchers.IO) { AiEngine.reply(msg) }
            if (text.startsWith("Erreur")) return@launch
            val delayMs = Random.nextLong(30_000L, 90_001L)
            val job = scope.launch {
                delay(delayMs)
                if (ReplySender.send(ctx, msg, text)) onSent?.invoke(msg)
                pending.remove(msg.id)
                onUpdate?.invoke()
            }
            pending[msg.id] = Pending(msg, text, System.currentTimeMillis() + delayMs, job)
            onUpdate?.invoke()
        }
    }

    fun cancel(id: Long) {
        pending.remove(id)?.job?.cancel()
        onUpdate?.invoke()
    }
}
