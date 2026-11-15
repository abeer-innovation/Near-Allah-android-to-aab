package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalTime

enum class PrayerType(
    val displayName: String,
    val arabicName: String,
    val subtitle: String,
    val icon: ImageVector,
    val order: Int
) {
    FAJR(
        displayName = "Fajr",
        arabicName = "الفجر",
        subtitle = "Dawn Prayer",
        icon = Icons.Outlined.Brightness5,
        order = 1
    ),
    DHUHR(
        displayName = "Dhuhr (Zuhr)",
        arabicName = "الظهر",
        subtitle = "Midday Prayer",
        icon = Icons.Filled.WbSunny,
        order = 2
    ),
    ASR(
        displayName = "Asr",
        arabicName = "العصر",
        subtitle = "Afternoon Prayer",
        icon = Icons.Filled.LightMode,
        order = 3
    ),
    MAGHRIB(
        displayName = "Maghrib",
        arabicName = "المغرب",
        subtitle = "Sunset Prayer",
        icon = Icons.Filled.WbTwilight,
        order = 4
    ),
    ISHA(
        displayName = "Isha",
        arabicName = "العشاء",
        subtitle = "Night Prayer",
        icon = Icons.Filled.Bedtime,
        order = 5
    )
}

data class PrayerItemState(
    val type: PrayerType,
    val isCompleted: Boolean = false,
    val calculatedTime: String = "--:--",
    val rawLocalTime: LocalTime? = null,
    val isNextPrayer: Boolean = false,
    val isReminderEnabled: Boolean = true,
    val hasJournalEntry: Boolean = false
) {
    val statusText: String
        get() = if (isCompleted) "Completed" else "Not completed"
}

enum class AppScreen {
    HOME,
    LOCATION,
    PROFILE
}
