package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.utils.appPreferences

/**
 * Lift up Bottom Bar if enabled
 *
 * @see com.huanchengfly.tieba.post.utils.AppPreferencesUtils.KEY_LIFT_BOTTOM_BAR
 * */
@Composable
fun LiftUpSpacer() {
    if (LocalContext.current.appPreferences.liftUpBottomBar) {
        Spacer( modifier = Modifier.height(16.dp))
    }
}
