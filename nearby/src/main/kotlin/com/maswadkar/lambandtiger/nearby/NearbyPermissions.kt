package com.maswadkar.lambandtiger.nearby

import android.Manifest

/** CDM scans for the joining player; only hosts request discoverability. */
object NearbyPermissions {
    fun runtimeFor(api: Int, host: Boolean = true): List<String> {
        require(api >= 29)
        return if (api < 31) emptyList() else buildList {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            if (host) add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
    }
}
