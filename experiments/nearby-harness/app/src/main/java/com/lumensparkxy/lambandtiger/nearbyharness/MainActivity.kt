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

/** Local diagnostics only. No connection client, permission requests, or game state. */
class MainActivity : Activity() {
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
        findViewById<Button>(R.id.refresh).setOnClickListener { refreshDiagnostics() }
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
