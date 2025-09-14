package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsState

/**
 * Receiver scope which is used by [PrefsScreen].
 */
interface PrefsScope {
    /**
     * Adds a single Pref
     *
     * @param content the content of the item
     */
    fun prefsItem(content: @Composable PrefsScope.() -> Unit)

}

internal class PrefsScopeImpl : PrefsScope {

    private var _prefsItems: MutableList<PrefsItem> = mutableListOf()
    val prefsItems: List<PrefsItem> get() = _prefsItems

    override fun prefsItem(content: @Composable PrefsScope.() -> Unit) {
        _prefsItems.add(
            PrefsItem(
                content = { @Composable { content() } }
            )
        )
    }

    fun getPrefsItem(index: Int): @Composable () -> Unit {
        val prefsItem = prefsItems[index]
        return prefsItem.content.invoke(this, index)
    }
}

internal class PrefsItem(
    val content: PrefsScope.(index: Int) -> @Composable () -> Unit
)

@Composable
fun depend(key: String): Boolean {
    val checked by rememberPreferenceAsState(booleanPreferencesKey(key), true)
    return checked
}