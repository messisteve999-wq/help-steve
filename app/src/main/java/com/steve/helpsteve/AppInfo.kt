package com.steve.helpsteve

object AppInfo {
    private val known = linkedMapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "com.facebook.orca" to "Messenger",
        "org.telegram.messenger" to "Telegram",
        "com.instagram.android" to "Instagram",
        "com.snapchat.android" to "Snapchat",
        "com.google.android.apps.messaging" to "Messages",
        "com.samsung.android.messaging" to "Messages Samsung",
        "com.google.android.gm" to "Gmail",
        "com.microsoft.office.outlook" to "Outlook",
        "com.discord" to "Discord",
        "com.skype.raider" to "Skype"
    )

    fun isMessaging(pkg: String) = known.containsKey(pkg)

    fun name(pkg: String) = known[pkg] ?: pkg

    fun supportedApps(): List<Pair<String, String>> =
        known.entries.map { it.key to it.value }
}
