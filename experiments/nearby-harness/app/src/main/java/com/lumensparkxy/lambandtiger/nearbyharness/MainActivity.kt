package com.lumensparkxy.lambandtiger.nearbyharness

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowInsets
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/** Isolated diagnostic experiment; no production game state. */
class MainActivity : Activity() {
    private lateinit var link: LinkHarness
    private var pendingHost: Boolean? = null
    private var foreground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val content = findViewById<LinearLayout>(R.id.content)
        val padding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        content.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                view.setPadding(padding + bars.left, padding + bars.top,
                    padding + bars.right, padding + bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(padding + insets.systemWindowInsetLeft,
                    padding + insets.systemWindowInsetTop,
                    padding + insets.systemWindowInsetRight,
                    padding + insets.systemWindowInsetBottom)
            }
            insets
        }
        content.requestApplyInsets()
        link = LinkHarness(this, content) { host ->
            link.stop("Checking permissions")
            val missing = NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT)
                .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) begin(host)
            else {
                pendingHost = host
                requestPermissions(NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT).toTypedArray(), 41)
            }
        }
        findViewById<Button>(R.id.refresh).setOnClickListener { refreshDiagnostics() }
    }

    override fun onStart() { super.onStart(); foreground = true }
    override fun onStop() {
        foreground = false
        if (::link.isInitialized) link.pause()
        super.onStop()
    }
    override fun onDestroy() {
        if (::link.isInitialized) link.stop("Activity closed")
        super.onDestroy()
    }
    private fun begin(host: Boolean) {
        if (!foreground) { link.blocked("Return and choose Host or Join again"); return }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            link.blocked("Google Play services unavailable. Repair it and retry."); return
        }
        runCatching { link.start(host) }.onFailure { link.blocked("Unable to start: ${it.javaClass.simpleName}") }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 41) return
        val host = pendingHost ?: return
        pendingHost = null
        refreshDiagnostics()
        if (NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT).all {
                checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) begin(host)
        else link.blocked("Permissions denied or cancelled. Choose Host/Join to retry; use app Settings if blocked permanently.")
    }

    override fun onResume() {
        super.onResume()
        refreshDiagnostics()
    }

    private fun refreshDiagnostics() {
        findViewById<TextView>(R.id.device_status).text = getString(
            R.string.device_details, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT
        )
        val availability = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this)
        findViewById<TextView>(R.id.services_status).text = getString(
            R.string.services_details,
            getString(if (availability == ConnectionResult.SUCCESS)
                R.string.services_available else R.string.services_unavailable),
            GoogleApiAvailability.getInstance().getErrorString(availability),
            packageVersion("com.google.android.gms"),
            packageVersion("com.android.vending")
        )
        findViewById<TextView>(R.id.capabilities_status).text = getString(
            R.string.capabilities_details,
            featureStatus(PackageManager.FEATURE_BLUETOOTH),
            featureStatus(PackageManager.FEATURE_BLUETOOTH_LE),
            featureStatus(PackageManager.FEATURE_WIFI)
        )
        findViewById<TextView>(R.id.permissions_status).text =
            NearbyPermissions.runtimeFor(Build.VERSION.SDK_INT).joinToString("\n") { permission ->
                getString(R.string.permission_status, permission.substringAfterLast('.'),
                    getString(if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
                        R.string.permission_granted else R.string.permission_not_granted))
            }
    }

    private fun featureStatus(feature: String): String = getString(
        if (packageManager.hasSystemFeature(feature)) R.string.feature_reported
        else R.string.feature_not_reported
    )

    @Suppress("DEPRECATION")
    private fun packageVersion(packageName: String): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName
            ?: getString(R.string.version_unknown)
    } catch (_: PackageManager.NameNotFoundException) {
        getString(R.string.package_not_found)
    }
}
