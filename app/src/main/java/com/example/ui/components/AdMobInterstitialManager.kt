package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.reminder.PrayerAlarmService
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages Google AdMob Interstitial Ads with:
 * 1. Preloading in advance and graceful error recovery.
 * 2. Showing an interstitial on app open only after the main screen loads successfully.
 * 3. Strict frequency capping to prevent showing ads repeatedly on every interaction.
 * 4. Protection of religious activities (never interrupts prayer tracking, alarms, or journal logging).
 * 5. Separation of test and production configurations.
 */
object AdMobInterstitialManager {

    private const val TAG = "AdMobInterstitial"
    private const val PREFS_NAME = "nearallah_admob_interstitial_prefs"
    private const val KEY_LAST_SHOWN_TIMESTAMP = "last_interstitial_shown_timestamp"

    private var cachedInterstitialAd: InterstitialAd? = null
    private var isAdLoading: Boolean = false
    private var hasShownOnCurrentAppLaunch: Boolean = false

    /**
     * Preloads an interstitial ad in advance.
     * Safe to call multiple times; ignores if an ad is already cached or actively loading.
     */
    fun preload(context: Context) {
        if (cachedInterstitialAd != null || isAdLoading) {
            return
        }

        try {
            isAdLoading = true
            val adRequest = AdRequest.Builder().build()
            val adUnitId = AdMobConfig.getInterstitialAdUnitId()

            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        cachedInterstitialAd = ad
                        isAdLoading = false
                        Log.d(TAG, "AdMob Interstitial Ad successfully loaded and cached.")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        cachedInterstitialAd = null
                        isAdLoading = false
                        Log.w(TAG, "AdMob Interstitial Ad failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                    }
                }
            )
        } catch (e: Exception) {
            cachedInterstitialAd = null
            isAdLoading = false
            Log.w(TAG, "Exception during interstitial preload: ${e.message}")
        }
    }

    /**
     * Determines whether an interstitial ad is permitted to be shown.
     * Evaluates:
     * - Protection of religious activities (active prayer alarms, scan namaz dialog).
     * - Per-app-launch limitation (at most once per app open).
     * - Frequency cap interval (e.g. 10 minutes between ad impressions).
     * - Presence of a loaded ad.
     */
    fun canShowInterstitial(
        context: Context,
        isReligiousActivityActive: Boolean = false
    ): Boolean {
        // 1. Never interrupt active prayer alarm audio or prayer logging activities
        if (isReligiousActivityActive || PrayerAlarmService.isAlarmActive) {
            Log.d(TAG, "Interstitial suppressed: religious activity or prayer alarm is active.")
            return false
        }

        // 2. Do not show repeatedly in the same app session
        if (hasShownOnCurrentAppLaunch) {
            Log.d(TAG, "Interstitial suppressed: already shown during current app session.")
            return false
        }

        // 3. Frequency cap check using persistent SharedPreferences
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownTime = prefs.getLong(KEY_LAST_SHOWN_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        val elapsed = now - lastShownTime

        if (elapsed < AdMobConfig.FREQUENCY_CAP_INTERVAL_MS) {
            Log.d(TAG, "Interstitial suppressed: frequency cap active (${elapsed / 1000}s / ${AdMobConfig.FREQUENCY_CAP_INTERVAL_MS / 1000}s).")
            return false
        }

        // 4. Must have a ready ad
        return cachedInterstitialAd != null
    }

    /**
     * Shows the interstitial ad if all policy, frequency capping, and religious protection conditions are met.
     * If unavailable or not allowed, gracefully continues the app without interruption.
     */
    fun showAppOpenInterstitial(
        activity: Activity,
        isReligiousActivityActive: Boolean = false,
        onDismissed: () -> Unit = {}
    ) {
        if (!canShowInterstitial(activity, isReligiousActivityActive)) {
            // If ad is not ready, trigger preload for next eligible time and proceed normally
            if (cachedInterstitialAd == null && !isAdLoading) {
                preload(activity.applicationContext)
            }
            onDismissed()
            return
        }

        val ad = cachedInterstitialAd ?: run {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "AdMob Interstitial Ad dismissed by user.")
                cachedInterstitialAd = null
                // Preload next ad in advance for future eligible session
                preload(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "AdMob Interstitial Ad failed to show: ${adError.message}")
                cachedInterstitialAd = null
                preload(activity.applicationContext)
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "AdMob Interstitial Ad presented on screen.")
                hasShownOnCurrentAppLaunch = true
                recordAdImpression(activity)
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.w(TAG, "Exception presenting interstitial ad: ${e.message}")
            cachedInterstitialAd = null
            onDismissed()
        }
    }

    private fun recordAdImpression(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SHOWN_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    /**
     * Testing hook to reset frequency cap and session state in unit tests.
     */
    fun resetStateForTesting(context: Context? = null) {
        cachedInterstitialAd = null
        isAdLoading = false
        hasShownOnCurrentAppLaunch = false
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }
}
