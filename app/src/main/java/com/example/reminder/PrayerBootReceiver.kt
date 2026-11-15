package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.model.PrayerType
import com.example.data.repository.PrayerPreferencesRepository

class PrayerBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET"
        ) {
            val repository = PrayerPreferencesRepository(context)
            val location = repository.getSavedLocation()
            val calcMethod = repository.getSavedCalculationMethod()
            val asrMethod = repository.getSavedAsrMethod()

            val enabledPrayers = PrayerType.values().associateWith { type ->
                repository.isReminderEnabled(type)
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
        }
    }
}
