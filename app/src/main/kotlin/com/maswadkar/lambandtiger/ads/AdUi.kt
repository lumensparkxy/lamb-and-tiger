package com.maswadkar.lambandtiger.ads

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.maswadkar.lambandtiger.BuildConfig
import com.maswadkar.lambandtiger.R

@Composable
internal fun HomeBanner() {
    val ads = LocalAds.current ?: return
    if (!ads.bannerAllowed) return
    LaunchedEffect(ads) { ads.prepare() }
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        val width = maxWidth.value.toInt()
        val banner = remember(ads, width) {
            AdView(context).apply {
                adUnitId = BuildConfig.BANNER_AD_UNIT_ID
                // Preserve the standard adaptive height (large adaptive can consume 150dp).
                @Suppress("DEPRECATION")
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
            }
        }
        var failed by remember(banner) { mutableStateOf(false) }
        DisposableEffect(banner) {
            banner.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { failed = true; banner.pause() }
            }
            banner.loadAd(ads.request())
            onDispose { banner.pause(); banner.destroy() }
        }
        if (!failed) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.advertisement), style = MaterialTheme.typography.labelSmall)
            AndroidView(factory = { banner }, modifier = Modifier.height(banner.adSize!!.height.dp))
        }
    }
}

@Composable
internal fun PrivacyControls() {
    val uri = LocalUriHandler.current
    val ads = LocalAds.current
    TextButton(onClick = { uri.openUri("https://github.com/lumensparkxy/lamb-and-tiger/blob/main/PRIVACY.md") }) {
        Text(stringResource(R.string.privacy_policy))
    }
    if (ads?.privacyRequired == true) TextButton(onClick = ads::privacyOptions) {
        Text(stringResource(R.string.ad_privacy_options))
    }
    if (ads?.privacyError == true) {
        Text(stringResource(R.string.ad_privacy_unavailable), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = ads::requestPrivacy) { Text(stringResource(R.string.retry_ad_privacy)) }
    }
}
