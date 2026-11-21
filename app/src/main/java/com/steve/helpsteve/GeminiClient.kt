package com.steve.helpsteve

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    private const val SERVER_URL = "http://127.0.0.1:8787/v1/generate"

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun ask(system: String, prompt: String): String {
        val context = appContext
            ?: return "Erreur : client serveur non initialisé"

        val token = SecureTokenStore.read(context)
            ?: return "Erreur : configure le jeton du serveur dans help-steve"

        var connection: HttpURLConnection? = null

        return try {
            connection = (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }

            val body = JSONObject()
                .put("prompt", prompt)
                .put("systemInstruction", system)
                .toString()

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val response = JSONObject(responseText)

            if (status !in 200..299) {
                when (status) {
                    401 -> "Erreur : jeton serveur refusé"
                    400 -> "Erreur : requête invalide"
                    413 -> "Erreur : requête trop volumineuse"
                    else -> "Erreur : serveur HTTP $status"
                }
            } else if (!response.optBoolean("success", false)) {
                "Erreur : génération Gemini refusée"
            } else {
                response.optString("text", "").trim()
                    .ifEmpty { "Pas de réponse Gemini" }
            }
        } catch (e: Exception) {
            "Erreur : connexion au serveur impossible (${e.javaClass.simpleName})"
        } finally {
            connection?.disconnect()
        }
    }
}
