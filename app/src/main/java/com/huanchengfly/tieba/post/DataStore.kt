@file:Suppress("unused")

package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val DATA_STORE_NAME = "app_preferences"
val dataStoreScope = CoroutineScope(Dispatchers.IO + CoroutineName(DATA_STORE_NAME) + SupervisorJob())

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
    scope = dataStoreScope
)

@Composable
fun <T> rememberPreferenceAsMutableState(
    key: Preferences.Key<T>,
    defaultValue: T
): MutableState<T> {
    val dataStore = LocalContext.current.dataStore
    val state = remember { mutableStateOf(defaultValue) }

    LaunchedEffect(Unit) {
        dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
            .collect { state.value = it }
    }

    LaunchedEffect(key1 = state.value, key2 = defaultValue) {
        delay(200)
        val newValue: T = state.value
        val oldValue: T? = dataStore.data.first()[key]

        when {
            newValue == oldValue -> return@LaunchedEffect

            newValue == defaultValue && oldValue == null -> return@LaunchedEffect

            else -> dataStore.asyncEdit(key, newValue.takeUnless { it == defaultValue })
        }
    }

    return state
}

@SuppressLint("ProduceStateDoesNotAssignValue")
@Composable
fun <T> rememberPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    val dataStore = LocalContext.current.dataStore

    return produceState(defaultValue, Unit) {
        dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
            .collect {
                value = it
            }
    }
}

@Composable
fun <T> DataStore<Preferences>.collectPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    return data.map { it[key] ?: defaultValue }.collectAsState(initial = defaultValue)
}

fun Flow<Preferences>.distinctUntilChangedByKeys(vararg keys: Preferences.Key<Any>): Flow<Preferences> {
    if (keys.size == 1) return this.distinctUntilChangedBy { it[keys.first()] }

    return this.distinctUntilChangedBy { data ->
        val values = arrayOfNulls<Any?>(keys.size)
        keys.forEachIndexed { i, key -> values[i] = data[key] }
        values.contentHashCode()
    }
}

fun<T> DataStore<Preferences>.asyncEdit(key: Preferences.Key<T>, value: T?) {
    dataStoreScope.launch {
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

fun DataStore<Preferences>.putBoolean(key: String, value: Boolean) {
    asyncEdit(booleanPreferencesKey(key), value)
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

fun MutablePreferences.putColor(key: String, color: Color) {
    this[intPreferencesKey(key)] = color.toArgb()
}
