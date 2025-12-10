package com.huanchengfly.tieba.post.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.arch.unsafeLazy

val Icons.Rounded.CommentNew: ImageVector by unsafeLazy {
    ImageVector.Builder(
        name = "Icons.CommentNew",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 16f,
        viewportHeight = 16f
    ).apply {
        group(
            translationY = -0.5f,
            clipPathData = PathData {
                moveTo(0f, 0f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(16f)
                horizontalLineToRelative(-16f)
                close()
            }
        ) {
            materialPath {
                moveTo(3.488f, 13.431f)
                lineTo(3.488f, 8.416f)
                arcTo(4.984f, 4.984f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8.503f, 3.401f)
                horizontalLineToRelative(0f)
                arcToRelative(4.984f, 4.984f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.015f, 5.015f)
                horizontalLineToRelative(0f)
                arcToRelative(4.984f, 4.984f, 0f, isMoreThanHalf = false, isPositiveArc = true, -5.015f, 5.015f)
                lineTo(3.488f, 13.431f)
                moveToRelative(0f, 1.433f)
                lineTo(8.503f, 14.864f)
                arcTo(6.421f, 6.421f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14.955f, 8.416f)
                horizontalLineToRelative(0f)
                arcTo(6.467f, 6.467f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8.503f, 1.968f)
                horizontalLineToRelative(0f)
                arcTo(6.467f, 6.467f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2.055f, 8.416f)
                verticalLineToRelative(5.015f)
                arcTo(1.437f, 1.437f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.488f, 14.868f)
                close()
                moveTo(9.369f, 10.565f)
                lineTo(5.805f, 10.565f)
                arcTo(0.635f, 0.635f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.092f, 9.944f)
                lineTo(5.092f, 9.944f)
                arcToRelative(0.635f, 0.635f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.713f, -0.622f)
                lineTo(9.369f, 9.322f)
                arcToRelative(0.635f, 0.635f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.713f, 0.622f)
                horizontalLineToRelative(0f)
                arcTo(0.635f, 0.635f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9.369f, 10.565f)
                close()
                moveTo(11.025f, 7.51f)
                lineTo(5.751f, 7.51f)
                arcTo(0.606f, 0.606f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.092f, 6.889f)
                lineTo(5.092f, 6.889f)
                arcTo(0.606f, 0.606f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.751f, 6.267f)
                horizontalLineToRelative(5.274f)
                arcToRelative(0.606f, 0.606f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.659f, 0.622f)
                horizontalLineToRelative(0f)
                arcTo(0.606f, 0.606f, 0f, isMoreThanHalf = false, isPositiveArc = true, 11.025f, 7.51f)
                close()
            }
        }
    }.build()
}
