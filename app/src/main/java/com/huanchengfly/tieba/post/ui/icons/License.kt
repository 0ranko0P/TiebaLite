@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package com.huanchengfly.tieba.post.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val Icons.Rounded.License: ImageVector
    get() {
        if (_License != null) {
            return _License!!
        }
        _License = ImageVector.Builder(
            name = "Rounded.License",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            materialPath {
                moveTo(395f, 485f)
                quadToRelative(-35f, -35f, -35f, -85f)
                reflectiveQuadToRelative(35f, -85f)
                quadToRelative(35f, -35f, 85f, -35f)
                reflectiveQuadToRelative(85f, 35f)
                quadToRelative(35f, 35f, 35f, 85f)
                reflectiveQuadToRelative(-35f, 85f)
                quadToRelative(-35f, 35f, -85f, 35f)
                reflectiveQuadToRelative(-85f, -35f)
                close()
                moveTo(240f, 920f)
                verticalLineToRelative(-309f)
                quadToRelative(-38f, -42f, -59f, -96f)
                reflectiveQuadToRelative(-21f, -115f)
                quadToRelative(0f, -134f, 93f, -227f)
                reflectiveQuadToRelative(227f, -93f)
                quadToRelative(134f, 0f, 227f, 93f)
                reflectiveQuadToRelative(93f, 227f)
                quadToRelative(0f, 61f, -21f, 115f)
                reflectiveQuadToRelative(-59f, 96f)
                verticalLineToRelative(309f)
                lineToRelative(-240f, -80f)
                lineToRelative(-240f, 80f)
                close()
                moveTo(650f, 570f)
                quadToRelative(70f, -70f, 70f, -170f)
                reflectiveQuadToRelative(-70f, -170f)
                quadToRelative(-70f, -70f, -170f, -70f)
                reflectiveQuadToRelative(-170f, 70f)
                quadToRelative(-70f, 70f, -70f, 170f)
                reflectiveQuadToRelative(70f, 170f)
                quadToRelative(70f, 70f, 170f, 70f)
                reflectiveQuadToRelative(170f, -70f)
                close()
                moveTo(320f, 801f)
                lineToRelative(160f, -41f)
                lineToRelative(160f, 41f)
                verticalLineToRelative(-124f)
                quadToRelative(-35f, 20f, -75.5f, 31.5f)
                reflectiveQuadTo(480f, 720f)
                quadToRelative(-44f, 0f, -84.5f, -11.5f)
                reflectiveQuadTo(320f, 677f)
                verticalLineToRelative(124f)
                close()
                moveTo(480f, 739f)
                close()
            }
        }.build()

        return _License!!
    }

private var _License: ImageVector? = null
