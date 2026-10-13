/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanchengfly.tieba.post.core.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.huanchengfly.tieba.post.core.common.ktx.fastMap

/**
 * Create a navigation state that persists config changes and process death.
 */
@Composable
fun rememberNavigationState(
    startKey: NavKey,
    topLevelKeys: List<NavKey>,
): NavigationState {
    val topLevelStack = rememberNavBackStack(startKey)
    val subStacks = rememberSubNavBackStack(topLevelKeys)

    return remember(startKey, subStacks) {
        NavigationState(
            startKey = startKey,
            topLevelStack = topLevelStack,
            subStacks = subStacks,
        )
    }
}

/**
 * State holder for navigation state.
 *
 * @param startKey - the starting navigation key. The user will exit the app through this key.
 * @param topLevelStack - the top level back stack. It holds only top level keys.
 * @param subStacks - the back stacks for each top level key
 */
class NavigationState(
    val startKey: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: SubNavBackStack<NavKey>,
) {
    val currentTopLevelKey: NavKey by derivedStateOf { topLevelStack.last() }

    val topLevelKeys
        get() = subStacks.keys

    @get:VisibleForTesting
    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks[currentTopLevelKey]
            ?: error("Sub stack for $currentTopLevelKey does not exist")

    val currentKey: NavKey by derivedStateOf { currentSubStack.last() }
}

/**
 * Remembers and returns a list of [NavEntry] decorated with the list of [entryDecorators]
 *
 * The returned list of [NavEntry] survives configuration change and recompositions but does not
 * survive process death. To ensure that the backStack and its states are recovered properly, the
 * [backStack] and the [entryDecorators]'s states passed into this function should be hoisted and
 * saved across process death.
 *
 * For example:
 * ```
 * // backStack saved during process death
 * val backStack = rememberNavBackStack()
 *
 * // the hoisted SaveableStateHolder used by the decorator is saved during process death
 * val decorators = listOf(rememberSavedStateNavEntryDecorator())
 *
 * // finally, decorate the entries
 * val entries = rememberDecoratedNavEntries(backStack, decorators, entryProvider)
 * ```
 *
 * **HOW TO USE** The returned list of entries should be stored and reused for the same backStack.
 * If you want to support multiple backStacks (i.e. each bottom tab with their own backStack), then
 * each backStack should be their own [rememberDecoratedNavEntries] with new [backStack] and
 * [entryDecorators] passed in.
 *
 * **MUTABLE BACKSTACK NAVIGATE/POP** The [backStack] should be a hoisted OBSERVABLE list, and
 * navigates or pops should be applied directly to this backStack. For example:
 * ```
 * val backStack = mutableStateListOf(A)
 * val entries = rememberDecoratedNavEntries(backStack, ...)
 *
 * // to navigate
 * backStack.add(B)
 *
 * // to pop
 * backStack.removeLastOrNull()
 * ```
 *
 * **IMMUTABLE BACKSTACK NAVIGATE/POP** This composable also supports navigates and pops with
 * immutable backStack. Simply replace the previous [backStack] with a new one but DO NOT replace
 * the decorator states. Also, DO NOT create a brand new [rememberDecoratedNavEntries]. For example
 *
 * ```
 *  val backStack = mutableStateOf(listOf(1, 2))
 *  val entries = rememberDecoratedNavEntries(backStack, ...)
 *
 *  // to navigate
 *  backStack.value = listOf(1, 2, 3)
 *
 *  // to pop
 *  backStack.value = listOf(1)
 * ```
 *
 * **MULTIPLE BACKSTACKS** Each call to [rememberDecoratedNavEntries] represents a single backStack.
 * To support multiple backStack, there should be one [rememberDecoratedNavEntries] for each
 * backStack. For example
 *
 * ```
 * val homeBackStack = mutableStateListOf(HomeKey)
 * val homeDecorators = mutableStateListOf(rememberSavedStateNavEntryDecorator(), ...)
 * val homeTabEntries = rememberDecoratedNavEntries(homeBackStack, homeDecorators, ...)
 *
 * val favoritesBackStack = mutableStateListOf(FavoritesKey)
 * val favoritesDecorators = mutableStateListOf(rememberSavedStateNavEntryDecorator(),...)
 * val favoritesTabEntries = rememberDecoratedNavEntries(favoritesBackStack, favoritesDecorators, ...)
 * ```
 *
 * You can also concatenate multiple backStacks to form a larger one. So, given the above setup:
 * ```
 * val concatenatedEntries = homeTabEntries + favoritesTabEntries
 *
 * // To navigate within the favorites backStack
 * favoritesBackStack.add(FavoritesDetailKey)
 * ```
 *
 * In this case, the updated favoritesBackStack and updated states will be reflected in
 * concatenatedEntries.
 *
 * @param T the type of the backStack key
 * @param backStack the list of keys that represent the backstack. If this backStack is observable,
 *   i.e. a [androidx.compose.runtime.snapshots.SnapshotStateList], then updates to this backStack
 *   will automatically trigger a re-calculation of the list of [NavEntry] to reflect the new
 *   backStack state.
 * @param entryDecorators the [NavEntryDecorator]s that are providing data to the content. If this
 *   list is observable (i.e. a [androidx.compose.runtime.snapshots.SnapshotStateList]), then
 *   updates to this list of decorators will automatically trigger a re-calculation of the list of
 *   [NavEntry] to reflect the new decorators state.
 * @param entryProvider a function that returns the [NavEntry] for a given key
 * @return a list of decorated [NavEntry]
 */
@Composable
fun <T : Any> rememberDecoratedNavEntries(
    backStack: List<T>,
    entryDecorators: List<@JvmSuppressWildcards NavEntryDecorator<T>> = listOf(),
    entryProvider: (key: T) -> NavEntry<T>,
): List<NavEntry<T>> {
    @Suppress("ListIterator")
    val entries = remember(backStack.toList(), entryProvider) {
        backStack.fastMap(entryProvider)
    }

    return androidx.navigation3.runtime.rememberDecoratedNavEntries(entries, entryDecorators)
}

/**
 * Convert NavigationState into NavEntries.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = subStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return topLevelStack
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}