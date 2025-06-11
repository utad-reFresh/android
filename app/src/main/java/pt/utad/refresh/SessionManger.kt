package pt.utad.refresh

import android.content.Context

class SessionManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}