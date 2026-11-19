package com.steve.helpsteve

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MessageNotificationListener : NotificationListenerService() {

    private val supportedApps = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.google.android.gm"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in supportedApps) return

        val extras = sbn.notification.extras

        val title = extras
            .getCharSequence("android.title")
            ?.toString()
            ?.trim()
            .orEmpty()

        val message = (
            extras.getCharSequence("android.bigText")
                ?: extras.getCharSequence("android.text")
        )?.toString()?.trim().orEmpty()

        if (title.isBlank() && message.isBlank()) return

        MessageStore.save(
            context = applicationContext,
            packageName = sbn.packageName,
            title = title,
            message = message,
            timestamp = sbn.postTime
        )

        Log.d("HELP_STEVE", "${sbn.packageName}: $title - $message")
    }
}
