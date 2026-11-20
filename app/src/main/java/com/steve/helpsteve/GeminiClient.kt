package com.steve.helpsteve

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

object GeminiClient {

    private const val PREFS = "help_steve_settings"
    private const val TOKEN_KEY = "server_access_token"
    private const val ENDPOINT = "http://127.0.0.1:8787/v1/generate"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN_KEY, token.trim())
            .apply()
    }

    fun hasToken(context: Context): Boolean {
        return !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, "")
            .isNullOrBlank()
    }

    fun generate(
        context: Context,
        prompt: String,
        systemInstruction: String,
        callback: (Result<String>) -> Unit
    ) {
        val token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, "")
            .orEmpty()

        if (token.isBlank()) {
            callbackOnMain(callback, Result.failure(
                IllegalStateException("Configure le token du serveur dans HELP-STEVE.")
            ))
            return
        }

        executor.execute {
            val result = runCatching {
                val connection = URL(ENDPOINT).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 60000
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Authorization", "Bearer $token"
                    )
                    connection.setRequestProperty(
                        "Content-Type", "application/json; charset=utf-8"
                    )

                    val body = JSONObject()
                        .put("prompt", prompt)
                        .put("systemInstruction", systemInstruction)
                        .toString()

                    connection.outputStream.use {
                        it.write(body.toByteArray(StandardCharsets.UTF_8))
                    }

                    val status = connection.responseCode
                    val stream = if (status in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                    val responseText = stream?.bufferedReader()?.use {
                        it.readText()
                    }.orEmpty()

                    val json = JSONObject(responseText)

                    if (status !in 200..299 || !json.optBoolean("success")) {
                        throw IllegalStateException(
                            json.optString("error", "Erreur serveur HTTP $status")
                        )
                    }

                    json.optString("text").trim().also {
                        if (it.isBlank()) {
                            throw IllegalStateException("Gemini a retourné une réponse vide.")
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }

            callbackOnMain(callback, result)
        }
    }

    private fun callbackOnMain(
        callback: (Result<String>) -> Unit,
        result: Result<String>
    ) {
        mainHandler.post {
            callback(result)
        }
    }
}
