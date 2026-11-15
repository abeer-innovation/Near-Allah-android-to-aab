package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.AlarmSoundMode
import java.time.LocalDate

class PrayerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val arabicName = intent.getStringExtra(EXTRA_ARABIC_NAME) ?: ""
        val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "your location"
        val prayerOrder = intent.getIntExtra(EXTRA_PRAYER_ORDER, 1)
        val soundModeName = intent.getStringExtra(EXTRA_SOUND_MODE) ?: AlarmSoundMode.ADHAN_TONE.name
        val dateStr = intent.getStringExtra(EXTRA_DATE) ?: LocalDate.now().toString()
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        createNotificationChannels(context)

        // Start Foreground Alarm Service for 5-minute audible alarm & full screen interface
        val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
            action = PrayerAlarmService.ACTION_START_ALARM
            putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmService.EXTRA_ARABIC_NAME, arabicName)
            putExtra(PrayerAlarmService.EXTRA_CITY_NAME, cityName)
            putExtra(PrayerAlarmService.EXTRA_PRAYER_ORDER, prayerOrder)
            putExtra(PrayerAlarmService.EXTRA_SOUND_MODE, soundModeName)
            putExtra(PrayerAlarmService.EXTRA_DATE, dateStr)
            putExtra(PrayerAlarmService.EXTRA_IS_TEST, isTest)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) { }

        // Automatically reschedule next alarms to ensure prayers continue sounding on subsequent days even when app is closed
        if (!isTest) {
            try {
                val repository = com.example.data.repository.PrayerPreferencesRepository(context)
                val location = repository.getSavedLocation()
                val calcMethod = repository.getSavedCalculationMethod()
                val asrMethod = repository.getSavedAsrMethod()
                val enabledPrayers = com.example.data.model.PrayerType.values().associateWith {
                    repository.isReminderEnabled(it)
                }
                val soundMode = repository.getAlarmSoundMode()
                PrayerReminderManager.updateAllReminders(
                    context = context,
                    location = location,
                    calculationMethod = calcMethod,
                    asrMethod = asrMethod,
                    enabledPrayers = enabledPrayers,
                    soundMode = soundMode
                )
            } catch (_: Exception) { }
        }
    }

    companion object {
        const val CHANNEL_ID_SOUND = "near_allah_prayer_sound_channel"
        const val CHANNEL_NAME_SOUND = "Prayer Alarms (Sound & Vibration)"

        const val CHANNEL_ID_SILENT = "near_allah_prayer_silent_channel"
        const val CHANNEL_NAME_SILENT = "Prayer Reminders (Silent)"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_ARABIC_NAME = "extra_arabic_name"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_PRAYER_ORDER = "extra_prayer_order"
        const val EXTRA_SOUND_MODE = "extra_sound_mode"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_IS_TEST = "extra_is_test"
        const val NOTIFICATION_ID_OFFSET = 7000

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                // Sound Channel
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()

                val soundChannel = NotificationChannel(
                    CHANNEL_ID_SOUND,
                    CHANNEL_NAME_SOUND,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Audible alerts when it is time for obligatory prayers."
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 250, 400)
                    setSound(soundUri, audioAttributes)
                }

                // Silent Channel
                val silentChannel = NotificationChannel(
                    CHANNEL_ID_SILENT,
                    CHANNEL_NAME_SILENT,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silent visual alerts for prayer times without audio."
                    enableVibration(false)
                    setSound(null, null)
                }

                notificationManager.createNotificationChannel(soundChannel)
                notificationManager.createNotificationChannel(silentChannel)
            }
        }
    }
}
