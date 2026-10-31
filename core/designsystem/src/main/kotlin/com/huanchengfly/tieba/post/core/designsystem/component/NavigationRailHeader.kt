package com.huanchengfly.tieba.post.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.WideNavigationRailState
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.core.designsystem.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 抽取自 [androidx.compose.material3.samples.WideNavigationRailArrangementsSample], 注意保持更新.
 *
 * commit a2bc0e4 'Fix some states in catalog to survive config changes' on branch androidx-main
 */
@Composable
fun NavigationRailHeader(
    modifier: Modifier = Modifier,
    state: WideNavigationRailState = rememberWideNavigationRailState(),
    scope: CoroutineScope = rememberCoroutineScope(),
) {
    val context = LocalContext.current
    val headerDescription =
        if (state.targetValue == WideNavigationRailValue.Expanded) {
            stringResource(R.string.core_design_btn_collapse_rail)
        } else {
            stringResource(R.string.core_design_btn_expand_rail)
        }

    // Header icon button should have a tooltip.
    @OptIn(ExperimentalMaterial3Api::class)
    TooltipBox(
        modifier = modifier,
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above
            ),
        tooltip = {
            PlainTooltip(
                Modifier.semantics {
                    // TODO(b/496338253): Remove this modifier once bug where tooltip
                    //  text is not announced by a11y screen readers is resolved.
                    liveRegion = LiveRegionMode.Assertive
                    paneTitle = headerDescription
                }
            ) {
                Text(headerDescription)
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(
            modifier =
                Modifier.padding(start = 24.dp).semantics {
                    // The button must announce the expanded or collapsed state of the
                    // rail for accessibility.
                    stateDescription =
                        if (state.currentValue == WideNavigationRailValue.Expanded) {
                            context.getString(R.string.core_design_expanded)
                        } else {
                            context.getString(R.string.core_design_collapsed)
                        }
                },
            onClick = {
                scope.launch {
                    if (state.targetValue == WideNavigationRailValue.Expanded) {
                        state.collapse()
                    } else {
                        state.expand()
                    }
                }
            },
        ) {
            if (state.targetValue == WideNavigationRailValue.Expanded) {
                Icon(Icons.AutoMirrored.Filled.MenuOpen, headerDescription)
            } else {
                Icon(Icons.Filled.Menu, headerDescription)
            }
        }
    }
}