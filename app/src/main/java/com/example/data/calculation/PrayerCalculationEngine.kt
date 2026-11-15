package com.example.data.calculation

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/**
 * Supported Prayer Calculation Conventions.
 */
enum class CalculationMethod(
    val title: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null,
    val maghribAngle: Double? = null
) {
    MUSLIM_WORLD_LEAGUE(
        title = "Muslim World League (MWL)",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    ISNA(
        title = "Islamic Society of North America (ISNA)",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    UMM_AL_QURA(
        title = "Umm al-Qura University, Makkah",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    EGYPTIAN(
        title = "Egyptian General Authority of Survey",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    KARACHI(
        title = "Univ. of Islamic Sciences, Karachi",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    DUBAI(
        title = "Dubai / UAE General Authority",
        fajrAngle = 18.2,
        ishaAngle = 18.2
    ),
    KUWAIT(
        title = "Kuwait Ministry of Awqaf",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),
    QATAR(
        title = "Qatar Awqaf",
        fajrAngle = 18.0,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    SINGAPORE(
        title = "MUIS Singapore",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    )
}

/**
 * Asr Juristic Method (Madhab)
 */
enum class AsrMethod(
    val title: String,
    val shadowFactor: Double
) {
    STANDARD(
        title = "Standard (Shafi'i, Maliki, Hanbali)",
        shadowFactor = 1.0
    ),
    HANAFI(
        title = "Hanafi",
        shadowFactor = 2.0
    )
}

data class CalculatedPrayerTimes(
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val date: LocalDate,
    val formattedFajr: String,
    val formattedSunrise: String,
    val formattedDhuhr: String,
    val formattedAsr: String,
    val formattedMaghrib: String,
    val formattedIsha: String
)

/**
 * Astronomical Prayer Time Calculation Engine.
 * Implements mathematically authentic solar formulas based on Julian Day,
 * Solar Declination, Equation of Time, and Juristic Shadow Angles.
 */
object PrayerCalculationEngine {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        asrMethod: AsrMethod = AsrMethod.STANDARD
    ): CalculatedPrayerTimes {
        // Calculate timezone offset in hours on this date at solar noon to avoid DST transition errors
        val middayInstant = date.atTime(12, 0).atZone(zoneId).toInstant()
        val timezoneOffsetHours = zoneId.rules.getOffset(middayInstant).totalSeconds / 3600.0

        // Julian Day Number at local noon
        val julianDay = toJulianDay(date.year, date.monthValue, date.dayOfMonth) - (longitude / (15.0 * 24.0))

        // Sun calculations
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(toDegrees(atan2(cos(toRadians(e)) * sin(toRadians(l)), cos(toRadians(l))))) / 15.0
        var equationOfTime = (q / 15.0) - fixHour(ra)
        // Normalize Equation of Time to [-12.0, 12.0] hours to prevent 24-hour wraparound shifts
        if (equationOfTime > 12.0) equationOfTime -= 24.0
        if (equationOfTime < -12.0) equationOfTime += 24.0

        val declination = toDegrees(asin(sin(toRadians(e)) * sin(toRadians(l))))

        // Solar Noon (Midday)
        val solarNoon = fixHour(12.0 + timezoneOffsetHours - (longitude / 15.0) - equationOfTime)

        // Dhuhr (Zuhr): 2.0 min safety buffer after solar noon to ensure sun has passed the zenith (zawwal)
        val dhuhrHours = solarNoon + (2.0 / 60.0)

        // Sunrise & Sunset (Standard atmospheric refraction angle = -0.8333°)
        val sunriseHourAngle = calculateHourAngle(-0.8333, latitude, declination)
        val sunriseHours = solarNoon - sunriseHourAngle
        val sunsetHours = solarNoon + sunriseHourAngle

        // Fajr (with robust high-latitude safe angle fallback)
        val fajrHourAngle = calculateTwilightHourAngle(-method.fajrAngle, latitude, declination, sunriseHourAngle)
        val fajrHours = solarNoon - fajrHourAngle

        // Asr (shadow factor depends on Juristic method)
        val asrAltitude = toDegrees(
            atan(1.0 / (asrMethod.shadowFactor + tan(toRadians(abs(latitude - declination)))))
        )
        val asrHourAngle = calculateHourAngle(asrAltitude, latitude, declination)
        val asrHours = solarNoon + asrHourAngle

        // Maghrib (sunset + 2 min buffer for complete disappearance of solar disc)
        val maghribHours = if (method.maghribAngle != null) {
            val maghribHourAngle = calculateHourAngle(-method.maghribAngle, latitude, declination)
            solarNoon + maghribHourAngle
        } else {
            sunsetHours + (2.0 / 60.0)
        }

        // Isha
        val ishaHours = if (method.ishaIntervalMinutes != null) {
            maghribHours + (method.ishaIntervalMinutes / 60.0)
        } else {
            val ishaHourAngle = calculateTwilightHourAngle(-method.ishaAngle, latitude, declination, sunriseHourAngle)
            solarNoon + ishaHourAngle
        }

        val fajrTime = hoursToLocalTime(fajrHours)
        val sunriseTime = hoursToLocalTime(sunriseHours)
        val dhuhrTime = hoursToLocalTime(dhuhrHours)
        val asrTime = hoursToLocalTime(asrHours)
        val maghribTime = hoursToLocalTime(maghribHours)
        val ishaTime = hoursToLocalTime(ishaHours)

        return CalculatedPrayerTimes(
            fajr = fajrTime,
            sunrise = sunriseTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            maghrib = maghribTime,
            isha = ishaTime,
            date = date,
            formattedFajr = fajrTime.format(timeFormatter),
            formattedSunrise = sunriseTime.format(timeFormatter),
            formattedDhuhr = dhuhrTime.format(timeFormatter),
            formattedAsr = asrTime.format(timeFormatter),
            formattedMaghrib = maghribTime.format(timeFormatter),
            formattedIsha = ishaTime.format(timeFormatter)
        )
    }

    private fun calculateHourAngle(altitude: Double, latitude: Double, declination: Double): Double {
        val latRad = toRadians(latitude)
        val decRad = toRadians(declination)
        val altRad = toRadians(altitude)

        val cosHourAngle = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clampedCos = cosHourAngle.coerceIn(-1.0, 1.0)
        return toDegrees(acos(clampedCos)) / 15.0
    }

    private fun calculateTwilightHourAngle(
        altitude: Double,
        latitude: Double,
        declination: Double,
        sunriseHourAngle: Double
    ): Double {
        val latRad = toRadians(latitude)
        val decRad = toRadians(declination)
        val altRad = toRadians(altitude)

        val cosHourAngle = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        return if (cosHourAngle in -1.0..1.0) {
            toDegrees(acos(cosHourAngle)) / 15.0
        } else {
            // High-latitude fallback (1/7th of night portion rule)
            val nightLength = max(2.0, 24.0 - (2.0 * sunriseHourAngle))
            val portion = nightLength / 7.0
            sunriseHourAngle + portion
        }
    }

    private fun toJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun hoursToLocalTime(hoursValue: Double): LocalTime {
        val fixed = fixHour(hoursValue)
        // Standard astronomical rounding: round to nearest minute (with 30-second boundary)
        // rather than truncating seconds, preventing prayer times from showing 1 minute too early
        val totalSeconds = (fixed * 3600.0).roundToInt()
        val totalMinutes = (totalSeconds + 30) / 60
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return LocalTime.of(hours, minutes, 0)
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun toRadians(degrees: Double): Double = Math.toRadians(degrees)
    private fun toDegrees(radians: Double): Double = Math.toDegrees(radians)
}
