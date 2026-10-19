package com.maswadkar.lambandtiger.nearby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.*
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.*

/** CDM owns discovery and explicit device selection; no app scanning permission. */
@Suppress("DEPRECATION")
class CompanionPairing(private val context: Context) {
    private val manager get() = context.getSystemService(CompanionDeviceManager::class.java)
    private var generation = 0
    private var pending = false
    fun cancel() { generation++; pending = false }
    fun request(picker: (IntentSender) -> Unit, failure: () -> Unit) {
        cancel(); clearAssociations(context)
        val ticket = generation
        pending = true
        val callback = object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(chooserLauncher: IntentSender) = launch(chooserLauncher)
            override fun onAssociationPending(intentSender: IntentSender) = launch(intentSender)
            private fun launch(sender: IntentSender) {
                if (ticket == generation && pending) runCatching { picker(sender) }.onFailure { failed() }
            }
            override fun onAssociationCreated(info: AssociationInfo) {
                // The Activity result carries the selection. Clean up a cancelled late result.
                if (ticket != generation) runCatching { if (Build.VERSION.SDK_INT >= 33) manager?.disassociate(info.id) }
            }
            override fun onFailure(error: CharSequence?) = failed()
            private fun failed() { if (ticket == generation && pending) { pending = false; failure() } }
        }
        runCatching {
            check(context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP))
            val request = AssociationRequest.Builder().addDeviceFilter(BluetoothDeviceFilter.Builder()
                .addServiceUuid(ParcelUuid(CdmBluetoothTransport.SERVICE_UUID), null).build())
                .setSingleDevice(false).build()
            if (Build.VERSION.SDK_INT >= 33) manager!!.associate(request, context.mainExecutor, callback)
            else manager!!.associate(request, callback, Handler(Looper.getMainLooper()))
        }.onFailure { pending = false; failure() }
    }
    fun result(data: Intent?): String? {
        if (!pending) return null
        pending = false
        val address = if (Build.VERSION.SDK_INT >= 33)
            data?.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)?.deviceMacAddress?.toString()
                ?: data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE, BluetoothDevice::class.java)?.address
        else data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)?.address
        return address?.takeIf { isAssociated(context, it) }
    }
    companion object {
        fun isAssociated(context: Context, address: String): Boolean = runCatching {
            if (!BluetoothAdapter.checkBluetoothAddress(address)) return false
            val manager = context.getSystemService(CompanionDeviceManager::class.java) ?: return false
            if (Build.VERSION.SDK_INT >= 33) manager.myAssociations.any { it.deviceMacAddress?.toString() == address }
            else address in manager.associations
        }.getOrDefault(false)
        /** Only this app's CDM grants; Android Bluetooth bonds and game saves are retained. */
        fun clearAssociations(context: Context) {
            runCatching {
                val manager = context.getSystemService(CompanionDeviceManager::class.java) ?: return
                if (Build.VERSION.SDK_INT >= 33) manager.myAssociations.forEach { manager.disassociate(it.id) }
                else manager.associations.forEach { manager.disassociate(it) }
            }
        }
    }
}
