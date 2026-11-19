package com.steve.helpsteve

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MessageNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val message = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotBlank() || message.isNotBlank()) {
            Log.d("HELP_STEVE", "${sbn.packageName}: $title - $message")
        }
    }
}
