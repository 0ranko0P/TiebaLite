package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R

internal val DefaultTextStyle = TextStyle.Default.copy()

val BebasFamily = FontFamily(
    Font(R.font.bebas, weight = FontWeight.Normal, loadingStrategy = FontLoadingStrategy.Async)
)

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = DefaultTextStyle.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    subtitle1 = DefaultTextStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    subtitle2 = DefaultTextStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.15.sp
    ),
    button = DefaultTextStyle.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    )
)