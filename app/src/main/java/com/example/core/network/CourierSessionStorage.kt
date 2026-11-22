package com.example.core.network

import android.content.Context

data class SavedCourierSession(
    val rememberMe: Boolean,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val courierId: String
)

object CourierSessionStorage {
    private const val PREFS_NAME = "kube_courier_auth_prefs"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_EMAIL = "saved_email"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_COURIER_ID = "courier_id"

    fun saveSession(
        context: Context,
        rememberMe: Boolean,
        email: String,
        accessToken: String,
        refreshToken: String,
        userId: String,
        courierId: String
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (rememberMe) {
            prefs.edit()
                .putBoolean(KEY_REMEMBER_ME, true)
                .putString(KEY_EMAIL, email.trim())
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_COURIER_ID, courierId)
                .apply()
        } else {
            clearSession(context)
        }
    }

    fun getSavedSession(context: Context): SavedCourierSession? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRemembered = prefs.getBoolean(KEY_REMEMBER_ME, false)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        if (!isRemembered || refreshToken.isBlank()) return null

        return SavedCourierSession(
            rememberMe = true,
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty(),
            refreshToken = refreshToken,
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            courierId = prefs.getString(KEY_COURIER_ID, "").orEmpty()
        )
    }

    fun clearSession(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
