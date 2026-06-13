package com.autoconfirm.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun sendSms(serverUrl: String, token: String, from: String, body: String, callback: (Boolean, String) -> Unit) {
        val json = JSONObject().apply { put("body", body); put("from", from); put("token", token) }
        post("$serverUrl/sms/receive", token, json.toString(), callback)
    }

    fun sendOrder(serverUrl: String, token: String, body: String, callback: (Boolean, String) -> Unit) {
        val json = JSONObject().apply { put("body", body); put("token", token) }
        post("$serverUrl/order/receive", token, json.toString(), callback)
    }

    fun send2FA(serverUrl: String, token: String, body: String, callback: (Boolean, String) -> Unit) {
        val json = JSONObject().apply { put("body", body); put("token", token) }
        post("$serverUrl/bot/2fa-auto", token, json.toString(), callback)
    }

    private fun post(url: String, token: String, jsonBody: String, callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-token", token)
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody(JSON))
                    .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { callback(false, e.message ?: "Erreur reseau") }
                    override fun onResponse(call: Call, response: Response) {
                        callback(response.isSuccessful, response.body?.string() ?: "")
                        response.close()
                    }
                })
            } catch (e: Exception) { callback(false, e.message ?: "Erreur") }
        }
    }
}
