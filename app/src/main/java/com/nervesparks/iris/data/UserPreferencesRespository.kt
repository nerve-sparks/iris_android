package com.nervesparks.iris.data

import android.content.Context

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"

class UserPreferencesRepository private constructor(context: Context) {

    private val sharedPreferences =
        context.applicationContext.getSharedPreferences(USER_PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Get the default model name, returns empty string if not set
    fun getDefaultModelName(): String {
        return sharedPreferences.getString(KEY_DEFAULT_MODEL_NAME, "") ?: ""
    }

    // Set the default model name
    fun setDefaultModelName(modelName: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_MODEL_NAME, modelName).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }
}