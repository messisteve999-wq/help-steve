package com.steve.helpsteve

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

// Reçoit les notifications des applications de messagerie.
// TODO v2.2 : ignorer les notifications envoyées par l'IA.
class HelpSteveListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        MessageStore.add(IncomingMessage(app = sbn.packageName, sender = title, text = text))
    }
}
