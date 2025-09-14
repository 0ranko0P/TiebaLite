package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.huanchengfly.tieba.post.App

val ImeManager by lazy {
    App.INSTANCE.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

fun showKeyboard(view: View): Boolean {
    view.requestFocus()
    return ImeManager.showSoftInput(view, 0)
}

fun hideKeyboard(view: View): Boolean {
    return ImeManager.hideSoftInputFromWindow(view.windowToken, 0)
}

@Composable
fun keyboardAnimationHeight(): State<Dp> {
    val height = with(LocalDensity.current) {
        WindowInsets.ime.exclude(WindowInsets.navigationBars).getBottom(this).toDp()
    }
    return rememberUpdatedState(height)
}

/**
 * https://stackoverflow.com/questions/68847559/how-can-i-detect-keyboard-opening-and-closing-in-jetpack-compose
 *
 * @author ujizin
 *
 * @return whether IME visible or not
 * */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun keyboardMaxHeight(): State<Dp> {
    val state = remember { mutableStateOf(250.dp) }
    val height = with(LocalDensity.current) {
        WindowInsets.imeAnimationTarget.exclude(WindowInsets.navigationBars).getBottom(this).toDp()
    }
    return state.apply { value = max(state.value, height) }
}
