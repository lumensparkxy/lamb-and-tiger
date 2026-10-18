package com.lumensparkxy.lambandtiger.nearbyharness

import android.Manifest

/** Permission policy for this experiment; no requests or radio calls are made here. */
internal object NearbyPermissions {
    fun runtimeFor(api: Int): List<String> {
        require(api >= 29) { "The harness requires Android 10 or later." }
        return buildList {
            if (api <= 32) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (api >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (api >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }
}
