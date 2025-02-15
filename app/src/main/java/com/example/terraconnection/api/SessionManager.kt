package com.example.terraconnection

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_AUTH_TOKEN = "auth_token"


    fun saveToken(context: Context, token: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getRole(context: Context): String? {
        val token = getToken(context) ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            // Add padding if needed
            val padding = when (payload.length % 4) {
                0 -> ""
                1 -> "==="
                2 -> "=="
                3 -> "="
                else -> ""
            }
            val decodedBytes = Base64.decode(payload + padding, Base64.URL_SAFE)
            val decodedString = String(decodedBytes, StandardCharsets.UTF_8)
            val jsonPayload = JSONObject(decodedString)
            jsonPayload.optString("role")
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
