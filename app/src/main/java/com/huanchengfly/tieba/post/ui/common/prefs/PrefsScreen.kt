package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.ui.widgets.compose.Container

/**
 * Main preference screen which holds
 *
 * @param modifier Modifier applied to the [LazyColumn] holding the list of Prefs
 */
@Composable
fun PrefsScreen(
    modifier: Modifier = Modifier,
    dividerThickness: Dp = Dp.Hairline, // Default no divider
    dividerIndent: Dp = Dp.Hairline, // indents on both sides
    contentPadding: PaddingValues = PaddingNone,
    content: PrefsScope.() -> Unit
) {
    val prefsScope = PrefsScopeImpl().apply(content)

    Container {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            items(prefsScope.prefsItems.size) { index ->
                prefsScope.getPrefsItem(index)()

                if (dividerThickness != Dp.Hairline) {
                    HorizontalDivider(
                        modifier = if (dividerIndent != Dp.Hairline) {
                            Modifier.padding(horizontal = dividerIndent)
                        } else {
                            Modifier
                        },
                        thickness = dividerThickness
                    )
                }
            }
        }
    }
}
