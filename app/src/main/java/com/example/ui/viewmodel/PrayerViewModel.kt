package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calculation.AsrMethod
import com.example.data.calculation.CalculatedPrayerTimes
import com.example.data.calculation.CalculationMethod
import com.example.data.calculation.PrayerCalculationEngine
import com.example.data.location.CityCatalog
import com.example.data.location.CityLocation
import com.example.data.location.LocationHelper
import com.example.data.model.AlarmSoundMode
import com.example.data.model.AppScreen
import com.example.data.model.JournalRecord
import com.example.data.model.PrayerItemState
import com.example.data.model.PrayerType
import com.example.data.repository.PhotoStorageHelper
import com.example.data.repository.PrayerPreferencesRepository
import com.example.reminder.PrayerReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class PrayerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val currentScreen: AppScreen = AppScreen.HOME,
    // Location & Calculation Settings
    val selectedLocation: CityLocation = CityCatalog.DEFAULT_CITY,
    val calculationMethod: CalculationMethod = CityCatalog.DEFAULT_CITY.recommendedMethod,
    val asrMethod: AsrMethod = CityCatalog.DEFAULT_CITY.recommendedAsrMethod,
    val citySearchQuery: String = "",
    val isDetectingLocation: Boolean = false,
    val locationErrorMessage: String? = null,
    val isLocationPermissionGranted: Boolean = false,
    // Notification permission, Alarms & Audio Settings
    val hasNotificationPermission: Boolean = false,
    val canScheduleExactAlarms: Boolean = true,
    val alarmSoundMode: AlarmSoundMode = AlarmSoundMode.ADHAN_TONE,
    val prayerRemindersEnabled: Map<PrayerType, Boolean> = PrayerType.values().associateWith { true },
    // Prayer completion records for selected date: Map of PrayerType -> isCompleted
    val selectedDatePrayers: Map<PrayerType, Boolean> = emptyMap(),
    // Set of all dates that have at least one completed prayer (for calendar dots)
    val datesWithCompletedPrayers: Set<LocalDate> = emptySet(),
    // Part 4 & 5: Prayer Photo Journal & Scan Namaz
    val journalRecords: List<JournalRecord> = emptyList(),
    val isScanNamazDialogOpen: Boolean = false,
    val scanNamazInitialPrayer: PrayerType = PrayerType.FAJR,
    val scanNamazInitialDate: LocalDate = LocalDate.now(),
    val activeEditingRecord: JournalRecord? = null
) {
    val isSelectedDateToday: Boolean
        get() = selectedDate == LocalDate.now()

    val calculatedPrayerTimes: CalculatedPrayerTimes
        get() {
            val zoneId = try {
                ZoneId.of(selectedLocation.timezoneId)
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }

            return PrayerCalculationEngine.calculate(
                date = selectedDate,
                latitude = selectedLocation.latitude,
                longitude = selectedLocation.longitude,
                zoneId = zoneId,
                method = calculationMethod,
                asrMethod = asrMethod
            )
        }

    val currentPrayerList: List<PrayerItemState>
        get() {
            val times = calculatedPrayerTimes
            val nowTime = LocalTime.now()

            val prayersWithTimes = listOf(
                PrayerType.FAJR to times.fajr,
                PrayerType.DHUHR to times.dhuhr,
                PrayerType.ASR to times.asr,
                PrayerType.MAGHRIB to times.maghrib,
                PrayerType.ISHA to times.isha
            )

            val nextPrayerType = if (isSelectedDateToday) {
                prayersWithTimes.firstOrNull { it.second.isAfter(nowTime) }?.first ?: PrayerType.FAJR
            } else null

            val journalPrayersForSelectedDate = journalRecords
                .filter { it.date == selectedDate }
                .map { it.prayerType }
                .toSet()

            return PrayerType.values().map { type ->
                val timeString = when (type) {
                    PrayerType.FAJR -> times.formattedFajr
                    PrayerType.DHUHR -> times.formattedDhuhr
                    PrayerType.ASR -> times.formattedAsr
                    PrayerType.MAGHRIB -> times.formattedMaghrib
                    PrayerType.ISHA -> times.formattedIsha
                }
                val rawTime = when (type) {
                    PrayerType.FAJR -> times.fajr
                    PrayerType.DHUHR -> times.dhuhr
                    PrayerType.ASR -> times.asr
                    PrayerType.MAGHRIB -> times.maghrib
                    PrayerType.ISHA -> times.isha
                }

                PrayerItemState(
                    type = type,
                    isCompleted = selectedDatePrayers[type] ?: false,
                    calculatedTime = timeString,
                    rawLocalTime = rawTime,
                    isNextPrayer = (type == nextPrayerType),
                    isReminderEnabled = prayerRemindersEnabled[type] ?: true,
                    hasJournalEntry = journalPrayersForSelectedDate.contains(type)
                )
            }
        }

    val completedCount: Int
        get() = currentPrayerList.count { it.isCompleted }

    val totalCount: Int = 5

    val progressFraction: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    val hasAnyCompletedRecord: Boolean
        get() = completedCount > 0

    val activeRemindersCount: Int
        get() = prayerRemindersEnabled.values.count { it }

    val filteredCities: List<CityLocation>
        get() = CityCatalog.searchCities(citySearchQuery)
}

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrayerPreferencesRepository(application)
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        rescheduleAlarms()
    }

    private fun createInitialState(): PrayerUiState {
        val today = LocalDate.now()
        val savedLocation = repository.getSavedLocation()
        val savedCalcMethod = repository.getSavedCalculationMethod()
        val savedAsrMethod = repository.getSavedAsrMethod()
        val savedDatePrayers = repository.getAllPrayersForDate(today)
        val trackedDates = repository.getDatesWithCompletedPrayers()
        val savedJournals = repository.getAllJournalRecords()
        val savedSoundMode = repository.getAlarmSoundMode()
        val canExact = PrayerReminderManager.canScheduleExactAlarms(getApplication())
        val reminders = PrayerType.values().associateWith { type ->
            repository.isReminderEnabled(type)
        }

        return PrayerUiState(
            selectedDate = today,
            currentMonth = YearMonth.now(),
            currentScreen = AppScreen.HOME,
            selectedLocation = savedLocation,
            calculationMethod = savedCalcMethod,
            asrMethod = savedAsrMethod,
            selectedDatePrayers = savedDatePrayers,
            datesWithCompletedPrayers = trackedDates,
            prayerRemindersEnabled = reminders,
            journalRecords = savedJournals,
            alarmSoundMode = savedSoundMode,
            canScheduleExactAlarms = canExact
        )
    }

    fun selectDate(date: LocalDate) {
        val datePrayers = repository.getAllPrayersForDate(date)
        _uiState.update { state ->
            val newMonth = YearMonth.from(date)
            state.copy(
                selectedDate = date,
                currentMonth = if (state.currentMonth != newMonth) newMonth else state.currentMonth,
                selectedDatePrayers = datePrayers
            )
        }
    }

    fun togglePrayer(type: PrayerType) {
        _uiState.update { state ->
            val date = state.selectedDate
            val currentStatus = state.selectedDatePrayers[type] ?: false
            val newStatus = !currentStatus

            repository.setPrayerStatus(date, type, newStatus)

            val updatedPrayers = state.selectedDatePrayers.toMutableMap()
            updatedPrayers[type] = newStatus

            state.copy(
                selectedDatePrayers = updatedPrayers,
                datesWithCompletedPrayers = repository.getDatesWithCompletedPrayers()
            )
        }
    }

    fun togglePrayerReminder(type: PrayerType) {
        _uiState.update { state ->
            val current = state.prayerRemindersEnabled[type] ?: true
            val updated = !current
            repository.setReminderEnabled(type, updated)

            val updatedMap = state.prayerRemindersEnabled.toMutableMap()
            updatedMap[type] = updated
            state.copy(prayerRemindersEnabled = updatedMap)
        }
        rescheduleAlarms()
    }

    fun setAlarmSoundMode(mode: AlarmSoundMode) {
        repository.setAlarmSoundMode(mode)
        _uiState.update { state ->
            state.copy(alarmSoundMode = mode)
        }
        rescheduleAlarms()
    }

    fun sendTestReminderNotification() {
        val state = _uiState.value
        PrayerReminderManager.sendImmediateTestNotification(
            context = getApplication(),
            type = PrayerType.FAJR,
            cityName = state.selectedLocation.name,
            soundMode = state.alarmSoundMode
        )
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _uiState.update { state ->
            state.copy(hasNotificationPermission = granted)
        }
        if (granted) {
            rescheduleAlarms()
        }
    }

    private fun rescheduleAlarms() {
        val state = _uiState.value
        PrayerReminderManager.updateAllReminders(
            context = getApplication(),
            location = state.selectedLocation,
            calculationMethod = state.calculationMethod,
            asrMethod = state.asrMethod,
            enabledPrayers = state.prayerRemindersEnabled,
            soundMode = state.alarmSoundMode
        )
    }

    fun selectCity(city: CityLocation) {
        repository.saveSelectedLocation(city)
        repository.setLocationUserSelected(true)
        repository.saveCalculationMethod(city.recommendedMethod)
        repository.saveAsrMethod(city.recommendedAsrMethod)
        _uiState.update { state ->
            state.copy(
                selectedLocation = city,
                calculationMethod = city.recommendedMethod,
                asrMethod = city.recommendedAsrMethod,
                citySearchQuery = "",
                locationErrorMessage = null
            )
        }
        rescheduleAlarms()
    }

    fun setCalculationMethod(method: CalculationMethod) {
        repository.saveCalculationMethod(method)
        _uiState.update { state ->
            state.copy(calculationMethod = method)
        }
        rescheduleAlarms()
    }

    fun setAsrMethod(method: AsrMethod) {
        repository.saveAsrMethod(method)
        _uiState.update { state ->
            state.copy(asrMethod = method)
        }
        rescheduleAlarms()
    }

    fun setCitySearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(citySearchQuery = query)
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { state ->
            state.copy(isLocationPermissionGranted = granted)
        }
    }

    fun requestGpsLocation(context: Context) {
        _uiState.update { it.copy(isDetectingLocation = true, locationErrorMessage = null) }
        viewModelScope.launch {
            val result = LocationHelper.fetchCurrentLocation(context)
            result.onSuccess { detectedCity ->
                repository.saveSelectedLocation(detectedCity)
                repository.setLocationUserSelected(true)
                repository.saveCalculationMethod(detectedCity.recommendedMethod)
                repository.saveAsrMethod(detectedCity.recommendedAsrMethod)
                _uiState.update { state ->
                    state.copy(
                        selectedLocation = detectedCity,
                        calculationMethod = detectedCity.recommendedMethod,
                        asrMethod = detectedCity.recommendedAsrMethod,
                        isDetectingLocation = false,
                        locationErrorMessage = null,
                        isLocationPermissionGranted = true
                    )
                }
                rescheduleAlarms()
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isDetectingLocation = false,
                        locationErrorMessage = error.message ?: "Unable to fetch location"
                    )
                }
            }
        }
    }

    fun dismissLocationError() {
        _uiState.update { it.copy(locationErrorMessage = null) }
    }

    fun previousMonth() {
        _uiState.update { state ->
            state.copy(currentMonth = state.currentMonth.minusMonths(1))
        }
    }

    fun nextMonth() {
        _uiState.update { state ->
            state.copy(currentMonth = state.currentMonth.plusMonths(1))
        }
    }

    fun goToToday() {
        val today = LocalDate.now()
        selectDate(today)
    }

    fun resetSelectedDatePrayers() {
        val date = _uiState.value.selectedDate
        repository.clearDatePrayers(date)
        _uiState.update { state ->
            state.copy(
                selectedDatePrayers = emptyMap(),
                datesWithCompletedPrayers = repository.getDatesWithCompletedPrayers()
            )
        }
    }

    fun clearAllUserDataAndPhotos() {
        val photosToDelete = repository.getAllJournalRecords().mapNotNull { it.imagePath }
        repository.clearAllPreferences()
        val today = LocalDate.now()
        _uiState.update {
            PrayerUiState(
                selectedDate = today,
                currentMonth = YearMonth.now(),
                currentScreen = AppScreen.HOME,
                selectedLocation = CityCatalog.DEFAULT_CITY,
                calculationMethod = CityCatalog.DEFAULT_CITY.recommendedMethod,
                asrMethod = CityCatalog.DEFAULT_CITY.recommendedAsrMethod,
                selectedDatePrayers = emptyMap(),
                datesWithCompletedPrayers = emptySet(),
                journalRecords = emptyList()
            )
        }
        rescheduleAlarms()
        viewModelScope.launch {
            photosToDelete.forEach { path ->
                PhotoStorageHelper.deleteLocalPhoto(path)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { state ->
            state.copy(currentScreen = screen)
        }
    }

    fun handleBackNavigation(): Boolean {
        if (_uiState.value.isScanNamazDialogOpen) {
            closeScanNamazDialog()
            return true
        }
        if (_uiState.value.currentScreen != AppScreen.HOME) {
            _uiState.update { it.copy(currentScreen = AppScreen.HOME) }
            return true
        }
        return false
    }

    // --- Part 4 & 5: Scan Namaz & Prayer Photo Journal Actions ---

    fun openScanNamazDialog(
        initialPrayer: PrayerType? = null,
        initialDate: LocalDate? = null,
        existingRecord: JournalRecord? = null
    ) {
        _uiState.update { state ->
            state.copy(
                isScanNamazDialogOpen = true,
                scanNamazInitialPrayer = existingRecord?.prayerType ?: initialPrayer ?: PrayerType.FAJR,
                scanNamazInitialDate = existingRecord?.date ?: initialDate ?: state.selectedDate,
                activeEditingRecord = existingRecord
            )
        }
    }

    fun closeScanNamazDialog() {
        _uiState.update { state ->
            state.copy(
                isScanNamazDialogOpen = false,
                activeEditingRecord = null
            )
        }
    }

    fun saveJournalRecord(
        prayerType: PrayerType,
        date: LocalDate,
        imagePath: String?,
        note: String,
        existingId: String? = null
    ) {
        val record = JournalRecord(
            id = existingId ?: java.util.UUID.randomUUID().toString(),
            prayerType = prayerType,
            date = date,
            imagePath = imagePath,
            note = note,
            timestampMillis = System.currentTimeMillis()
        )

        repository.saveJournalRecord(record)

        val updatedJournals = repository.getAllJournalRecords()
        val updatedPrayers = repository.getAllPrayersForDate(_uiState.value.selectedDate)

        _uiState.update { state ->
            state.copy(
                journalRecords = updatedJournals,
                selectedDatePrayers = if (state.selectedDate == date) updatedPrayers else state.selectedDatePrayers,
                datesWithCompletedPrayers = repository.getDatesWithCompletedPrayers(),
                isScanNamazDialogOpen = false,
                activeEditingRecord = null
            )
        }
    }

    fun deleteJournalRecord(record: JournalRecord) {
        repository.deleteJournalRecord(record.id)
        val updatedJournals = repository.getAllJournalRecords()
        _uiState.update { state ->
            state.copy(journalRecords = updatedJournals)
        }
        viewModelScope.launch {
            PhotoStorageHelper.deleteLocalPhoto(record.imagePath)
        }
    }

    fun checkExistingRecordFor(date: LocalDate, prayerType: PrayerType): JournalRecord? {
        return repository.getJournalRecordFor(date, prayerType)
    }
}
