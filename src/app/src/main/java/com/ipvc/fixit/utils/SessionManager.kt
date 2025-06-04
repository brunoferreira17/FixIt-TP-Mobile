package com.ipvc.fixit.utils

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "FixItPrefs"
    private const val KEY_LOGGED_USER_ID = "LOGGED_USER_ID"

    fun saveUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOGGED_USER_ID, userId).apply()
    }

    fun getLoggedUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGGED_USER_ID, null)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
