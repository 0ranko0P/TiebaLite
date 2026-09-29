@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package com.huanchengfly.tieba.post.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val Icons.Rounded.PersonHeart: ImageVector
    get() {
        if (_PersonHeart != null) {
            return _PersonHeart!!
        }
        _PersonHeart = ImageVector.Builder(
            name = "Icons.PersonHeart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .apply {
            materialPath {
                moveTo(12f, 18f)
                close()
                moveTo(4f, 20f)
                verticalLineTo(17.2f)
                quadTo(4f, 16.35f, 4.44f, 15.64f)
                quadTo(4.88f, 14.93f, 5.6f, 14.55f)
                quadTo(7.15f, 13.77f, 8.75f, 13.39f)
                reflectiveQuadTo(12f, 13f)
                quadToRelative(0.1f, 0f, 0.18f, 0f)
                reflectiveQuadToRelative(0.18f, 0f)
                quadToRelative(-0.28f, 0.45f, -0.41f, 0.96f)
                quadTo(11.8f, 14.48f, 11.8f, 15f)
                quadToRelative(-1.35f, 0.03f, -2.69f, 0.36f)
                reflectiveQuadTo(6.5f, 16.35f)
                quadTo(6.28f, 16.48f, 6.14f, 16.7f)
                quadTo(6f, 16.93f, 6f, 17.2f)
                verticalLineTo(18f)
                horizontalLineToRelative(7.08f)
                lineToRelative(2f, 2f)
                horizontalLineTo(4f)
                close()
                moveTo(9.18f, 10.83f)
                quadTo(8f, 9.65f, 8f, 8f)
                reflectiveQuadTo(9.18f, 5.18f)
                reflectiveQuadTo(12f, 4f)
                reflectiveQuadToRelative(2.83f, 1.18f)
                reflectiveQuadTo(16f, 8f)
                reflectiveQuadToRelative(-1.17f, 2.82f)
                reflectiveQuadTo(12f, 12f)
                reflectiveQuadTo(9.18f, 10.83f)
                close()
                moveTo(13.41f, 9.41f)
                quadTo(14f, 8.82f, 14f, 8f)
                reflectiveQuadTo(13.41f, 6.59f)
                reflectiveQuadTo(12f, 6f)
                reflectiveQuadTo(10.59f, 6.59f)
                quadTo(10f, 7.18f, 10f, 8f)
                reflectiveQuadToRelative(0.59f, 1.41f)
                reflectiveQuadTo(12f, 10f)
                reflectiveQuadTo(13.41f, 9.41f)
                close()
                moveTo(12f, 8f)
                close()
                moveToRelative(5.9f, 12f)
                lineTo(14.4f, 16.5f)
                quadTo(14.08f, 16.18f, 13.94f, 15.8f)
                quadTo(13.8f, 15.43f, 13.8f, 15.05f)
                quadToRelative(0f, -0.8f, 0.57f, -1.42f)
                reflectiveQuadTo(15.85f, 13f)
                quadToRelative(0.7f, 0f, 1.1f, 0.32f)
                reflectiveQuadTo(17.9f, 14.2f)
                quadToRelative(0.5f, -0.5f, 0.91f, -0.85f)
                quadTo(19.23f, 13f, 19.95f, 13f)
                quadToRelative(0.92f, 0f, 1.49f, 0.64f)
                reflectiveQuadTo(22f, 15.08f)
                quadToRelative(0f, 0.38f, -0.15f, 0.75f)
                reflectiveQuadTo(21.4f, 16.5f)
                lineTo(17.9f, 20f)
                close()
            }
        }.build()

        return _PersonHeart!!
    }

private var _PersonHeart: ImageVector? = null
