@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package com.huanchengfly.tieba.post.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val Icons.Rounded.Autoplay: ImageVector
    get() {
        if (_autoplay != null) {
            return _autoplay!!
        }
        _autoplay =
            ImageVector.Builder(
                name = "autoplay",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                materialPath {
                    moveTo(9.5f, 16.5f)
                    verticalLineToRelative(-9f)
                    lineToRelative(7f, 4.5f)
                    lineToRelative(-7f, 4.5f)
                    close()
                    moveTo(12f, 23f)
                    quadTo(9.3f, 23f, 6.94f, 21.76f)
                    quadTo(4.58f, 20.53f, 3f, 18.3f)
                    verticalLineTo(21f)
                    horizontalLineTo(1f)
                    verticalLineTo(15f)
                    horizontalLineTo(7f)
                    verticalLineToRelative(2f)
                    horizontalLineTo(4.55f)
                    quadToRelative(1.28f, 1.88f, 3.24f, 2.94f)
                    reflectiveQuadTo(12f, 21f)
                    quadToRelative(2.88f, 0f, 5.21f, -1.65f)
                    reflectiveQuadTo(20.5f, 14.98f)
                    lineToRelative(1.95f, 0.45f)
                    quadToRelative(-1.13f, 3.4f, -4f, 5.49f)
                    quadTo(15.58f, 23f, 12f, 23f)
                    close()
                    moveTo(1.05f, 11f)
                    quadTo(1.23f, 9.32f, 1.85f, 7.79f)
                    reflectiveQuadTo(3.58f, 4.95f)
                    lineTo(5f, 6.38f)
                    quadTo(4.2f, 7.4f, 3.7f, 8.56f)
                    reflectiveQuadTo(3.08f, 11f)
                    horizontalLineTo(1.05f)
                    close()
                    moveTo(6.4f, 4.97f)
                    lineTo(4.98f, 3.55f)
                    quadTo(6.3f, 2.45f, 7.83f, 1.81f)
                    quadTo(9.35f, 1.17f, 11f, 1.05f)
                    verticalLineToRelative(2f)
                    quadTo(9.73f, 3.17f, 8.58f, 3.67f)
                    reflectiveQuadTo(6.4f, 4.97f)
                    close()
                    moveToRelative(11.23f, 0f)
                    quadTo(16.6f, 4.17f, 15.44f, 3.67f)
                    quadTo(14.28f, 3.17f, 13f, 3.05f)
                    verticalLineToRelative(-2f)
                    quadToRelative(1.68f, 0.15f, 3.21f, 0.78f)
                    reflectiveQuadToRelative(2.84f, 1.72f)
                    lineTo(17.63f, 4.97f)
                    close()
                    moveTo(20.95f, 11f)
                    quadTo(20.83f, 9.73f, 20.33f, 8.56f)
                    reflectiveQuadTo(19.03f, 6.38f)
                    lineTo(20.45f, 4.95f)
                    quadToRelative(1.1f, 1.3f, 1.72f, 2.84f)
                    reflectiveQuadTo(22.95f, 11f)
                    horizontalLineToRelative(-2f)
                    close()
                }
            }.build()
        return _autoplay!!
    }

private var _autoplay: ImageVector? = null
