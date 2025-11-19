package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase

@DslMarker
annotation class PrefsDsl

/**
 * Receiver scope which is used by [PrefsScreen].
 */
class PrefsScope<T>(
    columnScope: ColumnScope,
    val preferenceState: State<T>,
    val updatePreference: (transform: (old: T) -> T) -> Unit,
    private val dividerIndent: Dp = Dp.Hairline
): ColumnScope by columnScope {

    /**
     * Adds a single preference.
     *
     * @param content the content of the item
     */
    @PrefsDsl
    @Composable
    inline fun Item(drawDivider: Boolean = false, content: @Composable PrefsScope<T>.(T) -> Unit) {
        content(preferenceState.value)
        if (drawDivider) {
            Divider()
        }
    }

    /**
     * Adds a single text preference.
     *
     * @param content the content of the item
     */
    @PrefsDsl
    @Composable
    inline fun TextItem(drawDivider: Boolean = false, content: @Composable PrefsScope<T>.() -> Unit) {
        content()
        if (drawDivider) {
            Divider()
        }
    }

    @PrefsDsl
    @Composable
    fun Divider() {
        HorizontalDivider(
            modifier = Modifier
                .onCase(dividerIndent != Dp.Hairline) { padding(horizontal = dividerIndent) },
        )
    }
}