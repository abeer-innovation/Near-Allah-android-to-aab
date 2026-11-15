package com.example.data.model

import java.time.LocalDate
import java.util.UUID

data class JournalRecord(
    val id: String = UUID.randomUUID().toString(),
    val prayerType: PrayerType,
    val date: LocalDate,
    val imagePath: String? = null,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)
