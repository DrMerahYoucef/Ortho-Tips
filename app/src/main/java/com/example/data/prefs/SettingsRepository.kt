package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "ortho_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class FontSizePreference {
    STANDARD, LARGE, EXTRA_LARGE
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val operativeMode: Boolean = false, // Keep screen awake & high visibility for operating room
    val highContrast: Boolean = false,
    val fontSize: FontSizePreference = FontSizePreference.STANDARD,
    val quickPearlsHome: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OPERATIVE_MODE = booleanPreferencesKey("operative_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val QUICK_PEARLS = booleanPreferencesKey("quick_pearls")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val fontSizeStr = preferences[PreferencesKeys.FONT_SIZE] ?: FontSizePreference.STANDARD.name
        val fontSize = try {
            FontSizePreference.valueOf(fontSizeStr)
        } catch (_: Exception) {
            FontSizePreference.STANDARD
        }

        UserSettings(
            themeMode = themeMode,
            operativeMode = preferences[PreferencesKeys.OPERATIVE_MODE] ?: false,
            highContrast = preferences[PreferencesKeys.HIGH_CONTRAST] ?: false,
            fontSize = fontSize,
            quickPearlsHome = preferences[PreferencesKeys.QUICK_PEARLS] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setOperativeMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPERATIVE_MODE] = enabled
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST] = enabled
        }
    }

    suspend fun setFontSize(fontSize: FontSizePreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = fontSize.name
        }
    }

    suspend fun setQuickPearls(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUICK_PEARLS] = enabled
        }
    }
}
