package com.steve.helpsteve

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.RemoteInput
import android.os.Bundle

object ReplySender {
    fun send(ctx: Context, msg: IncomingMessage, text: String): Boolean {
        if (text.isBlank() || text.startsWith("Erreur")) return false
        val target = MessageStore.replyTargets[msg.id] ?: return false
        return try {
            val intent = Intent()
            val results = Bundle()
            target.inputs.forEach { results.putCharSequence(it.resultKey, text) }
            RemoteInput.addResultsToIntent(target.inputs, intent, results)
            target.pending.send(ctx, 0, intent)
            MessageStore.sentByAi.add(text)
            MessageStore.add(msg.app, msg.sender, text, fromMe = true)
            true
        } catch (e: PendingIntent.CanceledException) {
            false
        }
    }
}
