package com.mapchina.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MapChinaTypography {
    val Display = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
    val Headline = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
    val Title = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val Body = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val Caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
    val Overline = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.5.sp)
}
