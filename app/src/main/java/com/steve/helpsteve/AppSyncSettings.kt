package com.steve.helpsteve

import android.content.Context

object AppSyncSettings {
    private const val PREFS = "help_steve_sync"

    fun isEnabled(context: Context, pkg: String): Boolean {
        if (!AppInfo.isMessaging(pkg)) return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(pkg, true)
    }

    fun setEnabled(context: Context, pkg: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(pkg, enabled)
            .apply()
    }
}
