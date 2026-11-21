package com.steve.helpsteve

object AiEngine {

    fun replyParts(msg: IncomingMessage, avoid: String? = null): List<String> {
        val s = MessageStore.settingsFor(msg.app, msg.sender)

        val history = MessageStore.history(msg.app, msg.sender)
            .takeLast(20)
            .joinToString("\n") {
                (if (it.fromMe) "Steve" else it.sender) + " : " + it.text
            }

        val prompt = buildString {
            append("HISTORIQUE DE LA CONVERSATION :\n")
            append(history)
            append("\n\n")

            append("MESSAGE AUQUEL TU DOIS RÉPONDRE :\n")
            append(msg.text)
            append("\n\n")

            append("INSTRUCTIONS :\n")
            append("- Réponds directement à ce message.\n")
            append("- Fais une réponse courte et naturelle.\n")
            append("- Une ou deux phrases maximum si possible.\n")
            append("- Pose une question uniquement si elle est pertinente.\n")
            append("- Ne parle pas spontanément de Steve.\n")
            append("- Ne répète pas une question déjà posée.\n")

            if (avoid != null) {
                append("- Évite cette réponse précédente : $avoid\n")
            }

            append("\nGénère uniquement la réponse à envoyer.")
        }

        val raw = GeminiClient.ask(
            Prompts.system(s.personality, s.language),
            prompt
        )

        if (raw.startsWith("Erreur")) {
            return listOf(raw)
        }

        val response = raw.split("[[MESSAGE]]")
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.trim('"', '“', '”', '\'')

        return if (response.isNullOrBlank()) {
            listOf("Je n'ai pas réussi à générer une réponse.")
        } else {
            listOf(response)
        }
    }

    fun reply(msg: IncomingMessage, avoid: String? = null): String =
        replyParts(msg, avoid).joinToString("\n\n")

    fun translate(text: String): String =
        GeminiClient.ask(
            "Tu es un traducteur. Réponds uniquement par la traduction en français.",
            text
        )
}
