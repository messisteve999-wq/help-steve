package com.steve.helpsteve

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    // À remplacer par l'adresse réelle du serveur-gemini sur Render
    private const val SERVER_URL = "https://VOTRE-SERVEUR.onrender.com/suggest"

    fun ask(system: String, prompt: String): String = try {
        val conn = (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 30000
        }
        val body = JSONObject().put("system", system).put("message", prompt).toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            .optString("text", "Pas de réponse").trim()
    } catch (e: Exception) {
        "Erreur : ${e.message}"
    }
}
