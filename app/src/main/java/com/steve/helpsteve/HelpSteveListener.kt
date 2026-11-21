package com.steve.helpsteve

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class HelpSteveListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName || !AppInfo.isMessaging(pkg)) return
        val extras = sbn.notification.extras
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        // Ignorer les notifications des messages envoyés par l'IA
        if (MessageStore.sentByAi.remove(text)) return
        val action = sbn.notification.actions?.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
        val target = action?.let { a -> a.remoteInputs?.let { ReplyTarget(a.actionIntent, it) } }
        MessageStore.add(AppInfo.name(pkg), sender, text, target = target)
    }
}
