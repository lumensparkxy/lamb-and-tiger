package com.maswadkar.lambandtiger.nearby

import android.Manifest.permission.*
import org.junit.Assert.*
import org.junit.Test

class NearbyPermissionsTest {
    @Test fun oldAndroidUsesCdmWithoutRuntimeLocation() {
        for (api in 29..30) for (host in listOf(true, false)) assertTrue(NearbyPermissions.runtimeFor(api, host).isEmpty())
    }
    @Test fun joiningNeedsOnlyConnectionPermission() {
        for (api in 31..37) assertEquals(listOf(BLUETOOTH_CONNECT), NearbyPermissions.runtimeFor(api, false))
    }
    @Test fun hostingAlsoNeedsDiscoverability() {
        for (api in 31..37) assertEquals(listOf(BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE), NearbyPermissions.runtimeFor(api, true))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedAndroid() { NearbyPermissions.runtimeFor(28) }
}
