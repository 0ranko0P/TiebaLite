package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings

/**
 * Lift up Bottom Bar if enabled
 *
 * @see UISettings.liftBottomBar
 * */
@Composable
fun LiftUpSpacer() {
    if (LocalUISettings.current.liftBottomBar) {
        Spacer(modifier = Modifier.height(16.dp))
    }
}
