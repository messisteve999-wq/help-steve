package com.steve.helpsteve

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    // À remplacer par l'adresse réelle du serveur-gemini sur Render
    private const val SERVER_URL = "https://VOTRE-SERVEUR.onrender.com/suggest"

    fun suggest(message: String): String {
        return try {
            val conn = (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }
            conn.outputStream.use { it.write(JSONObject().put("message", message).toString().toByteArray()) }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).optString("text", "Pas de réponse")
        } catch (e: Exception) {
            "Erreur : ${e.message}"
        }
    }
}
