package com.steve.helpsteve

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

object AutoReplyManager {

    fun hasCompatibleReplyAction(notification: Notification): Boolean {
        return findReplyAction(notification) != null
    }

    fun sendReply(
        context: Context,
        notification: Notification,
        replyText: String
    ): Boolean {
        if (replyText.isBlank()) return false

        val action = findReplyAction(notification) ?: return false
        val remoteInputs = action.remoteInputs ?: return false

        val freeFormInputs = remoteInputs.filter {
            it.allowFreeFormInput
        }

        if (freeFormInputs.isEmpty()) return false

        return try {
            val intent = Intent()
            val results = Bundle()

            freeFormInputs.forEach { input ->
                results.putCharSequence(input.resultKey, replyText)
            }

            RemoteInput.addResultsToIntent(
                freeFormInputs.toTypedArray(),
                intent,
                results
            )

            action.actionIntent.send(context, 0, intent)
            true
        } catch (error: Exception) {
            Log.e("HELP_STEVE_REPLY", "Échec de l'envoi RemoteInput", error)
            false
        }
    }

    private fun findReplyAction(
        notification: Notification
    ): Notification.Action? {
        val actions = notification.actions ?: return null

        return actions.firstOrNull { action ->
            val inputs = action.remoteInputs
            !action.actionIntent.isCanceled &&
                !inputs.isNullOrEmpty() &&
                inputs.any { it.allowFreeFormInput }
        }
    }
}
