package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlarmSoundMode
import com.example.data.repository.PrayerPreferencesRepository
import java.time.LocalDate

class PrayerAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    private var currentPrayerName: String = "Prayer"
    private var currentArabicName: String = ""
    private var currentCityName: String = "your location"
    private var currentPrayerOrder: Int = 1
    private var currentSoundMode: AlarmSoundMode = AlarmSoundMode.ADHAN_TONE
    private var currentDateStr: String = ""
    private var isTestRun: Boolean = false

    private val autoStopRunnable = Runnable {
        stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NearAllah:PrayerAlarmWakeLock"
            )?.apply {
                acquire(AUTO_STOP_DURATION_MS + 10_000L) // 5 minutes + safety margin
            }
        } catch (_: Exception) { }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent.action
        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_ALARM) {
            currentPrayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
            currentArabicName = intent.getStringExtra(EXTRA_ARABIC_NAME) ?: ""
            currentCityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "your location"
            currentPrayerOrder = intent.getIntExtra(EXTRA_PRAYER_ORDER, 1)
            val soundModeName = intent.getStringExtra(EXTRA_SOUND_MODE) ?: AlarmSoundMode.ADHAN_TONE.name
            currentSoundMode = try {
                AlarmSoundMode.valueOf(soundModeName)
            } catch (_: Exception) {
                AlarmSoundMode.ADHAN_TONE
            }
            currentDateStr = intent.getStringExtra(EXTRA_DATE) ?: LocalDate.now().toString()
            isTestRun = intent.getBooleanExtra(EXTRA_IS_TEST, false)

            // Deduplication: prevent repeated alarms for the same prayer and date if already stopped today
            val repository = PrayerPreferencesRepository(applicationContext)
            if (!isTestRun && repository.isAlarmStoppedFor(currentDateStr, currentPrayerOrder)) {
                stopSelf()
                return START_NOT_STICKY
            }

            startAlarm()
        }

        return START_NOT_STICKY
    }

    private fun startAlarm() {
        // Stop any previous media player instance to prevent overlapping playback
        stopAudioPlayback()

        isAlarmActive = true
        activePrayerName = currentPrayerName
        activeArabicName = currentArabicName
        activeCityName = currentCityName
        activePrayerOrder = currentPrayerOrder
        alarmStartTimeMillis = System.currentTimeMillis()

        // 1. Post Foreground Notification with Full-Screen Intent and STOP ALARM action
        createAlarmNotificationChannel()
        val notification = buildAlarmNotification()
        startForeground(NOTIFICATION_ID_OFFSET + currentPrayerOrder, notification)

        // 2. Launch Full-Screen Alarm Activity where allowed by OS
        try {
            val fullScreenIntent = Intent(this, PrayerAlarmActivity::class.java).apply {
                putExtra(EXTRA_PRAYER_NAME, currentPrayerName)
                putExtra(EXTRA_ARABIC_NAME, currentArabicName)
                putExtra(EXTRA_CITY_NAME, currentCityName)
                putExtra(EXTRA_PRAYER_ORDER, currentPrayerOrder)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(fullScreenIntent)
        } catch (_: Exception) {
            // Android 10+ background activity launch restrictions: handled via full-screen intent in notification
        }

        // 3. Start Audible Alarm Sound (continuous looping up to 5 minutes)
        if (currentSoundMode != AlarmSoundMode.SILENT) {
            playAlarmSound()
        }

        // 4. Start Repeating Vibration
        startVibration()

        // 5. Schedule Automatic Shutdown after exactly 5 minutes (300,000 ms)
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, AUTO_STOP_DURATION_MS)
    }

    private fun playAlarmSound() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // Respect device silent/vibrate mode if configured as silent or non-adhan
            if (audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT && currentSoundMode == AlarmSoundMode.SILENT) {
                return
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(applicationContext, soundUri)
                isLooping = true // Continuous sounding until stopped or 5 minutes elapse
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Fallback: ringtone may not be accessible, notification sound handles alert
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 800, 600, 800, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat continuously
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) { }
    }

    private fun stopAudioPlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (_: Exception) { } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (_: Exception) { }
    }

    fun stopAlarm() {
        handler.removeCallbacks(autoStopRunnable)
        stopAudioPlayback()

        // Mark this alarm as stopped for today so it will not start again today
        if (!isTestRun && currentDateStr.isNotBlank()) {
            val repository = PrayerPreferencesRepository(applicationContext)
            repository.markAlarmStopped(currentDateStr, currentPrayerOrder)
        }

        isAlarmActive = false
        alarmStartTimeMillis = 0L

        // Notify active activity or listeners to dismiss
        onAlarmStoppedListener?.invoke()

        val broadcast = Intent(ACTION_ALARM_STOPPED_BROADCAST)
        sendBroadcast(broadcast)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildAlarmNotification(): android.app.Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            currentPrayerOrder,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full Screen PendingIntent to PrayerAlarmActivity
        val fullScreenIntent = Intent(this, PrayerAlarmActivity::class.java).apply {
            putExtra(EXTRA_PRAYER_NAME, currentPrayerName)
            putExtra(EXTRA_ARABIC_NAME, currentArabicName)
            putExtra(EXTRA_CITY_NAME, currentCityName)
            putExtra(EXTRA_PRAYER_ORDER, currentPrayerOrder)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            2000 + currentPrayerOrder,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Immediate STOP ALARM Action
        val stopServiceIntent = Intent(this, PrayerAlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1000 + currentPrayerOrder,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (currentArabicName.isNotBlank()) {
            "Time for $currentPrayerName ($currentArabicName)"
        } else {
            "Time for $currentPrayerName"
        }
        val content = "Alarm sounding for $currentCityName • Auto-stops in 5 minutes"

        return NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content. Tap to view full screen or press STOP ALARM."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(0xFF4A7258.toInt()) // Sage green
            .setContentIntent(mainPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "STOP ALARM",
                stopPendingIntent
            )
            .build()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                CHANNEL_NAME_ALARM,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent audible prayer alarms with 5-minute continuous alert and full-screen dismissal."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 600, 800, 600)
                setSound(null, null) // Audio is managed directly by MediaPlayer for precise 5-minute looping
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoStopRunnable)
        stopAudioPlayback()
        isAlarmActive = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) { }
    }

    companion object {
        const val ACTION_START_ALARM = "com.example.reminder.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.reminder.ACTION_STOP_ALARM"
        const val ACTION_ALARM_STOPPED_BROADCAST = "com.example.reminder.ACTION_ALARM_STOPPED_BROADCAST"

        const val CHANNEL_ID_ALARM = "near_allah_prayer_alarm_channel"
        const val CHANNEL_NAME_ALARM = "Prayer Alarms (Full 5-Minute Alert)"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_ARABIC_NAME = "extra_arabic_name"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_PRAYER_ORDER = "extra_prayer_order"
        const val EXTRA_SOUND_MODE = "extra_sound_mode"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_IS_TEST = "extra_is_test"

        const val NOTIFICATION_ID_OFFSET = 8000
        const val AUTO_STOP_DURATION_MS = 5 * 60 * 1000L // Exactly 5 minutes

        // Live static state accessible by PrayerAlarmActivity
        var isAlarmActive: Boolean = false
            private set
        var activePrayerName: String = ""
            private set
        var activeArabicName: String = ""
            private set
        var activeCityName: String = ""
            private set
        var activePrayerOrder: Int = 1
            private set
        var alarmStartTimeMillis: Long = 0L
            private set

        var onAlarmStoppedListener: (() -> Unit)? = null

        fun stopAlarm(context: Context) {
            val stopIntent = Intent(context, PrayerAlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(stopIntent)
        }
    }
}
