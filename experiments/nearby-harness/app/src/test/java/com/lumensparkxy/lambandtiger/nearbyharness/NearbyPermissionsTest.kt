package com.lumensparkxy.lambandtiger.nearbyharness

import android.Manifest.permission.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyPermissionsTest {
    @Test fun android10And11UseLocationWithoutNewRadioPermissions() {
        for (api in 29..30) {
            assertEquals(setOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION),
                NearbyPermissions.runtimeFor(api).toSet())
        }
    }

    @Test fun android12And12LKeepLocationAndAddBluetooth() {
        for (api in 31..32) {
            val permissions = NearbyPermissions.runtimeFor(api)
            assertTrue(permissions.containsAll(listOf(ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION, BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE)))
            assertFalse(permissions.contains(NEARBY_WIFI_DEVICES))
        }
    }

    @Test fun android13Through16UseNearbyWifiWithoutLocation() {
        for (api in 33..36) {
            assertEquals(setOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE,
                NEARBY_WIFI_DEVICES), NearbyPermissions.runtimeFor(api).toSet())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun olderAndroidIsOutsideTheHarnessScope() {
        NearbyPermissions.runtimeFor(28)
    }
}
