package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
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

    @PrefsDsl
    @Composable
    fun Group(title: @Composable () -> Unit, content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            ProvideContentColorTextStyle(
                contentColor = MaterialTheme.colorScheme.primary,
                textStyle = MaterialTheme.typography.titleMedium,
                content = title
            )
        }

        content()
    }

    @PrefsDsl
    @NonRestartableComposable
    @Composable
    fun Group(@StringRes titleRes: Int, content: @Composable () -> Unit) {
        Group(
            title = {
                Text(text = stringResource(id = titleRes), modifier = Modifier.padding(start = 20.dp))
            },
            content = content
        )
    }

    /**
     * Adds a single preference.
     *
     * @param content the content of the item
     */
    @PrefsDsl
    @Composable
    inline fun Item(drawDivider: Boolean = false, content: @Composable (T) -> Unit) {
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
    inline fun TextItem(drawDivider: Boolean = false, content: @Composable () -> Unit) {
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