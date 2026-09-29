package com.huanchengfly.tieba.post.core.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

fun<T> DataStore<Preferences>.blockSet(key: Preferences.Key<T>, value: T?) {
    runBlocking {
        try {
            edit {
                if (value == null) it.remove(key) else it[key] = value
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error while persisting key [$key]", e)
        }
    }
}

fun <T> DataStore<Preferences>.blockGet(key: Preferences.Key<T>, defaultValue: T): T {
    return runBlocking {
        try {
            data.map { it[key] ?: defaultValue }.first()
        } catch (e: Exception) {
            throw IllegalStateException("Error while reading key [$key]", e)
        }
    }
}

fun DataStore<Preferences>.getInt(key: String, defaultValue: Int): Int {
    return blockGet(intPreferencesKey(key), defaultValue)
}

fun DataStore<Preferences>.getString(key: String, defaultValue: String): String {
    return blockGet(stringPreferencesKey(key), defaultValue)
}