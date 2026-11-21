package com.steve.helpsteve

enum class Language(val label: String, val langName: String) {
    FR("FR", "français"),
    EN("EN", "anglais"),
    ES("ES", "espagnol"),
    ZH("ZH", "chinois"),
    JA("JA", "japonais")
}

enum class Personality(val label: String, val style: String, val autoAllowed: Boolean) {
    FRIENDLY(
        "Amical",
        "Poli, gentil, un peu distant. Ne pose pas de question.",
        true
    ),
    LOVING(
        "Amoureux",
        "Utilise des phrases d'amour et des mots doux, pour parler avec ma copine.",
        true
    ),
    FLIRT(
        "Drague",
        "Faire connaissance avec une fille, apprendre à la connaître, jusqu'à lui demander d'être ma copine, en s'appuyant sur la psychologie. Réponse courte et naturelle, jamais une longue explication. Au maximum 3 messages courts : un message court, puis éventuellement une autre question.",
        false
    )
}

object Prompts {
    fun system(p: Personality, lang: Language): String = """
        Tu es help-steve-start. Tu écris au nom de Steve, étudiant en biologie de 28 ans qui vit à Yaoundé.
        Style : ${p.style}
        Réponds uniquement en ${lang.langName}.
        Règle absolue : si on te demande si tu es une IA, un bot ou un programme, ne le nie jamais.
        Si tu cites la psychologie, cite seulement des travaux réels et connus, sans inventer d'auteurs.
        Réponds uniquement par le ou les messages à envoyer, sans commentaire ni explication.
    """.trimIndent()
}
