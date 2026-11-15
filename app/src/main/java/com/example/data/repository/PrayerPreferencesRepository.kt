package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.calculation.AsrMethod
import com.example.data.calculation.CalculationMethod
import com.example.data.location.CityCatalog
import com.example.data.location.CityLocation
import com.example.data.model.JournalRecord
import com.example.data.model.PrayerType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class PrayerPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "near_allah_preferences",
        Context.MODE_PRIVATE
    )

    // --- Prayer Completion Persistence by Date ---

    fun getPrayerStatus(date: LocalDate, prayerType: PrayerType): Boolean {
        return prefs.getBoolean(getCompletionKey(date, prayerType), false)
    }

    fun setPrayerStatus(date: LocalDate, prayerType: PrayerType, isCompleted: Boolean) {
        prefs.edit().putBoolean(getCompletionKey(date, prayerType), isCompleted).apply()
        // Record date in tracked dates set if completed
        val trackedDates = getTrackedDates().toMutableSet()
        val dateString = date.toString()
        if (isCompleted) {
            trackedDates.add(dateString)
        } else {
            // Check if any prayer is still completed for this date
            val hasAny = PrayerType.values().any { type ->
                if (type == prayerType) false else getPrayerStatus(date, type)
            }
            if (!hasAny) {
                trackedDates.remove(dateString)
            }
        }
        prefs.edit().putStringSet(KEY_TRACKED_DATES, HashSet(trackedDates)).apply()
    }

    fun getAllPrayersForDate(date: LocalDate): Map<PrayerType, Boolean> {
        return PrayerType.values().associateWith { type ->
            getPrayerStatus(date, type)
        }
    }

    fun clearDatePrayers(date: LocalDate) {
        val editor = prefs.edit()
        PrayerType.values().forEach { type ->
            editor.remove(getCompletionKey(date, type))
        }
        val trackedDates = getTrackedDates().toMutableSet()
        trackedDates.remove(date.toString())
        editor.putStringSet(KEY_TRACKED_DATES, trackedDates).apply()
    }

    fun getDatesWithCompletedPrayers(): Set<LocalDate> {
        val strings = getTrackedDates()
        val result = mutableSetOf<LocalDate>()
        for (str in strings) {
            try {
                result.add(LocalDate.parse(str))
            } catch (_: Exception) { }
        }
        return result
    }

    private fun getTrackedDates(): Set<String> {
        return prefs.getStringSet(KEY_TRACKED_DATES, emptySet()) ?: emptySet()
    }

    private fun getCompletionKey(date: LocalDate, type: PrayerType): String {
        return "prayer_completion_${date}_${type.name}"
    }

    // --- Prayer Photo Journal Records Persistence ---

    fun getAllJournalRecords(): List<JournalRecord> {
        val jsonString = prefs.getString(KEY_JOURNAL_RECORDS, null) ?: return emptyList()
        val records = mutableListOf<JournalRecord>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val prayerTypeName = obj.getString("prayerType")
                val dateStr = obj.getString("date")
                val imagePath = if (obj.has("imagePath") && !obj.isNull("imagePath")) obj.getString("imagePath") else null
                val note = if (obj.has("note")) obj.getString("note") else ""
                val timestampMillis = if (obj.has("timestampMillis")) obj.getLong("timestampMillis") else System.currentTimeMillis()

                val prayerType = try {
                    PrayerType.valueOf(prayerTypeName)
                } catch (_: Exception) {
                    PrayerType.FAJR
                }
                val date = try {
                    LocalDate.parse(dateStr)
                } catch (_: Exception) {
                    LocalDate.now()
                }

                records.add(
                    JournalRecord(
                        id = id,
                        prayerType = prayerType,
                        date = date,
                        imagePath = imagePath,
                        note = note,
                        timestampMillis = timestampMillis
                    )
                )
            }
        } catch (_: Exception) { }
        return records.sortedByDescending { it.timestampMillis }
    }

    fun saveJournalRecord(record: JournalRecord) {
        val current = getAllJournalRecords().toMutableList()
        // If an entry with the same ID exists, update it; otherwise add it
        val index = current.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            current[index] = record
        } else {
            current.add(0, record)
        }
        persistJournalRecords(current)
        // Ensure the prayer status is synchronized to completed
        setPrayerStatus(record.date, record.prayerType, true)
    }

    fun deleteJournalRecord(id: String): JournalRecord? {
        val current = getAllJournalRecords().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val removed = current.removeAt(index)
            persistJournalRecords(current)
            return removed
        }
        return null
    }

    fun getJournalRecordFor(date: LocalDate, prayerType: PrayerType): JournalRecord? {
        return getAllJournalRecords().firstOrNull { it.date == date && it.prayerType == prayerType }
    }

    private fun persistJournalRecords(records: List<JournalRecord>) {
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("prayerType", record.prayerType.name)
                put("date", record.date.toString())
                put("imagePath", record.imagePath ?: JSONObject.NULL)
                put("note", record.note)
                put("timestampMillis", record.timestampMillis)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_JOURNAL_RECORDS, jsonArray.toString()).apply()
    }

    // --- Reminder Configuration Persistence ---

    fun isReminderEnabled(type: PrayerType): Boolean {
        return prefs.getBoolean("reminder_enabled_${type.name}", true)
    }

    fun setReminderEnabled(type: PrayerType, enabled: Boolean) {
        prefs.edit().putBoolean("reminder_enabled_${type.name}", enabled).apply()
    }

    fun areRemindersMasterEnabled(): Boolean {
        return prefs.getBoolean(KEY_MASTER_REMINDERS, true)
    }

    fun setMasterRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_REMINDERS, enabled).apply()
    }

    // --- Location & Settings Persistence ---

    fun saveSelectedLocation(city: CityLocation) {
        prefs.edit()
            .putString("loc_name", city.name)
            .putString("loc_country", city.country)
            .putString("loc_lat", city.latitude.toString())
            .putString("loc_lng", city.longitude.toString())
            .putString("loc_timezone", city.timezoneId)
            .putString("loc_method", city.recommendedMethod.name)
            .putString("loc_asr_method", city.recommendedAsrMethod.name)
            .apply()
    }

    fun getSavedLocation(): CityLocation {
        val name = prefs.getString("loc_name", null) ?: return CityCatalog.DEFAULT_CITY
        val country = prefs.getString("loc_country", "") ?: ""
        val lat = prefs.getString("loc_lat", null)?.toDoubleOrNull() ?: CityCatalog.DEFAULT_CITY.latitude
        val lng = prefs.getString("loc_lng", null)?.toDoubleOrNull() ?: CityCatalog.DEFAULT_CITY.longitude
        val timezone = prefs.getString("loc_timezone", CityCatalog.DEFAULT_CITY.timezoneId) ?: CityCatalog.DEFAULT_CITY.timezoneId
        val methodStr = prefs.getString("loc_method", CalculationMethod.KARACHI.name)
        val method = try {
            CalculationMethod.valueOf(methodStr ?: CalculationMethod.KARACHI.name)
        } catch (_: Exception) {
            CalculationMethod.KARACHI
        }
        val asrStr = prefs.getString("loc_asr_method", AsrMethod.HANAFI.name)
        val asrMethod = try {
            AsrMethod.valueOf(asrStr ?: AsrMethod.HANAFI.name)
        } catch (_: Exception) {
            AsrMethod.HANAFI
        }

        return CityLocation(
            name = name,
            country = country,
            latitude = lat,
            longitude = lng,
            timezoneId = timezone,
            recommendedMethod = method,
            recommendedAsrMethod = asrMethod
        )
    }

    fun saveCalculationMethod(method: CalculationMethod) {
        prefs.edit().putString("calc_method", method.name).apply()
    }

    fun getSavedCalculationMethod(): CalculationMethod {
        val name = prefs.getString("calc_method", null) ?: return getSavedLocation().recommendedMethod
        return try {
            CalculationMethod.valueOf(name)
        } catch (_: Exception) {
            getSavedLocation().recommendedMethod
        }
    }

    fun saveAsrMethod(method: AsrMethod) {
        prefs.edit().putString("asr_method", method.name).apply()
    }

    fun getSavedAsrMethod(): AsrMethod {
        val name = prefs.getString("asr_method", null) ?: return getSavedLocation().recommendedAsrMethod
        return try {
            AsrMethod.valueOf(name)
        } catch (_: Exception) {
            getSavedLocation().recommendedAsrMethod
        }
    }

    // --- Part 5: Alarm Sound Mode & Privacy ---

    fun getAlarmSoundMode(): com.example.data.model.AlarmSoundMode {
        val name = prefs.getString(KEY_ALARM_SOUND_MODE, com.example.data.model.AlarmSoundMode.ADHAN_TONE.name)
        return try {
            com.example.data.model.AlarmSoundMode.valueOf(name ?: com.example.data.model.AlarmSoundMode.ADHAN_TONE.name)
        } catch (_: Exception) {
            com.example.data.model.AlarmSoundMode.ADHAN_TONE
        }
    }

    fun setAlarmSoundMode(mode: com.example.data.model.AlarmSoundMode) {
        prefs.edit().putString(KEY_ALARM_SOUND_MODE, mode.name).apply()
    }

    // --- Alarm Stopped State (Deduplication) ---

    fun markAlarmStopped(dateStr: String, prayerOrder: Int) {
        prefs.edit().putBoolean("alarm_stopped_${dateStr}_$prayerOrder", true).apply()
    }

    fun isAlarmStoppedFor(dateStr: String, prayerOrder: Int): Boolean {
        return prefs.getBoolean("alarm_stopped_${dateStr}_$prayerOrder", false)
    }

    fun isLocationUserSelected(): Boolean {
        return prefs.getBoolean("loc_user_selected", false)
    }

    fun setLocationUserSelected(selected: Boolean) {
        prefs.edit().putBoolean("loc_user_selected", selected).apply()
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }

    suspend fun clearAllUserData(context: Context) {
        // Delete all photo files
        val records = getAllJournalRecords()
        records.forEach { record ->
            PhotoStorageHelper.deleteLocalPhoto(record.imagePath)
        }
        // Clear all preferences
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TRACKED_DATES = "tracked_completed_dates"
        private const val KEY_MASTER_REMINDERS = "master_reminders_enabled"
        private const val KEY_JOURNAL_RECORDS = "prayer_journal_records_json"
        private const val KEY_ALARM_SOUND_MODE = "alarm_sound_mode"
    }
}
