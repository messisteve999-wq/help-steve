package com.steve.helpsteve

import android.content.Context
import android.util.Base64

data class ContactSettings(
    val personality: String = "Amical",
    val responseMode: String = "Suggestions",
    val language: String = "Français"
)

object ContactSettingsStore {

    private const val PREFS = "help_steve_contact_settings"

    private fun contactKey(packageName: String, contact: String): String {
        val raw = "$packageName|$contact"
        val encoded = Base64.encodeToString(
            raw.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "contact_$encoded"
    }

    fun get(
        context: Context,
        packageName: String,
        contact: String
    ): ContactSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = contactKey(packageName, contact)

        return ContactSettings(
            personality = prefs.getString("${key}_personality", "Amical") ?: "Amical",
            responseMode = prefs.getString("${key}_mode", "Suggestions") ?: "Suggestions",
            language = prefs.getString("${key}_language", "Français") ?: "Français"
        )
    }

    fun save(
        context: Context,
        packageName: String,
        contact: String,
        settings: ContactSettings
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = contactKey(packageName, contact)

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
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = contactKey(packageName, contact)

        prefs.edit()
            .remove("${key}_personality")
            .remove("${key}_mode")
            .remove("${key}_language")
            .apply()
    }
}
