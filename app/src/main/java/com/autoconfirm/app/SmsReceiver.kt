package com.autoconfirm.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val prefs = context.getSharedPreferences("autoconfirm", Context.MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "") ?: return
        val token = prefs.getString("token", "") ?: return
        if (serverUrl.isEmpty() || token.isEmpty()) return
        for (sms in messages) {
            val from = sms.originatingAddress ?: ""
            val body = sms.messageBody ?: ""
            Log.d("AutoConfirm", "SMS recu de $from: $body")
            ApiClient.sendSms(serverUrl, token, from, body) { success, _ ->
                Log.d("AutoConfirm", "SMS envoye au serveur: $success")
            }
        }
    }
}
