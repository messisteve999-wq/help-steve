package com.steve.helpsteve

object AiEngine {
    fun replyParts(msg: IncomingMessage, avoid: String? = null): List<String> {
        val s = MessageStore.settingsFor(msg.app, msg.sender)
        val history = MessageStore.history(msg.app, msg.sender).takeLast(20)
            .joinToString("\n") {
                (if (it.fromMe) "Steve" else it.sender) + " : " + it.text
            }

        val prompt = buildString {
            append("Historique de la conversation :\n")
            append(history)
            append("\n\n")
            append("Réponds au dernier message reçu en respectant la personnalité.\n")
            append("Génère entre 1 et 3 messages indépendants si cela améliore la conversation.\n")
            append("Sépare les messages avec le marqueur exact [[MESSAGE]].\n")
            append("Ne répète pas une question déjà posée.\n")
            if (avoid != null) append("Évite cette réponse précédente : $avoid\n")
            append("Dernier message reçu : ${msg.text}")
        }

        val raw = GeminiClient.ask(Prompts.system(s.personality, s.language), prompt)
        if (raw.startsWith("Erreur")) return listOf(raw)

        val parts = raw.split("[[MESSAGE]]")
            .map { it.trim().trim('"', '“', '”') }
            .filter { it.isNotBlank() }
            .take(3)

        return parts.ifEmpty { listOf(raw.trim()) }
    }

    fun reply(msg: IncomingMessage, avoid: String? = null): String =
        replyParts(msg, avoid).joinToString("\n\n")

    fun translate(text: String): String =
        GeminiClient.ask(
            "Tu es un traducteur. Réponds uniquement par la traduction en français.",
            text
        )
}
