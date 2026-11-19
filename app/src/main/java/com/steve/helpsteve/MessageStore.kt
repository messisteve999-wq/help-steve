package com.steve.helpsteve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedMessage(
    val packageName: String,
    val title: String,
    val message: String,
    val timestamp: Long
)

object MessageStore {

    private const val PREFS = "help_steve_history"
    private const val KEY_MESSAGES = "messages"
    private const val MAX_MESSAGES = 100

    fun save(
        context: Context,
        packageName: String,
        title: String,
        message: String,
        timestamp: Long
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = try {
            JSONArray(prefs.getString(KEY_MESSAGES, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        val updated = JSONArray()
        val newMessage = JSONObject()
            .put("packageName", packageName)
            .put("title", title)
            .put("message", message)
            .put("timestamp", timestamp)

        updated.put(newMessage)

        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue

            val duplicate =
                item.optString("packageName") == packageName &&
                item.optString("title") == title &&
                item.optString("message") == message &&
                item.optLong("timestamp") == timestamp

            if (!duplicate && updated.length() < MAX_MESSAGES) {
                updated.put(item)
            }
        }

        prefs.edit().putString(KEY_MESSAGES, updated.toString()).apply()
    }

    fun getAll(context: Context): List<SavedMessage> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = try {
            JSONArray(prefs.getString(KEY_MESSAGES, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }

        val result = mutableListOf<SavedMessage>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(
                SavedMessage(
                    packageName = item.optString("packageName"),
                    title = item.optString("title"),
                    message = item.optString("message"),
                    timestamp = item.optLong("timestamp")
                )
            )
        }

        return result
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_MESSAGES)
            .apply()
    }
}
