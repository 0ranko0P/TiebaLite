package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import kotlinx.coroutines.Dispatchers

/**
 * Main preference screen
 */
@Composable
fun <T> PrefsScreen(
    modifier: Modifier = Modifier,
    settings: Settings<T>,
    initialValue: T,
    dividerIndent: Dp = Dp.Hairline, // indents on both sides
    contentPadding: PaddingValues = PaddingNone,
    content: @Composable PrefsScope<T>.() -> Unit
) {
    Container {
        val preferenceState = settings.collectAsStateWithLifecycle(
            initialValue = initialValue,
            minActiveState = Lifecycle.State.CREATED,
            context = Dispatchers.IO
        )
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            val prefsScope = remember(dividerIndent) {
                PrefsScope(this, preferenceState, settings::save, dividerIndent)
            }
            prefsScope.content()
        }
    }
}

/**
 * Preference screen without savable settings, basically a [Column].
 */
@Composable
fun TextPrefsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingNone,
    content: @Composable ColumnScope.() -> Unit
) {
    Container {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}
