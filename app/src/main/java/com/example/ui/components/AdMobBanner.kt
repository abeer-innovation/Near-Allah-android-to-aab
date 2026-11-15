package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CreamBorder
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.DarkBrownText
import com.example.ui.theme.MediumBrownText
import com.example.ui.theme.SageGreenPrimary
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds

object AdMobConfig {
    /**
     * Official Google AdMob sample test IDs
     * Reference: https://developers.google.com/admob/android/test-ads
     */
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Production Ad Unit IDs:
     * When ready for release, provide your registered Google AdMob IDs here.
     * The app architecture automatically switches to production IDs without code restructuring.
     */
    var productionAppId: String? = null
    var productionBannerAdUnitId: String? = null
    var productionInterstitialAdUnitId: String? = null

    // Frequency cap: at least 10 minutes between interstitial ad impressions
    const val FREQUENCY_CAP_INTERVAL_MS: Long = 10 * 60 * 1000L

    fun getAppId(): String = productionAppId ?: TEST_APP_ID
    fun getBannerAdUnitId(): String = productionBannerAdUnitId ?: TEST_BANNER_AD_UNIT_ID
    fun getInterstitialAdUnitId(): String = productionInterstitialAdUnitId ?: TEST_INTERSTITIAL_AD_UNIT_ID

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                val reqConfig = com.google.android.gms.ads.RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
                MobileAds.setRequestConfiguration(reqConfig)
                MobileAds.initialize(context) {
                    isInitialized = true
                    // Load ads in advance gracefully
                    AdMobInterstitialManager.preload(context.applicationContext)
                }
            } catch (_: Exception) {
                // Graceful fallback in environments without Google Play Services
            }
        }
    }
}

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.getBannerAdUnitId()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AdMobConfig.initialize(context)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_banner_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CreamBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle ad disclaimer badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = CreamSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("admob_label")
            ) {
                Text(
                    text = "AD • TEST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MediumBrownText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.testTag("admob_adview"),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            setAdUnitId(adUnitId)
                            adListener = object : AdListener() {
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    // Non-fatal fallback for emulator or offline environments
                                }
                            }
                            try {
                                val adRequest = AdRequest.Builder().build()
                                loadAd(adRequest)
                            } catch (_: Exception) { }
                        }
                    }
                )
            }
        }
    }
}
