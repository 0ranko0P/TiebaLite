package com.huanchengfly.tieba.post.core.ui.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

@ExperimentalMaterial3AdaptiveApi
@ReadOnlyComposable
@Composable
fun isListDetail(): Boolean = LocalListDetailSceneScope.current != null