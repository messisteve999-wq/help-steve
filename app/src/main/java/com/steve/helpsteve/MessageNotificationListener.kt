package com.steve.helpsteve

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MessageNotificationListener : NotificationListenerService() {

    private val supportedApps = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.google.android.gm"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in supportedApps) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val message = (
            extras.getCharSequence("android.bigText")
                ?: extras.getCharSequence("android.text")
            )?.toString()?.trim().orEmpty()

        if (title.isBlank() || message.isBlank()) return

        MessageStore.save(
            context = applicationContext,
            packageName = sbn.packageName,
            title = title,
            message = message,
            timestamp = sbn.postTime
        )

        val contact = ContactSettingsStore.get(
            applicationContext,
            sbn.packageName,
            title
        )

        val systemInstruction = """
            Tu es HELP-STEVE, un assistant qui aide l'utilisateur à répondre à ses messages.
            Personnalité demandée : ${contact.personality}.
            Langue obligatoire : ${contact.language}.
            Écris une seule réponse naturelle, courte et directement envoyable.
            Ne donne ni explication, ni introduction, ni liste, ni guillemets.
            N'invente aucun fait et respecte le contexte du message.
        """.trimIndent()

        val prompt = """
            Propose une réponse au message reçu de $title :
            "$message"

            Retourne uniquement le texte de la réponse.
        """.trimIndent()

        Log.d("HELP_STEVE", "${sbn.packageName}: $title - $message")

        // Le mode Automatique ne déclenche jamais un envoi. V1.5 ne fait que proposer du texte.
        GeminiClient.generate(applicationContext, prompt, systemInstruction) { result ->
            result.onSuccess { suggestion ->
                val intent = Intent(FloatingService.ACTION_SHOW_SUGGESTION).apply {
                    setPackage(packageName)
                    putExtra(FloatingService.EXTRA_CONTACT, title)
                    putExtra(FloatingService.EXTRA_PACKAGE, sbn.packageName)
                    putExtra(FloatingService.EXTRA_MESSAGE, message)
                    putExtra(FloatingService.EXTRA_SUGGESTION, suggestion.trim())
                    putExtra(FloatingService.EXTRA_PERSONALITY, contact.personality)
                    putExtra(FloatingService.EXTRA_LANGUAGE, contact.language)
                }
                sendBroadcast(intent)
            }.onFailure { error ->
                Log.e("HELP_STEVE", "Génération de suggestion impossible", error)
            }
        }
    }
}
