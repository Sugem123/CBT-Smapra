package com.smapra.exambro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exam_preferences")

@Singleton
class ExamPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_LAST_URL = stringPreferencesKey("last_url")
        private val KEY_SAVED_URLS = stringSetPreferencesKey("saved_urls")
        private val KEY_EXAM_CONFIG_KEY = stringPreferencesKey("exam_config_key")
        private val KEY_ALLOWED_DOMAINS = stringSetPreferencesKey("allowed_domains")
    }

    val lastUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_URL].orEmpty()
    }

    val savedUrls: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_URLS] ?: emptySet()
    }

    val examConfigKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXAM_CONFIG_KEY].orEmpty()
    }

    val allowedDomains: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_ALLOWED_DOMAINS] ?: emptySet()
    }

    suspend fun saveUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_URL] = url
            val current = prefs[KEY_SAVED_URLS] ?: emptySet()
            prefs[KEY_SAVED_URLS] = current + url
        }
    }

    suspend fun clearSavedUrls() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_URLS)
        }
    }

    suspend fun saveExamConfigKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXAM_CONFIG_KEY] = key
        }
    }

    suspend fun saveAllowedDomains(domains: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALLOWED_DOMAINS] = domains
        }
    }

    suspend fun clearExamData() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_EXAM_CONFIG_KEY)
            prefs.remove(KEY_ALLOWED_DOMAINS)
        }
    }
}
