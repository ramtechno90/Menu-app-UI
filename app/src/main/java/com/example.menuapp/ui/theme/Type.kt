package com.example.menuapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.menuapp.R

val BeVietnamPro = FontFamily(
    Font(R.font.bevietnam_regular, FontWeight.Normal),
    Font(R.font.bevietnam_bold, FontWeight.Bold),
    Font(R.font.bevietnam_extrabold, FontWeight.ExtraBold),
    Font(R.font.bevietnam_light, FontWeight.Light),
    Font(R.font.bevietnam_medium, FontWeight.Medium),
    Font(R.font.bevietnam_semibold, FontWeight.SemiBold),
    Font(R.font.bevietnam_thin, FontWeight.Thin)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
)
