package com.steve.helpsteve

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

object AutoReplyManager {

    private const val TAG = "HELP_STEVE_REPLY"

    /**
     * Vérifie la présence d'une véritable action REPLY
     * avec une saisie textuelle libre.
     */
    fun hasCompatibleReplyAction(notification: Notification): Boolean {
        return findReplyAction(notification) != null
    }

    /**
     * Envoie uniquement via une action explicitement identifiée REPLY.
     */
    fun sendReply(
        context: Context,
        notification: Notification,
        replyText: String
    ): Boolean {

        if (replyText.isBlank()) return false

        val action = findReplyAction(notification) ?: return false

        val remoteInput = action.remoteInputs
            ?.firstOrNull { it.allowFreeFormInput }
            ?: return false

        return try {
            val fillInIntent = Intent()

            val results = Bundle().apply {
                putCharSequence(
                    remoteInput.resultKey,
                    replyText.trim()
                )
            }

            RemoteInput.addResultsToIntent(
                arrayOf(remoteInput),
                fillInIntent,
                results
            )

            action.actionIntent.send(
                context,
                0,
                fillInIntent
            )

            Log.d(TAG, "Action REPLY transmise à Android")
            true

        } catch (error: Exception) {
            Log.e(TAG, "Échec de l'action REPLY", error)
            false
        }
    }

    private fun findReplyAction(
        notification: Notification
    ): Notification.Action? {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }

        val actions = notification.actions ?: return null

        return actions.firstOrNull { action ->
            action.semanticAction ==
                Notification.Action.SEMANTIC_ACTION_REPLY &&
                action.remoteInputs?.any {
                    it.allowFreeFormInput
                } == true
        }
    }
}
