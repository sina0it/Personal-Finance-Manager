package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sina_finance_prefs", Context.MODE_PRIVATE)

    private val _activeProfileId = MutableStateFlow(prefs.getString("active_profile_id", "IRAN_TOMAN") ?: "IRAN_TOMAN")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "DARK") ?: "DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isAppLocked = MutableStateFlow(prefs.getBoolean("is_app_locked", false))
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun setActiveProfileId(profileId: String) {
        prefs.edit().putString("active_profile_id", profileId).apply()
        _activeProfileId.value = profileId
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setAppLock(enabled: Boolean, pinHash: String? = null) {
        prefs.edit()
            .putBoolean("is_app_locked", enabled)
            .putString("pin_hash", pinHash)
            .apply()
        _isAppLocked.value = enabled
    }

    fun getPinHash(): String? = prefs.getString("pin_hash", null)

    fun clearAll() {
        prefs.edit().clear().apply()
        _activeProfileId.value = "IRAN_TOMAN"
        _hasCompletedOnboarding.value = false
        _isAppLocked.value = false
    }
}
