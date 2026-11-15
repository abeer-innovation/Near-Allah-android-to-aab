package com.example.data.model

enum class AlarmSoundMode(
    val title: String,
    val description: String
) {
    ADHAN_TONE(
        title = "Adhan / Prayer Alert Tone",
        description = "Plays a clear devotional notification tone when prayer time arrives."
    ),
    GENTLE_CHIME(
        title = "Gentle Chime",
        description = "Plays a soft chime alert at prayer time."
    ),
    SILENT(
        title = "Silent Notification",
        description = "Delivers a silent visual notification without playing audio."
    )
}
