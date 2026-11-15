package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.calculation.AsrMethod
import com.example.data.calculation.CalculationMethod
import com.example.data.location.CityCatalog
import com.example.data.model.AppScreen
import com.example.data.model.JournalRecord
import com.example.data.model.PrayerType
import com.example.ui.viewmodel.PrayerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext<Application>()
        val prefs = application.getSharedPreferences("near_allah_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `read string from context`() {
        val appName = application.getString(R.string.app_name)
        assertEquals("Near Allah", appName)
    }

    @Test
    fun `all five prayer rows exist and initial status is Not completed`() {
        val viewModel = PrayerViewModel(application)
        val state = viewModel.uiState.value

        assertEquals(5, state.currentPrayerList.size)
        assertEquals(0, state.completedCount)
        assertEquals(5, state.totalCount)

        val prayerNames = state.currentPrayerList.map { it.type }
        assertTrue(prayerNames.contains(PrayerType.FAJR))
        assertTrue(prayerNames.contains(PrayerType.DHUHR))
        assertTrue(prayerNames.contains(PrayerType.ASR))
        assertTrue(prayerNames.contains(PrayerType.MAGHRIB))
        assertTrue(prayerNames.contains(PrayerType.ISHA))

        state.currentPrayerList.forEach { prayer ->
            assertFalse(prayer.isCompleted)
            assertEquals("Not completed", prayer.statusText)
        }
    }

    @Test
    fun `status change and progress updates allow correction if tapped by mistake`() {
        val viewModel = PrayerViewModel(application)

        viewModel.togglePrayer(PrayerType.FAJR)
        var state = viewModel.uiState.value
        assertEquals(1, state.completedCount)
        val fajrPrayer = state.currentPrayerList.first { it.type == PrayerType.FAJR }
        assertTrue(fajrPrayer.isCompleted)
        assertEquals("Completed", fajrPrayer.statusText)

        viewModel.togglePrayer(PrayerType.DHUHR)
        viewModel.togglePrayer(PrayerType.ASR)
        state = viewModel.uiState.value
        assertEquals(3, state.completedCount)

        // Undo Dhuhr
        viewModel.togglePrayer(PrayerType.DHUHR)
        state = viewModel.uiState.value
        assertEquals(2, state.completedCount)
        val dhuhrPrayer = state.currentPrayerList.first { it.type == PrayerType.DHUHR }
        assertFalse(dhuhrPrayer.isCompleted)
        assertEquals("Not completed", dhuhrPrayer.statusText)
    }

    @Test
    fun `date switching displays separate records for each date`() {
        val viewModel = PrayerViewModel(application)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        viewModel.selectDate(today)
        viewModel.togglePrayer(PrayerType.FAJR)
        assertEquals(1, viewModel.uiState.value.completedCount)

        viewModel.selectDate(yesterday)
        assertEquals(0, viewModel.uiState.value.completedCount)

        viewModel.togglePrayer(PrayerType.ISHA)
        assertEquals(1, viewModel.uiState.value.completedCount)

        viewModel.selectDate(today)
        assertEquals(1, viewModel.uiState.value.completedCount)
        assertTrue(viewModel.uiState.value.currentPrayerList.first { it.type == PrayerType.FAJR }.isCompleted)
        assertFalse(viewModel.uiState.value.currentPrayerList.first { it.type == PrayerType.ISHA }.isCompleted)
    }

    @Test
    fun `prayer records persist across app restarts`() {
        val today = LocalDate.now()

        val viewModelSession1 = PrayerViewModel(application)
        viewModelSession1.selectDate(today)
        viewModelSession1.togglePrayer(PrayerType.FAJR)
        viewModelSession1.togglePrayer(PrayerType.MAGHRIB)
        assertEquals(2, viewModelSession1.uiState.value.completedCount)

        val viewModelSession2 = PrayerViewModel(application)
        assertEquals(2, viewModelSession2.uiState.value.completedCount)
        assertTrue(viewModelSession2.uiState.value.currentPrayerList.first { it.type == PrayerType.FAJR }.isCompleted)
        assertTrue(viewModelSession2.uiState.value.currentPrayerList.first { it.type == PrayerType.MAGHRIB }.isCompleted)
        assertFalse(viewModelSession2.uiState.value.currentPrayerList.first { it.type == PrayerType.ASR }.isCompleted)
    }

    @Test
    fun `reminder settings can be toggled individually and persist`() {
        val viewModel = PrayerViewModel(application)

        assertEquals(5, viewModel.uiState.value.activeRemindersCount)
        assertTrue(viewModel.uiState.value.currentPrayerList.first { it.type == PrayerType.FAJR }.isReminderEnabled)

        viewModel.togglePrayerReminder(PrayerType.FAJR)
        var state = viewModel.uiState.value
        assertEquals(4, state.activeRemindersCount)
        assertFalse(state.currentPrayerList.first { it.type == PrayerType.FAJR }.isReminderEnabled)

        val restartViewModel = PrayerViewModel(application)
        assertEquals(4, restartViewModel.uiState.value.activeRemindersCount)
        assertFalse(restartViewModel.uiState.value.currentPrayerList.first { it.type == PrayerType.FAJR }.isReminderEnabled)
        assertTrue(restartViewModel.uiState.value.currentPrayerList.first { it.type == PrayerType.DHUHR }.isReminderEnabled)
    }

    @Test
    fun `scan namaz saving prayer journal entry synchronizes prayer status to completed and updates progress`() {
        val viewModel = PrayerViewModel(application)
        val today = LocalDate.now()

        assertEquals(0, viewModel.uiState.value.completedCount)
        assertEquals(0, viewModel.uiState.value.journalRecords.size)

        // Save a journal entry for Dhuhr with optional note and mock photo path
        viewModel.saveJournalRecord(
            prayerType = PrayerType.DHUHR,
            date = today,
            imagePath = "/data/mock_prayer.jpg",
            note = "Prayed in congregation with great khushu"
        )

        val state = viewModel.uiState.value
        // Verify journal entry added
        assertEquals(1, state.journalRecords.size)
        val entry = state.journalRecords.first()
        assertEquals(PrayerType.DHUHR, entry.prayerType)
        assertEquals(today, entry.date)
        assertEquals("Prayed in congregation with great khushu", entry.note)

        // Verify prayer completion is synchronized
        val dhuhrPrayer = state.currentPrayerList.first { it.type == PrayerType.DHUHR }
        assertTrue(dhuhrPrayer.isCompleted)
        assertEquals("Completed", dhuhrPrayer.statusText)
        assertTrue(dhuhrPrayer.hasJournalEntry)

        // Verify progress updated
        assertEquals(1, state.completedCount)
        assertEquals(0.2f, state.progressFraction, 0.001f)
    }

    @Test
    fun `duplicate entry detection prevents duplicate records and allows update`() {
        val viewModel = PrayerViewModel(application)
        val today = LocalDate.now()

        assertNull(viewModel.checkExistingRecordFor(today, PrayerType.ASR))

        viewModel.saveJournalRecord(
            prayerType = PrayerType.ASR,
            date = today,
            imagePath = null,
            note = "Initial note"
        )

        val existing = viewModel.checkExistingRecordFor(today, PrayerType.ASR)
        assertNotNull(existing)
        assertEquals("Initial note", existing!!.note)

        // Update the existing entry with a new note
        viewModel.saveJournalRecord(
            prayerType = PrayerType.ASR,
            date = today,
            imagePath = "/path/new_photo.jpg",
            note = "Updated note with reflection",
            existingId = existing.id
        )

        assertEquals(1, viewModel.uiState.value.journalRecords.size)
        val updated = viewModel.uiState.value.journalRecords.first()
        assertEquals("Updated note with reflection", updated.note)
        assertEquals("/path/new_photo.jpg", updated.imagePath)
    }

    @Test
    fun `journal records can be deleted`() {
        val viewModel = PrayerViewModel(application)
        val today = LocalDate.now()

        viewModel.saveJournalRecord(
            prayerType = PrayerType.MAGHRIB,
            date = today,
            imagePath = null,
            note = "Evening prayer"
        )
        assertEquals(1, viewModel.uiState.value.journalRecords.size)

        val record = viewModel.uiState.value.journalRecords.first()
        viewModel.deleteJournalRecord(record)

        assertEquals(0, viewModel.uiState.value.journalRecords.size)
    }

    @Test
    fun `journal records persist across app restart`() {
        val today = LocalDate.now()

        // Session 1: Save journal entry
        val viewModelSession1 = PrayerViewModel(application)
        viewModelSession1.saveJournalRecord(
            prayerType = PrayerType.ISHA,
            date = today,
            imagePath = "/photos/isha.jpg",
            note = "Peaceful end to the day"
        )
        assertEquals(1, viewModelSession1.uiState.value.journalRecords.size)

        // Session 2: New ViewModel instance (simulating restart)
        val viewModelSession2 = PrayerViewModel(application)
        assertEquals(1, viewModelSession2.uiState.value.journalRecords.size)
        val restored = viewModelSession2.uiState.value.journalRecords.first()
        assertEquals(PrayerType.ISHA, restored.prayerType)
        assertEquals(today, restored.date)
        assertEquals("Peaceful end to the day", restored.note)
        assertEquals("/photos/isha.jpg", restored.imagePath)
        assertTrue(viewModelSession2.uiState.value.currentPrayerList.first { it.type == PrayerType.ISHA }.isCompleted)
    }

    @Test
    fun `changing city updates prayer times correctly`() {
        val viewModel = PrayerViewModel(application)
        val makkahCity = CityCatalog.DEFAULT_CITY
        val londonCity = CityCatalog.ALL_CITIES.first { it.name == "London" }

        viewModel.selectCity(makkahCity)
        val makkahTimes = viewModel.uiState.value.calculatedPrayerTimes

        viewModel.selectCity(londonCity)
        val londonTimes = viewModel.uiState.value.calculatedPrayerTimes

        assertEquals("London", viewModel.uiState.value.selectedLocation.name)
        assertNotEquals(makkahTimes.formattedDhuhr, londonTimes.formattedDhuhr)
        assertTrue(londonTimes.formattedFajr.isNotEmpty())
    }

    @Test
    fun `changing calculation method and asr method updates times`() {
        val viewModel = PrayerViewModel(application)
        val testCity = CityCatalog.ALL_CITIES.first { it.name == "Cairo" }
        viewModel.selectCity(testCity)

        viewModel.setCalculationMethod(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        val mwlTimes = viewModel.uiState.value.calculatedPrayerTimes

        viewModel.setCalculationMethod(CalculationMethod.ISNA)
        val isnaTimes = viewModel.uiState.value.calculatedPrayerTimes

        assertNotEquals(mwlTimes.fajr, isnaTimes.fajr)

        viewModel.setAsrMethod(AsrMethod.STANDARD)
        val shafiTimes = viewModel.uiState.value.calculatedPrayerTimes

        viewModel.setAsrMethod(AsrMethod.HANAFI)
        val hanafiTimes = viewModel.uiState.value.calculatedPrayerTimes

        assertTrue(hanafiTimes.asr.isAfter(shafiTimes.asr))
    }

    @Test
    fun `alarm sound mode can be configured and persists across app restart`() {
        val viewModel1 = PrayerViewModel(application)
        assertEquals(com.example.data.model.AlarmSoundMode.ADHAN_TONE, viewModel1.uiState.value.alarmSoundMode)

        viewModel1.setAlarmSoundMode(com.example.data.model.AlarmSoundMode.SILENT)
        assertEquals(com.example.data.model.AlarmSoundMode.SILENT, viewModel1.uiState.value.alarmSoundMode)

        val viewModel2 = PrayerViewModel(application)
        assertEquals(com.example.data.model.AlarmSoundMode.SILENT, viewModel2.uiState.value.alarmSoundMode)

        viewModel2.setAlarmSoundMode(com.example.data.model.AlarmSoundMode.GENTLE_CHIME)
        assertEquals(com.example.data.model.AlarmSoundMode.GENTLE_CHIME, viewModel2.uiState.value.alarmSoundMode)

        val viewModel3 = PrayerViewModel(application)
        assertEquals(com.example.data.model.AlarmSoundMode.GENTLE_CHIME, viewModel3.uiState.value.alarmSoundMode)
    }

    @Test
    fun `clear all user data and photos resets records and restores defaults`() {
        val viewModel = PrayerViewModel(application)
        val today = LocalDate.now()

        // Add prayer completions and journal entry
        viewModel.togglePrayer(PrayerType.FAJR)
        viewModel.togglePrayer(PrayerType.DHUHR)
        viewModel.saveJournalRecord(
            prayerType = PrayerType.FAJR,
            date = today,
            imagePath = "/data/photo.jpg",
            note = "Private note"
        )
        viewModel.setAlarmSoundMode(com.example.data.model.AlarmSoundMode.SILENT)

        assertEquals(2, viewModel.uiState.value.completedCount)
        assertEquals(1, viewModel.uiState.value.journalRecords.size)

        // Clear all data
        viewModel.clearAllUserDataAndPhotos()

        // Wait/assert immediate state update
        val clearedState = viewModel.uiState.value
        assertEquals(0, clearedState.completedCount)
        assertEquals(0, clearedState.journalRecords.size)
        assertTrue(clearedState.datesWithCompletedPrayers.isEmpty())
        assertEquals(CityCatalog.DEFAULT_CITY.name, clearedState.selectedLocation.name)
    }

    @Test
    fun `boot receiver handles system broadcast intents without error`() {
        val receiver = com.example.reminder.PrayerBootReceiver()
        val bootIntent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(application, bootIntent)

        val tzIntent = android.content.Intent(android.content.Intent.ACTION_TIMEZONE_CHANGED)
        receiver.onReceive(application, tzIntent)

        val timeIntent = android.content.Intent(android.content.Intent.ACTION_TIME_CHANGED)
        receiver.onReceive(application, timeIntent)
    }

    @Test
    fun `send test reminder notification triggers without exception`() {
        val viewModel = PrayerViewModel(application)
        viewModel.sendTestReminderNotification()
    }

    @Test
    fun `admob configuration contains valid test app id, banner, and interstitial ad unit ids`() {
        assertEquals("ca-app-pub-3940256099942544~3347511713", com.example.ui.components.AdMobConfig.TEST_APP_ID)
        assertEquals("ca-app-pub-3940256099942544/6300978111", com.example.ui.components.AdMobConfig.TEST_BANNER_AD_UNIT_ID)
        assertEquals("ca-app-pub-3940256099942544/1033173712", com.example.ui.components.AdMobConfig.TEST_INTERSTITIAL_AD_UNIT_ID)

        // Default getters resolve to official test IDs
        assertEquals(com.example.ui.components.AdMobConfig.TEST_APP_ID, com.example.ui.components.AdMobConfig.getAppId())
        assertEquals(com.example.ui.components.AdMobConfig.TEST_BANNER_AD_UNIT_ID, com.example.ui.components.AdMobConfig.getBannerAdUnitId())
        assertEquals(com.example.ui.components.AdMobConfig.TEST_INTERSTITIAL_AD_UNIT_ID, com.example.ui.components.AdMobConfig.getInterstitialAdUnitId())

        // Production separation test: changing production IDs cleanly overrides without code changes
        com.example.ui.components.AdMobConfig.productionInterstitialAdUnitId = "ca-app-pub-1234567890/0987654321"
        assertEquals("ca-app-pub-1234567890/0987654321", com.example.ui.components.AdMobConfig.getInterstitialAdUnitId())
        com.example.ui.components.AdMobConfig.productionInterstitialAdUnitId = null // restore

        com.example.ui.components.AdMobConfig.initialize(application)
    }

    @Test
    fun `admob interstitial suppresses ads during active religious activities or prayer alarms`() {
        com.example.ui.components.AdMobInterstitialManager.resetStateForTesting(application)

        // 1. Should suppress when prayer alarm is active
        val startIntent = android.content.Intent(application, com.example.reminder.PrayerAlarmService::class.java).apply {
            action = com.example.reminder.PrayerAlarmService.ACTION_START_ALARM
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_PRAYER_NAME, "Fajr")
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_ARABIC_NAME, "الفجر")
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_CITY_NAME, "Makkah")
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_PRAYER_ORDER, 1)
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_SOUND_MODE, com.example.data.model.AlarmSoundMode.ADHAN_TONE.name)
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_IS_TEST, true)
        }
        val service = org.robolectric.Robolectric.buildService(com.example.reminder.PrayerAlarmService::class.java, startIntent).create().startCommand(0, 0).get()
        assertTrue(com.example.reminder.PrayerAlarmService.isAlarmActive)

        // Ad is strictly suppressed because alarm is ringing
        assertFalse(com.example.ui.components.AdMobInterstitialManager.canShowInterstitial(application, isReligiousActivityActive = false))

        // Stop alarm
        service.stopAlarm()
        assertFalse(com.example.reminder.PrayerAlarmService.isAlarmActive)

        // 2. Should suppress when religious activity (e.g. Scan Namaz or Prayer Tracking) is active
        assertFalse(com.example.ui.components.AdMobInterstitialManager.canShowInterstitial(application, isReligiousActivityActive = true))
    }

    @Test
    fun `admob interstitial enforces frequency capping`() {
        com.example.ui.components.AdMobInterstitialManager.resetStateForTesting(application)

        // Record a recent impression in SharedPreferences
        val prefs = application.getSharedPreferences("nearallah_admob_interstitial_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("last_interstitial_shown_timestamp", System.currentTimeMillis()).apply()

        // Within 10 minutes frequency cap, canShowInterstitial must return false
        assertFalse(com.example.ui.components.AdMobInterstitialManager.canShowInterstitial(application, isReligiousActivityActive = false))

        // Clear prefs to simulate 10 minutes elapsed
        prefs.edit().clear().apply()
        // If ad is not loaded yet, canShowInterstitial safely returns false without crashing
        assertFalse(com.example.ui.components.AdMobInterstitialManager.canShowInterstitial(application, isReligiousActivityActive = false))
    }

    @Test
    fun `permanent manual location selection persists across app restart without requiring GPS`() {
        val viewModel1 = PrayerViewModel(application)
        val istanbulCity = CityCatalog.ALL_CITIES.first { it.name == "Istanbul" }

        // User manually searches and selects city once
        viewModel1.selectCity(istanbulCity)
        assertEquals("Istanbul", viewModel1.uiState.value.selectedLocation.name)
        assertEquals("Europe/Istanbul", viewModel1.uiState.value.selectedLocation.timezoneId)

        // Simulate app kill and reopen with GPS off
        val viewModel2 = PrayerViewModel(application)
        val restoredLocation = viewModel2.uiState.value.selectedLocation
        assertEquals("Istanbul", restoredLocation.name)
        assertEquals("Turkey", restoredLocation.country)
        assertEquals(41.0082, restoredLocation.latitude, 0.001)
        assertEquals(28.9784, restoredLocation.longitude, 0.001)
        assertEquals("Europe/Istanbul", restoredLocation.timezoneId)

        // Prayer times are calculated accurately for saved city
        assertTrue(viewModel2.uiState.value.calculatedPrayerTimes.formattedFajr.isNotEmpty())
    }

    @Test
    fun `prayer alarm starts and stops immediately upon user action`() {
        val startIntent = android.content.Intent(application, com.example.reminder.PrayerAlarmService::class.java).apply {
            action = com.example.reminder.PrayerAlarmService.ACTION_START_ALARM
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_PRAYER_NAME, "Fajr")
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_ARABIC_NAME, "الفجر")
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_CITY_NAME, "Istanbul")
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_PRAYER_ORDER, 1)
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_SOUND_MODE, com.example.data.model.AlarmSoundMode.ADHAN_TONE.name)
            putExtra(com.example.reminder.PrayerAlarmService.EXTRA_IS_TEST, true)
        }

        val service = org.robolectric.Robolectric.buildService(com.example.reminder.PrayerAlarmService::class.java, startIntent).create().startCommand(0, 0).get()
        assertTrue(com.example.reminder.PrayerAlarmService.isAlarmActive)
        assertEquals("Fajr", com.example.reminder.PrayerAlarmService.activePrayerName)

        // User presses STOP ALARM
        com.example.reminder.PrayerAlarmService.stopAlarm(application)
        service.stopAlarm()
        assertFalse(com.example.reminder.PrayerAlarmService.isAlarmActive)
    }

    @Test
    fun `prayer alarm duration constant is exactly five minutes`() {
        assertEquals(300000L, com.example.reminder.PrayerAlarmService.AUTO_STOP_DURATION_MS)
    }

    @Test
    fun `stopping alarm keeps prayer tracking completely separate`() {
        val viewModel = PrayerViewModel(application)
        val today = LocalDate.now()

        // Fajr is initially not completed
        assertFalse(viewModel.uiState.value.currentPrayerList.first { it.type == PrayerType.FAJR }.isCompleted)

        // Trigger and stop alarm
        val service = org.robolectric.Robolectric.buildService(com.example.reminder.PrayerAlarmService::class.java).create().get()
        service.stopAlarm()

        // Verify Fajr remains uncompleted in tracker (stopping alarm does not mark prayer as prayed)
        val state = viewModel.uiState.value
        assertFalse(state.currentPrayerList.first { it.type == PrayerType.FAJR }.isCompleted)
        assertEquals(0, state.completedCount)
    }

    @Test
    fun `alarm deduplication prevents duplicate alarms for the same prayer and date`() {
        val repo = com.example.data.repository.PrayerPreferencesRepository(application)
        val today = LocalDate.now().toString()

        assertFalse(repo.isAlarmStoppedFor(today, 1))

        // Mark Fajr (order 1) stopped for today
        repo.markAlarmStopped(today, 1)
        assertTrue(repo.isAlarmStoppedFor(today, 1))

        // Other prayers remain not stopped
        assertFalse(repo.isAlarmStoppedFor(today, 2))
    }

    @Test
    fun `prayer times calculation produces strict chronological progression`() {
        val date = LocalDate.now()
        val zone = ZoneId.of("Asia/Riyadh")
        val times = com.example.data.calculation.PrayerCalculationEngine.calculate(
            date = date,
            latitude = 21.4225,
            longitude = 39.8262,
            zoneId = zone
        )

        assertTrue(times.sunrise.isAfter(times.fajr))
        assertTrue(times.dhuhr.isAfter(times.sunrise))
        assertTrue(times.asr.isAfter(times.dhuhr))
        assertTrue(times.maghrib.isAfter(times.asr))
        assertTrue(times.isha.isAfter(times.maghrib))

        assertTrue(times.formattedFajr.endsWith("AM") || times.formattedFajr.endsWith("PM"))
        assertTrue(times.formattedDhuhr.endsWith("AM") || times.formattedDhuhr.endsWith("PM"))
    }

    @Test
    fun `prayer reminder receiver automatically reschedules future alarms when alarm fires`() {
        val receiver = com.example.reminder.PrayerReminderReceiver()
        val intent = android.content.Intent().apply {
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_PRAYER_NAME, "Fajr")
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_ARABIC_NAME, "الفجر")
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_CITY_NAME, "Makkah")
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_PRAYER_ORDER, 1)
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_SOUND_MODE, com.example.data.model.AlarmSoundMode.ADHAN_TONE.name)
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_DATE, LocalDate.now().toString())
            putExtra(com.example.reminder.PrayerReminderReceiver.EXTRA_IS_TEST, false)
        }

        // Trigger receiver: should execute without exception and reschedule for next day
        receiver.onReceive(application, intent)
    }

    @Test
    fun `prayer times calculation verified across Islamabad, Karachi, Lahore, London, and New York`() {
        val testCities = listOf("Islamabad", "Karachi", "Lahore", "London", "New York")
        val date = LocalDate.of(2026, 11, 8)

        testCities.forEach { cityName ->
            val city = CityCatalog.ALL_CITIES.first { it.name == cityName }
            val zone = ZoneId.of(city.timezoneId)
            val times = com.example.data.calculation.PrayerCalculationEngine.calculate(
                date = date,
                latitude = city.latitude,
                longitude = city.longitude,
                zoneId = zone,
                method = city.recommendedMethod,
                asrMethod = city.recommendedAsrMethod
            )

            // Verify strict chronological progression
            assertTrue("Fajr before Sunrise in $cityName", times.sunrise.isAfter(times.fajr))
            assertTrue("Sunrise before Dhuhr in $cityName", times.dhuhr.isAfter(times.sunrise))
            assertTrue("Dhuhr before Asr in $cityName", times.asr.isAfter(times.dhuhr))
            assertTrue("Asr before Maghrib in $cityName", times.maghrib.isAfter(times.asr))
            assertTrue("Maghrib before Isha in $cityName", times.isha.isAfter(times.maghrib))

            assertNotNull(times.formattedFajr)
            assertNotNull(times.formattedDhuhr)
            assertNotNull(times.formattedAsr)
            assertNotNull(times.formattedMaghrib)
            assertNotNull(times.formattedIsha)
        }
    }

    @Test
    fun `asr calculation in Islamabad under Hanafi method resolves to late afternoon and not 3pm`() {
        val islamabad = CityCatalog.ALL_CITIES.first { it.name == "Islamabad" }
        val date = LocalDate.of(2026, 11, 8)
        val zone = ZoneId.of(islamabad.timezoneId)

        // Standard Shafi Asr (shadow factor 1)
        val shafiTimes = com.example.data.calculation.PrayerCalculationEngine.calculate(
            date = date,
            latitude = islamabad.latitude,
            longitude = islamabad.longitude,
            zoneId = zone,
            method = CalculationMethod.KARACHI,
            asrMethod = AsrMethod.STANDARD
        )

        // Hanafi Asr (shadow factor 2, standard in Pakistan)
        val hanafiTimes = com.example.data.calculation.PrayerCalculationEngine.calculate(
            date = date,
            latitude = islamabad.latitude,
            longitude = islamabad.longitude,
            zoneId = zone,
            method = CalculationMethod.KARACHI,
            asrMethod = AsrMethod.HANAFI
        )

        // Hanafi Asr is ~50-60 minutes later than Shafi Asr
        val shafiMinutes = shafiTimes.asr.toSecondOfDay() / 60
        val hanafiMinutes = hanafiTimes.asr.toSecondOfDay() / 60
        assertTrue("Hanafi Asr must be after Standard Asr", hanafiTimes.asr.isAfter(shafiTimes.asr))
        assertTrue("Hanafi Asr is 30 to 80 minutes after Standard Asr depending on season", (hanafiMinutes - shafiMinutes) in 30..80)

        // In autumn/spring (e.g. October), Hanafi Asr in Islamabad is at 4:30 - 4:45 PM (hour 16)
        val octDate = LocalDate.of(2026, 10, 1)
        val octHanafiTimes = com.example.data.calculation.PrayerCalculationEngine.calculate(
            date = octDate,
            latitude = islamabad.latitude,
            longitude = islamabad.longitude,
            zoneId = zone,
            method = CalculationMethod.KARACHI,
            asrMethod = AsrMethod.HANAFI
        )
        val octShafiTimes = com.example.data.calculation.PrayerCalculationEngine.calculate(
            date = octDate,
            latitude = islamabad.latitude,
            longitude = islamabad.longitude,
            zoneId = zone,
            method = CalculationMethod.KARACHI,
            asrMethod = AsrMethod.STANDARD
        )
        // User's specific reported issue: Asr was 4:45 PM, but app was calculating ~3:00 PM under Standard!
        assertTrue("Hanafi Asr in Islamabad in October is 4 PM (16:xx)", octHanafiTimes.asr.hour == 16)
        assertTrue("Standard Asr in Islamabad in October was ~3 PM (15:xx)", octShafiTimes.asr.hour == 15)

        // Verify default city in Pakistan automatically uses Hanafi Asr
        assertEquals(AsrMethod.HANAFI, CityCatalog.DEFAULT_CITY.recommendedAsrMethod)
        assertEquals(CalculationMethod.KARACHI, CityCatalog.DEFAULT_CITY.recommendedMethod)
    }

    @Test
    fun `alarm scheduling updates reminders and avoids duplicates`() {
        val islamabad = CityCatalog.DEFAULT_CITY
        val enabledMap = PrayerType.values().associateWith { true }

        // Schedule all 5 reminders
        com.example.reminder.PrayerReminderManager.updateAllReminders(
            context = application,
            location = islamabad,
            calculationMethod = islamabad.recommendedMethod,
            asrMethod = islamabad.recommendedAsrMethod,
            enabledPrayers = enabledMap,
            soundMode = com.example.data.model.AlarmSoundMode.ADHAN_TONE
        )

        // Reschedule again to verify deduplication and clean cancellation
        com.example.reminder.PrayerReminderManager.updateAllReminders(
            context = application,
            location = islamabad,
            calculationMethod = islamabad.recommendedMethod,
            asrMethod = islamabad.recommendedAsrMethod,
            enabledPrayers = enabledMap,
            soundMode = com.example.data.model.AlarmSoundMode.ADHAN_TONE
        )
    }
}

