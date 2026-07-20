package com.crossmint.kotlin.auth

import android.content.Context
import android.content.SharedPreferences
import com.jakewharton.processphoenix.ProcessPhoenix

enum class AuthMode {
    CROSSMINT,
    BYOA,
}

object AuthModePreferences {
    private const val PREFS_NAME = "crossmint_auth_prefs"
    private const val KEY_AUTH_MODE = "auth_mode"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAuthMode(context: Context): AuthMode {
        val prefs = getPrefs(context)
        val modeName = prefs.getString(KEY_AUTH_MODE, AuthMode.CROSSMINT.name)
        return try {
            AuthMode.valueOf(modeName ?: AuthMode.CROSSMINT.name)
        } catch (e: IllegalArgumentException) {
            AuthMode.CROSSMINT
        }
    }

    fun setAuthMode(
        context: Context,
        mode: AuthMode,
    ) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_AUTH_MODE, mode.name).apply()
    }

    fun setAuthModeAndRestart(
        context: Context,
        mode: AuthMode,
    ) {
        setAuthMode(context, mode)
        ProcessPhoenix.triggerRebirth(context)
    }
}
