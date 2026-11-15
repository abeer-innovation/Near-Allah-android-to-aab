package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrayerItemState
import com.example.data.model.PrayerType
import com.example.ui.components.AdMobBanner
import com.example.ui.components.AppHeader
import com.example.ui.components.CalendarSection
import com.example.ui.components.EmptyStateNotice
import com.example.ui.components.HorizontalPrayerRow
import com.example.ui.components.LocationHeaderBanner
import com.example.ui.components.PrayerItemCard
import com.example.ui.components.PrayerProgressCard
import com.example.ui.components.PrayerRemindersCard
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DarkBrownText
import com.example.ui.theme.HeaderDarkGreen
import com.example.ui.viewmodel.PrayerUiState
import java.time.LocalDate
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun HomeScreen(
    uiState: PrayerUiState,
    onProfileClick: () -> Unit,
    onChangeLocationClick: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onTogglePrayerCompletion: (PrayerType) -> Unit,
    onTogglePrayerReminder: (PrayerType) -> Unit,
    onResetDayPrayers: () -> Unit,
    onNotificationPermissionResult: (Boolean) -> Unit,
    onTestNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onNotificationPermissionResult(isGranted)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = HeaderDarkGreen.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CreamBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header with App Name, Tagline, and working Profile Icon
            Surface(
                color = HeaderDarkGreen,
                shadowElevation = 2.dp
            ) {
                Box(modifier = Modifier.statusBarsPadding()) {
                    AppHeader(
                        onProfileClick = onProfileClick
                    )
                }
            }

            // Main Content Area with Adaptive Sizing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("home_scroll_container"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Location Banner (Saved location displayed on home screen)
                    item {
                        LocationHeaderBanner(
                            location = uiState.selectedLocation,
                            calculationMethod = uiState.calculationMethod,
                            asrMethod = uiState.asrMethod,
                            onChangeLocationClick = onChangeLocationClick
                        )
                    }

                    // 2. Google AdMob Banner (Visible on open)
                    item {
                        AdMobBanner()
                    }

                    // 3. Monthly Calendar Section (Date aware, month/year boundary handling)
                    item {
                        CalendarSection(
                            currentMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            datesWithCompletedPrayers = uiState.datesWithCompletedPrayers,
                            onSelectDate = onSelectDate,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onGoToToday = onGoToToday
                        )
                    }

                    // 4. Immediately below calendar: Horizontal, swipeable row of cards for all five prayers
                    item {
                        HorizontalPrayerRow(
                            prayerList = uiState.currentPrayerList,
                            onToggleCompletion = onTogglePrayerCompletion,
                            onToggleReminder = onTogglePrayerReminder
                        )
                    }

                    // 5. Today's / Selected Date Prayer Progress Card (Dynamic "X of 5 prayers completed")
                    item {
                        PrayerProgressCard(
                            completedCount = uiState.completedCount,
                            totalCount = uiState.totalCount,
                            selectedDate = uiState.selectedDate,
                            isToday = uiState.isSelectedDateToday,
                            onResetClick = onResetDayPrayers
                        )
                    }

                    // 6. Prayer Reminders Quick-Control Card
                    item {
                        PrayerRemindersCard(
                            enabledCount = uiState.activeRemindersCount,
                            totalCount = uiState.totalCount,
                            hasNotificationPermission = uiState.hasNotificationPermission,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onNotificationPermissionResult(true)
                                }
                            },
                            onTestNotificationClick = onTestNotificationClick,
                            alarmSoundMode = uiState.alarmSoundMode
                        )
                    }

                    // 7. Empty state display when no prayers have been recorded yet for the selected date
                    if (!uiState.hasAnyCompletedRecord) {
                        item {
                            EmptyStateNotice()
                        }
                    }

                    // Section Title for Detailed Prayers
                    item {
                        Text(
                            text = "Daily Prayer Checklist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText,
                                fontSize = 17.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // 8. Five Prayer Cards (Fajr, Dhuhr (Zuhr), Asr, Maghrib, Isha)
                    items(
                        items = uiState.currentPrayerList,
                        key = { item: PrayerItemState -> item.type.name }
                    ) { prayerItemState: PrayerItemState ->
                        PrayerItemCard(
                            prayerState = prayerItemState,
                            onToggleCompletion = { onTogglePrayerCompletion(prayerItemState.type) },
                            onToggleReminder = { onTogglePrayerReminder(prayerItemState.type) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
