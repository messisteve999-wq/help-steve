package com.steve.helpsteve

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class MessageNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "HELP_STEVE"

        // Évite de traiter plusieurs fois la même notification.
        private val processedNotifications = ConcurrentHashMap<String, Long>()

        private fun markAsNewNotification(key: String): Boolean {
            val now = System.currentTimeMillis()

            synchronized(processedNotifications) {
                val iterator = processedNotifications.entries.iterator()

                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value > 10 * 60 * 1000L) {
                        iterator.remove()
                    }
                }

                val previousTime = processedNotifications[key]

                // Bloque les répétitions rapprochées, notamment
                // lorsqu'une application actualise sa notification
                // après une réponse automatique.
                if (previousTime != null &&
                    now - previousTime < 2 * 60 * 1000L
                ) {
                    return false
                }

                processedNotifications[key] = now

                while (processedNotifications.size > 300) {
                    val oldest = processedNotifications.entries
                        .minByOrNull { it.value } ?: break
                    processedNotifications.remove(oldest.key)
                }
            }

            return true
        }
    }

    private val supportedApps = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.google.android.gm"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in supportedApps) return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras
            .getCharSequence("android.title")
            ?.toString()
            ?.trim()
            .orEmpty()

        val message = (
            extras.getCharSequence("android.bigText")
                ?: extras.getCharSequence("android.text")
            )?.toString()?.trim().orEmpty()

        if (title.isBlank() || message.isBlank()) return

        // Ignore les notifications qui ne représentent pas un message exploitable.
        val eventKey = "${sbn.packageName}|${sbn.key}|${title.trim().lowercase()}|${message.hashCode()}"

        if (!markAsNewNotification(eventKey)) {
            Log.d(TAG, "Notification déjà traitée")
            return
        }

        MessageStore.save(
            context = applicationContext,
            packageName = sbn.packageName,
            title = title,
            message = message,
            timestamp = sbn.postTime
        )

        val prefs = getSharedPreferences(
            "help_steve_settings",
            Context.MODE_PRIVATE
        )

        val hasPersonalSettings = ContactSettingsStore.hasCustomSettings(
            applicationContext,
            sbn.packageName,
            title
        )

        // Les réglages du contact sont prioritaires.
        // Sinon, on utilise les réglages généraux.
        val settings = if (hasPersonalSettings) {
            ContactSettingsStore.get(
                applicationContext,
                sbn.packageName,
                title
            )
        } else {
            ContactSettings(
                personality = prefs.getString("personality", "Amical")
                    ?: "Amical",
                responseMode = prefs.getString("responseMode", "Suggestions")
                    ?: "Suggestions",
                language = prefs.getString("language", "Français")
                    ?: "Français"
            )
        }

        // Le mode Automatique exige une configuration explicite du contact.
// Le mode général ne peut jamais autoriser un envoi automatique.
        val automaticMode = hasPersonalSettings &&
            settings.responseMode.equals(
                "Automatique",
                ignoreCase = true
            )

        if (automaticMode) {
            Log.d(TAG, "Mode Automatique explicitement activé pour $title")
        } else {
            Log.d(TAG, "Mode Suggestions appliqué pour $title")
        }

        val systemInstruction = """
            Tu es HELP-STEVE, un assistant qui aide l'utilisateur à répondre à ses messages.
            Personnalité demandée : ${settings.personality}.
            Langue obligatoire : ${settings.language}.
            Écris une seule réponse naturelle, courte et directement envoyable.
            Ne donne ni explication, ni introduction, ni liste, ni guillemets.
            N'invente aucun fait et respecte le contexte du message.
        """.trimIndent()

        val prompt = """
            Propose une réponse au message reçu de $title :
            "$message"

            Retourne uniquement le texte de la réponse.
        """.trimIndent()

        // Ne jamais écrire le contenu privé du message dans les logs.
        Log.d(
            TAG,
            "Message reçu depuis ${sbn.packageName}, mode=${settings.responseMode}"
        )

        GeminiClient.generate(
            applicationContext,
            prompt,
            systemInstruction
        ) { result ->

            result.onSuccess { generatedText ->

                val suggestion = generatedText.trim()

                if (suggestion.isBlank()) {
                    Log.w(TAG, "Gemini a retourné une réponse vide")
                    return@onSuccess
                }

                // L'envoi automatique exige une action RemoteInput compatible.
                if (automaticMode) {
                    val sent = try {
                        AutoReplyManager.sendReply(
                            applicationContext,
                            notification,
                            suggestion
                        )
                    } catch (error: Exception) {
                        Log.e(TAG, "Erreur pendant la tentative automatique", error)
                        false
                    }

                    if (sent) {
                        // Android a accepté l'envoi de l'action.
                        // Cela ne garantit pas la réception par le destinataire.
                        Log.d(TAG, "Action de réponse automatique transmise à Android")
                        return@onSuccess
                    }

                    Log.w(
                        TAG,
                        "Envoi automatique impossible : affichage d'une suggestion manuelle"
                    )
                }

                // Mode Suggestions ou échec de l'envoi automatique :
                // l'utilisateur reçoit une suggestion à envoyer lui-même.
                showSuggestion(
                    contact = title,
                    appPackage = sbn.packageName,
                    message = message,
                    suggestion = suggestion,
                    personality = settings.personality,
                    language = settings.language
                )

            }.onFailure { error ->
                Log.e(TAG, "Génération de réponse impossible", error)
            }
        }
    }

    private fun showSuggestion(
        contact: String,
        appPackage: String,
        message: String,
        suggestion: String,
        personality: String,
        language: String
    ) {
        val intent = Intent(FloatingService.ACTION_SHOW_SUGGESTION).apply {
            setPackage(packageName)

            putExtra(FloatingService.EXTRA_CONTACT, contact)
            putExtra(FloatingService.EXTRA_PACKAGE, appPackage)
            putExtra(FloatingService.EXTRA_MESSAGE, message)
            putExtra(FloatingService.EXTRA_SUGGESTION, suggestion)
            putExtra(FloatingService.EXTRA_PERSONALITY, personality)
            putExtra(FloatingService.EXTRA_LANGUAGE, language)
        }

        sendBroadcast(intent)
    }
}
