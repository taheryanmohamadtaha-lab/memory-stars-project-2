package com.memorystars.leitner.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

data class AppTheme(
    val id: String, val name: String, val emoji: String, val decor: String,
    val primary: Color, val secondary: Color, val lightBg: Color, val darkBg: Color,
    val minLevel: Int = 0
)

val Themes = listOf(
    AppTheme("rainbow", "رنگین‌کمان", "🌈", "🌈 ☁️ ⭐ ☁️ 🌈", Color(0xFF7E57C2), Color(0xFFFF7043), Color(0xFFFFF8E1), Color(0xFF1F1A33)),
    AppTheme("galaxy", "کهکشان و ستاره‌ها", "🌌", "✨ 🌟 🪐 🌟 ✨", Color(0xFF5E35B1), Color(0xFF29B6F6), Color(0xFFEDE7F6), Color(0xFF120E2B)),
    AppTheme("ocean", "اقیانوس آبی", "🌊", "🐠 🌊 🐳 🌊 🐙", Color(0xFF0277BD), Color(0xFF26C6DA), Color(0xFFE1F5FE), Color(0xFF0B1E2D)),
    AppTheme("candy", "آب‌نباتی و صورتی", "🍭", "🍭 🍬 🧁 🍬 🍭", Color(0xFFD81B60), Color(0xFFFFA726), Color(0xFFFCE4EC), Color(0xFF2B1220)),
    AppTheme("minimal", "ساده و مینیمال", "◻️", "· · · · ·", Color(0xFF455A64), Color(0xFF00897B), Color(0xFFF5F5F5), Color(0xFF181A1B)),
    AppTheme("forest", "جنگل سبز", "🌲", "🌳 🍃 🦉 🍃 🌳", Color(0xFF2E7D32), Color(0xFF8D6E63), Color(0xFFE8F5E9), Color(0xFF10210F), 1),
    AppTheme("dino", "دایناسورها", "🦖", "🦖 🌋 🦕 🌋 🦖", Color(0xFF558B2F), Color(0xFFEF6C00), Color(0xFFF1F8E9), Color(0xFF1A2110), 2),
    AppTheme("space", "فضای فضانوردی", "🚀", "🚀 🛰️ 👨‍🚀 🛰️ 🚀", Color(0xFF283593), Color(0xFFEF5350), Color(0xFFE8EAF6), Color(0xFF0A0F26), 3),
    AppTheme("animals", "حیوانات بامزه", "🐼", "🐼 🐯 🦊 🐯 🐼", Color(0xFFF57C00), Color(0xFF6D4C41), Color(0xFFFFF3E0), Color(0xFF2A1B0A), 4)
)

fun themeScheme(t: AppTheme, dark: Boolean): ColorScheme =
    if (dark) {
        val surface = lerp(t.darkBg, Color.White, 0.09f)
        darkColorScheme(
            primary = lerp(t.primary, Color.White, 0.45f), onPrimary = Color(0xFF14102B),
            secondary = lerp(t.secondary, Color.White, 0.35f), onSecondary = Color(0xFF14102B),
            background = t.darkBg, onBackground = Color(0xFFF1EFFA),
            surface = surface, onSurface = Color(0xFFF1EFFA),
            surfaceVariant = lerp(t.darkBg, Color.White, 0.16f), onSurfaceVariant = Color(0xFFD9D5EA),
            primaryContainer = lerp(t.primary, t.darkBg, 0.6f), onPrimaryContainer = Color(0xFFF1EFFA)
        )
    } else {
        lightColorScheme(
            primary = t.primary, onPrimary = Color.White,
            secondary = t.secondary, onSecondary = Color.White,
            background = t.lightBg, onBackground = Color(0xFF1C1B2A),
            surface = Color.White, onSurface = Color(0xFF1C1B2A),
            surfaceVariant = lerp(t.lightBg, t.primary, 0.12f), onSurfaceVariant = Color(0xFF3B3950),
            primaryContainer = lerp(t.primary, Color.White, 0.8f), onPrimaryContainer = Color(0xFF1C1B2A)
        )
    }
