package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppScreen
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocationScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScanNamazDialog
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.NearAllahTheme
import com.example.ui.viewmodel.PrayerViewModel

class MainActivity : ComponentActivity() {

    private val prayerViewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ui.components.AdMobConfig.initialize(applicationContext)
        setContent {
            NearAllahTheme {
                NearAllahApp(viewModel = prayerViewModel)
            }
        }
    }
}

@Composable
fun NearAllahApp(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    // AdMob Interstitial: Show on app open after the main screen loads, with frequency capping and religious protection
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500L)
        if (activity != null) {
            val isReligiousActive = com.example.reminder.PrayerAlarmService.isAlarmActive ||
                    uiState.isScanNamazDialogOpen ||
                    uiState.currentScreen != AppScreen.HOME
            com.example.ui.components.AdMobInterstitialManager.showAppOpenInterstitial(
                activity = activity,
                isReligiousActivityActive = isReligiousActive
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CreamBackground,
        bottomBar = {
            AppBottomNavigationBar(
                currentScreen = uiState.currentScreen,
                onNavigate = { screen -> viewModel.navigateTo(screen) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (uiState.currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        uiState = uiState,
                        onProfileClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                        onChangeLocationClick = { viewModel.navigateTo(AppScreen.LOCATION) },
                        onSelectDate = { date -> viewModel.selectDate(date) },
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onGoToToday = { viewModel.goToToday() },
                        onTogglePrayerCompletion = { type -> viewModel.togglePrayer(type) },
                        onTogglePrayerReminder = { type -> viewModel.togglePrayerReminder(type) },
                        onResetDayPrayers = { viewModel.resetSelectedDatePrayers() },
                        onNotificationPermissionResult = { granted -> viewModel.setNotificationPermissionGranted(granted) },
                        onTestNotificationClick = { viewModel.sendTestReminderNotification() }
                    )
                }
                AppScreen.LOCATION -> {
                    LocationScreen(
                        uiState = uiState,
                        onNavigateHome = { viewModel.navigateTo(AppScreen.HOME) },
                        onSelectCity = { city ->
                            viewModel.selectCity(city)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        onSearchQueryChange = { query -> viewModel.setCitySearchQuery(query) },
                        onCalculationMethodChange = { method -> viewModel.setCalculationMethod(method) },
                        onAsrMethodChange = { method -> viewModel.setAsrMethod(method) },
                        onDetectGpsLocation = { context -> viewModel.requestGpsLocation(context) },
                        onDismissError = { viewModel.dismissLocationError() }
                    )
                }
                AppScreen.PROFILE -> {
                    ProfileScreen(
                        uiState = uiState,
                        onNavigateHome = { viewModel.navigateTo(AppScreen.HOME) },
                        onOpenScanNamaz = { prayer, existingRecord ->
                            viewModel.openScanNamazDialog(prayer, null, existingRecord)
                        },
                        onDeleteJournalRecord = { record ->
                            viewModel.deleteJournalRecord(record)
                        },
                        onAlarmSoundModeChange = { mode ->
                            viewModel.setAlarmSoundMode(mode)
                        },
                        onTestAlarmClick = {
                            viewModel.sendTestReminderNotification()
                        },
                        onClearAllData = {
                            viewModel.clearAllUserDataAndPhotos()
                        }
                    )
                }
            }

            // Part 4: Scan Namaz Dialog
            if (uiState.isScanNamazDialogOpen) {
                ScanNamazDialog(
                    initialPrayer = uiState.scanNamazInitialPrayer,
                    initialDate = uiState.scanNamazInitialDate,
                    existingRecord = uiState.activeEditingRecord,
                    onDismiss = { viewModel.closeScanNamazDialog() },
                    onSaveRecord = { prayer, date, imagePath, note, existingId ->
                        viewModel.saveJournalRecord(prayer, date, imagePath, note, existingId)
                    },
                    checkDuplicate = { date, prayer ->
                        viewModel.checkExistingRecordFor(date, prayer)
                    }
                )
            }
        }
    }
}
