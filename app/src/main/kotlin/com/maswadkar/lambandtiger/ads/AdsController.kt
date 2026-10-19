package com.maswadkar.lambandtiger.ads

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.*
import com.maswadkar.lambandtiger.BuildConfig

internal val LocalAds = staticCompositionLocalOf<AdsController?> { null }

/** Keeps SDK initialization process-wide without retaining an Activity. */
private object AdsRuntime {
    var initialized by mutableStateOf(false)
    var initializing = false
    val attempted = mutableSetOf<String>()
    fun initialize(activity: Activity) {
        if (initialized || initializing) return
        initializing = true
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder()
            .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G).build())
        val context = activity.applicationContext
        Thread {
            MobileAds.initialize(context) {
                Handler(Looper.getMainLooper()).post { initialized = true; initializing = false }
            }
        }.start()
    }
}

/** Activity-owned; all entry points and callbacks run on the main thread. */
internal class AdsController(private val activity: Activity) {
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private val prefs = activity.getSharedPreferences("advertising", Activity.MODE_PRIVATE)
    private val gate = InterstitialGate(System::currentTimeMillis,
        { prefs.getLong("lastShown", 0) },
        { prefs.edit().putLong("lastShown", it).apply() }, AdsRuntime.attempted)
    var resumed by mutableStateOf(false); private set
    var eligible by mutableStateOf(false); private set
    var privacyRequired by mutableStateOf(false); private set
    var privacyError by mutableStateOf(false); private set
    private var home = false
    private var settings = false
    private var completed: String? = null
    private var destroyed = false
    private var requested = false
    private var updating = false
    private var formLoading = false
    private var modal = false
    private var generation = 0
    private var interstitial: InterstitialAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var lastLoad = 0L
    var showing = false; private set
    private var finishPending: String? = null
    private var activeAdKey: String? = null
    val interstitialReady: Boolean get() = valid() && resumed && eligible && !modal && interstitial != null &&
        System.currentTimeMillis() - loadedAt < 3_600_000L
    val bannerAllowed: Boolean get() = BuildConfig.ADS_ENABLED && eligible &&
        AdsRuntime.initialized && resumed && home && !modal && !showing

    fun updateLocation(isHome: Boolean, isSettings: Boolean, completedMatch: String?) {
        home = isHome; settings = isSettings; completed = completedMatch
        refresh()
    }
    fun resume() { resumed = true; refresh() }
    fun pause() { resumed = false }
    fun destroy() {
        destroyed = true; generation++; interstitial = null; gate.cancel()
    }
    private fun valid() = !destroyed && !activity.isFinishing && !activity.isDestroyed
    private fun refresh() {
        if (!BuildConfig.ADS_ENABLED || !valid() || !resumed) return
        finishPending?.let { key -> finishPending = null; gate.finish(key, completed) }
        if (home && !requested) requestPrivacy()
        if (home && requested && !updating && !eligible && !privacyError && !modal) showRequiredForm()
        if (eligible && AdsRuntime.initialized && (home || completed != null)) preload()
    }
    fun requestPrivacy() {
        if (!BuildConfig.ADS_ENABLED || !valid() || !(home || settings) || modal || updating) return
        requested = true; updating = true; privacyError = false; invalidate()
        val token = generation
        val parameters = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(true).build()
        consent.requestConsentInfoUpdate(activity, parameters, {
            if (valid() && token == generation) {
                updating = false
                privacyRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                if (consent.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) showRequiredForm()
                else acceptEligibility()
            }
        }, {
            if (valid() && token == generation) { updating = false; privacyError = true; eligible = false }
        })
    }
    private fun showRequiredForm() {
        if (!valid() || !resumed || !(home || settings) || formLoading || modal ||
            consent.consentStatus != ConsentInformation.ConsentStatus.REQUIRED) return
        formLoading = true
        val token = generation
        UserMessagingPlatform.loadConsentForm(activity, { form ->
            formLoading = false
            if (valid() && token == generation && resumed && (home || settings)) {
                modal = true
                form.show(activity) { error ->
                    modal = false
                    if (valid() && token == generation) {
                        privacyError = error != null
                        if (error == null) acceptEligibility()
                    }
                }
            }
        }, { if (valid() && token == generation) { formLoading = false; privacyError = true } })
    }
    private fun acceptEligibility() {
        eligible = consent.canRequestAds()
        privacyRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (eligible) AdsRuntime.initialize(activity)
        refresh()
    }
    fun privacyOptions() {
        if (!valid() || !resumed || !settings || !privacyRequired || modal) return
        invalidate(); modal = true
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            modal = false
            if (valid()) { privacyError = error != null; if (error == null) acceptEligibility() }
        }
    }
    private fun invalidate() {
        generation++; eligible = false; interstitial = null; loading = false; lastLoad = 0L
    }
    fun request(): AdRequest = AdRequest.Builder().addNetworkExtrasBundle(
        AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") }).build()

    fun prepare() { refresh() }

    private fun preload() {
        if (!valid() || showing || modal || loading || !eligible || !resumed) return
        if (interstitial != null && System.currentTimeMillis() - loadedAt < 3_600_000L) return
        if (System.currentTimeMillis() - lastLoad < 60_000L) return
        interstitial = null; loading = true; lastLoad = System.currentTimeMillis()
        val token = generation
        InterstitialAd.load(activity, BuildConfig.INTERSTITIAL_AD_UNIT_ID, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                if (valid() && token == generation) {
                    loading = false
                    // Do not retain an Activity-bound ad loaded after navigation into play.
                    if (resumed && (home || completed != null)) { interstitial = ad; loadedAt = System.currentTimeMillis() }
                }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (valid() && token == generation) { loading = false; interstitial = null }
            }
        })
    }
    fun betweenGames(key: String, proceed: () -> Unit) {
        if (!BuildConfig.ADS_ENABLED) { proceed(); return }
        val ad = interstitial
        val ready = interstitialReady
        if (!gate.begin(key, completed, ready, proceed)) return
        interstitial = null; showing = true; activeAdKey = key
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { gate.shown() }
            override fun onAdDismissedFullScreenContent() = finish(key)
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish(key)
        }
        try { ad.show(activity) } catch (_: RuntimeException) { finish(key) }
    }
    private fun finish(key: String) {
        if (activeAdKey != key) return
        activeAdKey = null; showing = false
        if (!valid()) { gate.cancel(); return }
        if (resumed) gate.finish(key, completed) else finishPending = key
    }
}
