package com.example.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.calculation.AsrMethod
import com.example.data.calculation.CalculatedPrayerTimes
import com.example.data.calculation.CalculationMethod
import com.example.data.calculation.PrayerCalculationEngine
import com.example.data.location.CityLocation
import com.example.data.model.AlarmSoundMode
import com.example.data.model.PrayerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object PrayerReminderManager {

    private const val TAG_VERIFICATION = "PrayerAlarmVerification"

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() == true
        }
        return true
    }

    /**
     * Primary scheduling method that calculates astronomical prayer times for the location's
     * today and tomorrow dates, dynamically handling whether each prayer has elapsed today.
     */
    fun updateAllReminders(
        context: Context,
        location: CityLocation,
        calculationMethod: CalculationMethod,
        asrMethod: AsrMethod,
        enabledPrayers: Map<PrayerType, Boolean>,
        soundMode: AlarmSoundMode = AlarmSoundMode.ADHAN_TONE
    ) {
        PrayerReminderReceiver.createNotificationChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val locationZone = try {
            ZoneId.of(location.timezoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val today = LocalDate.now(locationZone)
        val tomorrow = today.plusDays(1)
        val now = ZonedDateTime.now(locationZone)

        val todayTimes = PrayerCalculationEngine.calculate(
            date = today,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = locationZone,
            method = calculationMethod,
            asrMethod = asrMethod
        )

        val tomorrowTimes = PrayerCalculationEngine.calculate(
            date = tomorrow,
            latitude = location.latitude,
            longitude = location.longitude,
            zoneId = locationZone,
            method = calculationMethod,
            asrMethod = asrMethod
        )

        PrayerType.values().forEach { prayerType ->
            val isEnabled = enabledPrayers[prayerType] ?: false
            if (!isEnabled) {
                cancelPrayerAlarm(context, alarmManager, prayerType)
                return@forEach
            }

            val todayPrayerTime = when (prayerType) {
                PrayerType.FAJR -> todayTimes.fajr
                PrayerType.DHUHR -> todayTimes.dhuhr
                PrayerType.ASR -> todayTimes.asr
                PrayerType.MAGHRIB -> todayTimes.maghrib
                PrayerType.ISHA -> todayTimes.isha
            }

            val tomorrowPrayerTime = when (prayerType) {
                PrayerType.FAJR -> tomorrowTimes.fajr
                PrayerType.DHUHR -> tomorrowTimes.dhuhr
                PrayerType.ASR -> tomorrowTimes.asr
                PrayerType.MAGHRIB -> tomorrowTimes.maghrib
                PrayerType.ISHA -> tomorrowTimes.isha
            }

            val todayDateTime = today.atTime(todayPrayerTime).atZone(locationZone)
            val (targetDateTime, targetPrayerTime) = if (todayDateTime.isAfter(now)) {
                todayDateTime to todayPrayerTime
            } else {
                tomorrow.atTime(tomorrowPrayerTime).atZone(locationZone) to tomorrowPrayerTime
            }

            // Always cancel previous alarm first to prevent duplicates
            cancelPrayerAlarm(context, alarmManager, prayerType)

            schedulePrayerAlarm(
                context = context,
                alarmManager = alarmManager,
                type = prayerType,
                targetDateTime = targetDateTime,
                calculatedPrayerTime = targetPrayerTime,
                cityName = location.name,
                soundMode = soundMode,
                timezoneId = location.timezoneId
            )
        }
    }

    /**
     * Overloaded method for backward compatibility and manual updates.
     */
    fun updateAllReminders(
        context: Context,
        times: CalculatedPrayerTimes,
        cityName: String,
        enabledPrayers: Map<PrayerType, Boolean>,
        soundMode: AlarmSoundMode = AlarmSoundMode.ADHAN_TONE,
        timezoneId: String = "Asia/Riyadh"
    ) {
        PrayerReminderReceiver.createNotificationChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val locationZone = try {
            ZoneId.of(timezoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val now = ZonedDateTime.now(locationZone)

        PrayerType.values().forEach { prayerType ->
            val isEnabled = enabledPrayers[prayerType] ?: false
            if (!isEnabled) {
                cancelPrayerAlarm(context, alarmManager, prayerType)
                return@forEach
            }

            val prayerTime = when (prayerType) {
                PrayerType.FAJR -> times.fajr
                PrayerType.DHUHR -> times.dhuhr
                PrayerType.ASR -> times.asr
                PrayerType.MAGHRIB -> times.maghrib
                PrayerType.ISHA -> times.isha
            }

            var targetDateTime = now.with(prayerTime)
            if (targetDateTime.isBefore(now)) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            cancelPrayerAlarm(context, alarmManager, prayerType)

            schedulePrayerAlarm(
                context = context,
                alarmManager = alarmManager,
                type = prayerType,
                targetDateTime = targetDateTime,
                calculatedPrayerTime = prayerTime,
                cityName = cityName,
                soundMode = soundMode,
                timezoneId = timezoneId
            )
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun schedulePrayerAlarm(
        context: Context,
        alarmManager: AlarmManager,
        type: PrayerType,
        targetDateTime: ZonedDateTime,
        calculatedPrayerTime: LocalTime,
        cityName: String,
        soundMode: AlarmSoundMode,
        timezoneId: String
    ) {
        val triggerMillis = targetDateTime.toInstant().toEpochMilli()
        val targetDate = targetDateTime.toLocalDate().toString()
        val scheduledAlarmTime = targetDateTime.toLocalTime().toString()

        // Required verification debug log:
        // Prayer Name | Location | Date | Timezone | Calculated Prayer Time | Scheduled Alarm Time
        Log.d(
            TAG_VERIFICATION,
            "${type.displayName} | $cityName | $targetDate | $timezoneId | $calculatedPrayerTime | $scheduledAlarmTime"
        )

        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_NAME, type.displayName)
            putExtra(PrayerReminderReceiver.EXTRA_ARABIC_NAME, type.arabicName)
            putExtra(PrayerReminderReceiver.EXTRA_CITY_NAME, cityName)
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_ORDER, type.order)
            putExtra(PrayerReminderReceiver.EXTRA_SOUND_MODE, soundMode.name)
            putExtra(PrayerReminderReceiver.EXTRA_DATE, targetDate)
            putExtra(PrayerReminderReceiver.EXTRA_IS_TEST, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.order,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Graceful fallback if exact alarms are disallowed by OS/manufacturer
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (_: Exception) { }
        }
    }

    fun cancelPrayerAlarm(
        context: Context,
        alarmManager: AlarmManager,
        type: PrayerType
    ) {
        val intent = Intent(context, PrayerReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.order,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Triggers an immediate 5-minute audible alarm and full-screen UI for real testing
     */
    fun sendImmediateTestAlarm(
        context: Context,
        type: PrayerType = PrayerType.FAJR,
        cityName: String = "your location",
        soundMode: AlarmSoundMode = AlarmSoundMode.ADHAN_TONE
    ) {
        PrayerReminderReceiver.createNotificationChannels(context)
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_NAME, type.displayName)
            putExtra(PrayerReminderReceiver.EXTRA_ARABIC_NAME, type.arabicName)
            putExtra(PrayerReminderReceiver.EXTRA_CITY_NAME, cityName)
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_ORDER, type.order)
            putExtra(PrayerReminderReceiver.EXTRA_SOUND_MODE, soundMode.name)
            putExtra(PrayerReminderReceiver.EXTRA_DATE, LocalDate.now().toString())
            putExtra(PrayerReminderReceiver.EXTRA_IS_TEST, true)
        }
        context.sendBroadcast(intent)
    }

    fun sendImmediateTestNotification(
        context: Context,
        type: PrayerType = PrayerType.FAJR,
        cityName: String = "your location",
        soundMode: AlarmSoundMode = AlarmSoundMode.ADHAN_TONE
    ) {
        sendImmediateTestAlarm(context, type, cityName, soundMode)
    }
}
