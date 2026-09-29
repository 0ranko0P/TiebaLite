package com.huanchengfly.tieba.post.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope

/**
 * Evaluates a boolean query against the current [UiMediaScope], wrapped in a [derivedStateOf].
 *
 * Use this function for queries that involve frequently changing values, such as
 * [UiMediaScope.windowWidth] or [UiMediaScope.windowHeight]. It ensures that compositions only
 * recompose when the boolean result of the [query] changes, not on every small change to the
 * underlying values (like a 1px size change).
 *
 * For queries on stable properties, you can use the simpler [mediaQuery] function.
 *
 * @sample androidx.compose.ui.samples.MediaQuerySample
 * @param query The condition to evaluate against the [UiMediaScope].
 * @return A [State] holding the boolean result of the query. The state will only update when the
 *   evaluated result of the query changes.
 */
@ExperimentalMediaQueryApi
@Composable
fun <T> derivedMediaQueryState(query: UiMediaScope.() -> T): State<T> {
    val mediaScope = LocalUiMediaScope.current
    val currentQuery by rememberUpdatedState(query)

    return remember(mediaScope) { derivedStateOf { mediaScope.currentQuery() } }
}
