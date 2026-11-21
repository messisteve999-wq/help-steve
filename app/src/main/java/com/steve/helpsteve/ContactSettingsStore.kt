package com.steve.helpsteve

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

data class ContactSettings(
    val personality: String = "Amical",
    val responseMode: String = "Suggestions",
    val language: String = "Français"
)

object ContactSettingsStore {

    private const val PREFS = "help_steve_contact_settings"

    private fun normalizePackageName(value: String): String {
        return when (value.trim()) {
            "WhatsApp" -> "com.whatsapp"
            "WhatsApp Business" -> "com.whatsapp.w4b"
            "Messenger" -> "com.facebook.orca"
            "Telegram" -> "org.telegram.messenger"
            "Gmail" -> "com.google.android.gm"
            else -> value.trim()
        }
    }

    private fun aliases(packageName: String): List<String> {
        return when (normalizePackageName(packageName)) {
            "com.whatsapp" -> listOf("com.whatsapp", "WhatsApp")
            "com.whatsapp.w4b" -> listOf("com.whatsapp.w4b", "WhatsApp Business")
            "com.facebook.orca" -> listOf("com.facebook.orca", "Messenger")
            "org.telegram.messenger" -> listOf("org.telegram.messenger", "Telegram")
            "com.google.android.gm" -> listOf("com.google.android.gm", "Gmail")
            else -> listOf(packageName.trim())
        }
    }

    private fun makeKey(packageName: String, contact: String): String {
        val raw = "${packageName.trim()}|${contact.trim()}"
        val encoded = Base64.encodeToString(
            raw.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "contact_$encoded"
    }

    private fun hasSettings(prefs: SharedPreferences, key: String): Boolean {
        return prefs.contains("${key}_personality") ||
            prefs.contains("${key}_mode") ||
            prefs.contains("${key}_language")
    }

    private fun readSettings(
        prefs: SharedPreferences,
        key: String
    ): ContactSettings {
        return ContactSettings(
            personality = prefs.getString("${key}_personality", "Amical") ?: "Amical",
            responseMode = prefs.getString("${key}_mode", "Suggestions") ?: "Suggestions",
            language = prefs.getString("${key}_language", "Français") ?: "Français"
        )
    }

    fun hasCustomSettings(
        context: Context,
        packageName: String,
        contact: String
    ): Boolean {
        if (contact.isBlank()) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        for (alias in aliases(packageName)) {
            val key = makeKey(alias, contact)
            if (hasSettings(prefs, key)) return true
        }

        return false
    }

    fun get(
        context: Context,
        packageName: String,
        contact: String
    ): ContactSettings {
        if (contact.isBlank()) return ContactSettings()

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val canonicalKey = makeKey(normalizePackageName(packageName), contact)

        if (hasSettings(prefs, canonicalKey)) {
            return readSettings(prefs, canonicalKey)
        }

        // Récupère les anciennes clés utilisant les noms affichés.
        for (alias in aliases(packageName)) {
            val oldKey = makeKey(alias, contact)

            if (oldKey != canonicalKey && hasSettings(prefs, oldKey)) {
                val oldSettings = readSettings(prefs, oldKey)
                save(context, packageName, contact, oldSettings)
                return oldSettings
            }
        }

        return ContactSettings()
    }

    fun save(
        context: Context,
        packageName: String,
        contact: String,
        settings: ContactSettings
    ) {
        if (contact.isBlank()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = makeKey(normalizePackageName(packageName), contact)

        prefs.edit()
            .putString("${key}_personality", settings.personality)
            .putString("${key}_mode", settings.responseMode)
            .putString("${key}_language", settings.language)
            .apply()
    }

    fun delete(
        context: Context,
        packageName: String,
        contact: String
    ) {
        if (contact.isBlank()) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        for (alias in aliases(packageName)) {
            val key = makeKey(alias, contact)
            editor.remove("${key}_personality")
                .remove("${key}_mode")
                .remove("${key}_language")
        }

        editor.apply()
    }
}
