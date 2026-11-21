package com.steve.helpsteve

object AiEngine {
    fun reply(msg: IncomingMessage, avoid: String? = null): String {
        val s = MessageStore.settingsFor(msg.app, msg.sender)
        val history = MessageStore.history(msg.app, msg.sender).takeLast(20)
            .joinToString("\n") { (if (it.fromMe) "Steve" else it.sender) + " : " + it.text }
        val prompt = buildString {
            append("Historique de la conversation :\n$history\n\n")
            append("Ne pose pas une question déjà posée dans cet historique.\n")
            if (avoid != null) append("Propose une réponse différente de : $avoid\n")
            append("Rédige la réponse au dernier message reçu.")
        }
        return GeminiClient.ask(Prompts.system(s.personality, s.language), prompt)
    }

    fun translate(text: String): String =
        GeminiClient.ask("Tu es un traducteur. Réponds uniquement par la traduction en français.", text)
}
