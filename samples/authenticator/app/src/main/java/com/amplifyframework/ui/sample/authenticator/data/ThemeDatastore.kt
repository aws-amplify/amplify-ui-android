package com.amplifyframework.ui.sample.authenticator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amplifyframework.ui.sample.authenticator.SupportedTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeDatastore(context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme")

    private val datastore = context.dataStore

    private val darkModeKey = booleanPreferencesKey("darkMode")
    private val themeKey = stringPreferencesKey("theme")

    suspend fun saveTheme(theme: SupportedTheme) {
        datastore.edit { settings ->
            settings[themeKey] = theme.name
        }
    }

    suspend fun saveDarkMode(darkMode: Boolean) {
        datastore.edit { settings ->
            settings[darkModeKey] = darkMode
        }
    }

    val darkMode: Flow<Boolean> = datastore.data.map { settings ->
        settings[darkModeKey] ?: false
    }
    val theme: Flow<SupportedTheme> = datastore.data.map { settings ->
        val name = settings[themeKey] ?: SupportedTheme.Default.name
        SupportedTheme.valueOf(name)
    }

}