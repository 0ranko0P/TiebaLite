package com.huanchengfly.tieba.post.core.ui.util

import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

@ReadOnlyComposable
@Composable
fun isListDetail(): Boolean = LocalListDetailSceneScope.current != null