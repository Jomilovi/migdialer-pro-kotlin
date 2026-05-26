package com.migdialer.pro.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Type-safe wrapper over SharedPreferences.
 * Single source of truth for all user preferences.
 * No Room dependency needed — settings volume is tiny.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    // ── SIM preference ────────────────────────────────────────────────────────

    /** -1 = ask every time, 0 = SIM slot 1, 1 = SIM slot 2 */
    var defaultSimSlot: Int
        get() = prefs.getInt(KEY_DEFAULT_SIM, DEFAULT_SIM_ASK)
        set(v) = prefs.edit { putInt(KEY_DEFAULT_SIM, v) }

    // ── Haptics ───────────────────────────────────────────────────────────────

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(v) = prefs.edit { putBoolean(KEY_VIBRATION, v) }

    // ── Number format ─────────────────────────────────────────────────────────

    /** true = national format, false = international (+prefix) */
    var nationalFormat: Boolean
        get() = prefs.getBoolean(KEY_NATIONAL_FORMAT, true)
        set(v) = prefs.edit { putBoolean(KEY_NATIONAL_FORMAT, v) }

    // ── Accent colour ─────────────────────────────────────────────────────────

    /** Stored as ARGB int; default = white */
    var accentColorIndex: Int
        get() = prefs.getInt(KEY_ACCENT_INDEX, ACCENT_WHITE)
        set(v) = prefs.edit { putInt(KEY_ACCENT_INDEX, v) }

    // ── Onboarding ────────────────────────────────────────────────────────────

    var onboardingShown: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
        set(v) = prefs.edit { putBoolean(KEY_ONBOARDING_SHOWN, v) }

    companion object {
        private const val PREF_FILE            = "migdialer_prefs"
        private const val KEY_DEFAULT_SIM      = "default_sim_slot"
        private const val KEY_VIBRATION        = "vibration_enabled"
        private const val KEY_NATIONAL_FORMAT  = "national_format"
        private const val KEY_ACCENT_INDEX     = "accent_color_index"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

        const val DEFAULT_SIM_ASK = -1
        const val ACCENT_WHITE    = 0
        const val ACCENT_GREEN    = 1
        const val ACCENT_BLUE     = 2
    }
}
