package com.example.data.location

import com.example.data.calculation.AsrMethod
import com.example.data.calculation.CalculationMethod

data class CityLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val recommendedMethod: CalculationMethod = CalculationMethod.KARACHI,
    val recommendedAsrMethod: AsrMethod = AsrMethod.HANAFI
)

object CityCatalog {

    /**
     * Sensible default location for Pakistan:
     * Islamabad, Pakistan with University of Islamic Sciences, Karachi calculation method
     * and Hanafi Asr jurisprudence.
     */
    val DEFAULT_CITY = CityLocation(
        name = "Islamabad",
        country = "Pakistan",
        latitude = 33.6844,
        longitude = 73.0479,
        timezoneId = "Asia/Karachi",
        recommendedMethod = CalculationMethod.KARACHI,
        recommendedAsrMethod = AsrMethod.HANAFI
    )

    val ALL_CITIES: List<CityLocation> = listOf(
        DEFAULT_CITY,
        CityLocation("Karachi", "Pakistan", 24.8607, 67.0011, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Lahore", "Pakistan", 31.5204, 74.3587, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Rawalpindi", "Pakistan", 33.5651, 73.0169, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Peshawar", "Pakistan", 34.0151, 71.5249, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Quetta", "Pakistan", 30.1798, 66.9750, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Multan", "Pakistan", 30.1575, 71.5249, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Faisalabad", "Pakistan", 31.4504, 73.1350, "Asia/Karachi", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Makkah", "Saudi Arabia", 21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA, AsrMethod.STANDARD),
        CityLocation("Madinah", "Saudi Arabia", 24.5247, 39.5692, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA, AsrMethod.STANDARD),
        CityLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA, AsrMethod.STANDARD),
        CityLocation("Jeddah", "Saudi Arabia", 21.5433, 39.1728, "Asia/Riyadh", CalculationMethod.UMM_AL_QURA, AsrMethod.STANDARD),
        CityLocation("Jerusalem", "Palestine", 31.7683, 35.2137, "Asia/Jerusalem", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo", CalculationMethod.EGYPTIAN, AsrMethod.STANDARD),
        CityLocation("Alexandria", "Egypt", 31.2001, 29.9187, "Africa/Cairo", CalculationMethod.EGYPTIAN, AsrMethod.STANDARD),
        CityLocation("Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.HANAFI),
        CityLocation("Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.HANAFI),
        CityLocation("Dubai", "United Arab Emirates", 25.2048, 55.2708, "Asia/Dubai", CalculationMethod.DUBAI, AsrMethod.STANDARD),
        CityLocation("Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773, "Asia/Dubai", CalculationMethod.DUBAI, AsrMethod.STANDARD),
        CityLocation("Doha", "Qatar", 25.2854, 51.5310, "Asia/Qatar", CalculationMethod.QATAR, AsrMethod.STANDARD),
        CityLocation("Kuwait City", "Kuwait", 29.3759, 47.9774, "Asia/Kuwait", CalculationMethod.KUWAIT, AsrMethod.STANDARD),
        CityLocation("Manama", "Bahrain", 26.2285, 50.5860, "Asia/Bahrain", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Muscat", "Oman", 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Amman", "Jordan", 31.9454, 35.9284, "Asia/Amman", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Beirut", "Lebanon", 33.8938, 35.5018, "Asia/Beirut", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Baghdad", "Iraq", 33.3152, 44.3661, "Asia/Baghdad", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Chittagong", "Bangladesh", 22.3569, 91.7832, "Asia/Dhaka", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Surabaya", "Indonesia", -7.2575, 112.7521, "Asia/Jakarta", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Bandung", "Indonesia", -6.9175, 107.6191, "Asia/Jakarta", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Singapore", "Singapore", 1.3521, 103.8198, "Asia/Singapore", CalculationMethod.SINGAPORE, AsrMethod.STANDARD),
        CityLocation("London", "United Kingdom", 51.5074, -0.1278, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Birmingham", "United Kingdom", 52.4862, -1.8904, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Manchester", "United Kingdom", 53.4808, -2.2426, "Europe/London", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Paris", "France", 48.8566, 2.3522, "Europe/Paris", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Berlin", "Germany", 52.5200, 13.4050, "Europe/Berlin", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Amsterdam", "Netherlands", 52.3676, 4.9041, "Europe/Amsterdam", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("New York", "United States", 40.7128, -74.0060, "America/New_York", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Chicago", "United States", 41.8781, -87.6298, "America/Chicago", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Houston", "United States", 29.7604, -95.3698, "America/Chicago", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Dallas", "United States", 32.7767, -96.7970, "America/Chicago", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Los Angeles", "United States", 34.0522, -118.2437, "America/Los_Angeles", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("San Francisco", "United States", 37.7749, -122.4194, "America/Los_Angeles", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Toronto", "Canada", 43.6532, -79.3832, "America/Toronto", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Montreal", "Canada", 45.5017, -73.5673, "America/Toronto", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Vancouver", "Canada", 49.2827, -123.1207, "America/Vancouver", CalculationMethod.ISNA, AsrMethod.STANDARD),
        CityLocation("Sydney", "Australia", -33.8688, 151.2093, "Australia/Sydney", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Melbourne", "Australia", -37.8136, 144.9631, "Australia/Melbourne", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Tokyo", "Japan", 35.6762, 139.6503, "Asia/Tokyo", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Casablanca", "Morocco", 33.5731, -7.5898, "Africa/Casablanca", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Algiers", "Algeria", 36.7538, 3.0588, "Africa/Algiers", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Tunis", "Tunisia", 36.8065, 10.1815, "Africa/Tunis", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Nairobi", "Kenya", -1.2921, 36.8219, "Africa/Nairobi", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Johannesburg", "South Africa", -26.2041, 28.0473, "Africa/Johannesburg", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.STANDARD),
        CityLocation("Tashkent", "Uzbekistan", 41.2995, 69.2401, "Asia/Tashkent", CalculationMethod.KARACHI, AsrMethod.HANAFI),
        CityLocation("Baku", "Azerbaijan", 40.4093, 49.8671, "Asia/Baku", CalculationMethod.MUSLIM_WORLD_LEAGUE, AsrMethod.HANAFI)
    )

    fun searchCities(query: String): List<CityLocation> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return ALL_CITIES
        return ALL_CITIES.filter {
            it.name.lowercase().contains(trimmed) || it.country.lowercase().contains(trimmed)
        }
    }
}
