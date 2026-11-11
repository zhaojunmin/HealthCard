package com.healthcard.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 健康打卡主界面主题（清爽浅色 · 翡翠绿，research-grounded）。
 *
 * 核心原则：**单一主强调色 + 中性面板分层**。层级靠 surface / surfaceVariant /
 * background 搭配 on-color 与字重建立；强调色（柔和翡翠绿）只保留给进度环与完成态。
 * 完成态用 primaryContainer(干净薄荷) + onPrimaryContainer(深绿)，不发浊、不刺眼。
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF10B981),            // 翡翠绿：进度环 / 完成态图标底 / CTA
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),   // 完成态卡片底：干净薄荷
    onPrimaryContainer = Color(0xFF065F46), // 完成态卡片内容：深翡翠
    tertiary = Color(0xFF2DD4BF),           // 进度环渐变的青色端
    background = Color(0xFFF6F8F7),         // 近白背景
    onBackground = Color(0xFF18211D),       // 主文字
    surface = Color(0xFFFFFFFF),            // 纯白面板
    onSurface = Color(0xFF18211D),
    surfaceVariant = Color(0xFFEAF0ED),     // 未完成卡片底 / 环轨道 / 导航按钮底
    onSurfaceVariant = Color(0xFF5B6660),   // 次级文字
    outline = Color(0xFFDBE3DE),            // 细边框
    error = Color(0xFFDC2626),
)

@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
