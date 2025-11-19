@file:Suppress("unused")

package com.huanchengfly.tieba.post

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun Flow<Preferences>.distinctUntilChangedByKeys(vararg keys: Preferences.Key<*>): Flow<Preferences> {
    if (keys.size == 1) return this.distinctUntilChangedBy { it[keys.first()] }

    return this.distinctUntilChangedBy { data ->
        val values = arrayOfNulls<Any?>(keys.size)
        keys.forEachIndexed { i, key -> values[i] = data[key] ?: key }
        values.contentHashCode()
    }
}

fun<T> DataStore<Preferences>.asyncEdit(key: Preferences.Key<T>, value: T?) {
    AppBackgroundScope.launch {
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

fun DataStore<Preferences>.putString(key: String, value: String?) {
    asyncEdit(stringPreferencesKey(key), value)
}

fun DataStore<Preferences>.putBoolean(key: String, value: Boolean?) {
    asyncEdit(booleanPreferencesKey(key), value)
}

fun DataStore<Preferences>.putFloat(key: String, value: Float) {
    asyncEdit(floatPreferencesKey(key), value)
}

fun DataStore<Preferences>.putInt(key: String, value: Int) {
    asyncEdit(intPreferencesKey(key), value)
}

fun DataStore<Preferences>.putLong(key: String, value: Long) {
    asyncEdit(longPreferencesKey(key), value)
}

fun DataStore<Preferences>.getInt(key: String, defaultValue: Int): Int {
    return blockGet(intPreferencesKey(key), defaultValue)
}

fun DataStore<Preferences>.getString(key: String, defaultValue: String): String {
    return blockGet(stringPreferencesKey(key), defaultValue)
}

fun DataStore<Preferences>.getBoolean(key: String, defaultValue: Boolean): Boolean {
    return blockGet(booleanPreferencesKey(key), defaultValue)
}

fun DataStore<Preferences>.getFloat(key: String, defaultValue: Float): Float {
    return blockGet(floatPreferencesKey(key), defaultValue)
}

fun DataStore<Preferences>.getLong(key: String, defaultValue: Long): Long {
    return blockGet(longPreferencesKey(key), defaultValue)
}

fun Preferences.getColor(key: String): Color? = this[intPreferencesKey(key)]?.let { Color(it) }

/**
 * Same with [MutablePreferences.setUnchecked]
 * */
private fun<T> MutablePreferences.putNullable(key: Preferences.Key<T>, value: T?) {
    if (value != null) {
        this[key] = value
    } else {
        this.remove(key)
    }
}

fun MutablePreferences.putBoolean(key: String, value: Boolean?) = putNullable(booleanPreferencesKey(key), value)

fun MutablePreferences.putInt(key: String, value: Int?) = putNullable(intPreferencesKey(key), value)

fun MutablePreferences.putLong(key: String, value: Long?) = putNullable(longPreferencesKey(key), value)

fun MutablePreferences.putString(key: String, value: String?) = putNullable(stringPreferencesKey(key), value)

fun MutablePreferences.putColor(key: String, color: Color?) = putInt(key, color?.toArgb())
