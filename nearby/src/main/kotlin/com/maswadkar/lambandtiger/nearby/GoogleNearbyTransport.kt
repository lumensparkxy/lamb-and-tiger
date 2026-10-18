package com.maswadkar.lambandtiger.nearby

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.util.UUID

class GoogleNearbyTransport(context: Context) : Transport {
    private val context = context.applicationContext
    private val client = Nearby.getConnectionsClient(this.context)
    private val handler = Handler(Looper.getMainLooper())
    val label = "Lamb and Tiger ${UUID.randomUUID().toString().take(4)}"
    private val service = "com.maswadkar.lambandtiger.nearby.v1"
    private var generation = 0
    private var endpoint: String? = null
    private var listener: Transport.Listener? = null
    private var lifecycle: ConnectionLifecycleCallback? = null
    private var receiver: PayloadCallback? = null
    private var accepted = false
    private var connected = false
    private var hasCode = false
    private val peers = mutableSetOf<String>()
    private val timeout = PairingTimeout({ delay, task -> handler.postDelayed({ task() }, delay) }, ::fail)
    private var startedAt = 0L
    private fun trace(stage: String) {
        android.util.Log.i("NearbyLink", "${android.os.SystemClock.elapsedRealtime()-startedAt}ms $stage")
    }
    override fun stop() {
        generation++
        timeout.cancel()
        connected = false; accepted = false; hasCode = false; endpoint = null
        peers.clear(); receiver = null; lifecycle = null
        client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints()
    }
    private fun fail(e: Exception) = fail(if (e is ApiException)
        ConnectionsStatusCodes.getStatusCodeString(e.statusCode) else e.javaClass.simpleName)
    private fun fail(reason: String) { trace("failed: $reason"); stop(); listener?.stopped(reason) }
    override fun start(host: Boolean, listener: Transport.Listener) {
        stop(); this.listener = listener
        startedAt=android.os.SystemClock.elapsedRealtime(); trace(if(host) "advertising requested" else "discovery requested")
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            fail("PlayServicesUnavailable"); return
        }
        val ticket = generation
        fun valid() = ticket == generation
        val payload = object : PayloadCallback() {
            override fun onPayloadReceived(id: String, payload: Payload) {
                if (!valid() || !connected || endpoint != id) return
                val bytes = payload.asBytes()
                if (bytes == null || bytes.size > 32768) fail("InvalidPayload") else listener.bytes(bytes)
            }
            override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {
                if (valid() && endpoint == id && update.status == PayloadTransferUpdate.Status.FAILURE)
                    fail("TransferFailed")
            }
        }
        receiver = payload
        val callbacks = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
                if (!valid() || (endpoint != null && endpoint != id)) { client.rejectConnection(id); return }
                if (info.authenticationDigits.isBlank()) { client.rejectConnection(id); fail("MissingCode"); return }
                trace("connection initiated")
                timeout.enter(PairingTimeout.Stage.CONFIRMATION)
                endpoint = id; hasCode = true
                client.stopDiscovery(); client.stopAdvertising()
                listener.confirmation(info.endpointName.take(64), info.authenticationDigits)
            }
            override fun onConnectionResult(id: String, result: ConnectionResolution) {
                if (!valid() || endpoint != id) return
                trace("connection result ${result.status.statusCode}")
                if (!result.status.isSuccess || !accepted) { fail("ConnectionRejected"); return }
                timeout.cancel()
                connected = true; listener.connected()
            }
            override fun onDisconnected(id: String) { if (valid() && endpoint == id) fail("Disconnected") }
        }
        lifecycle = callbacks
        timeout.enter(PairingTimeout.Stage.SEARCH)
        try {
            val task = if (host) client.startAdvertising(label, service, callbacks,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
            else client.startDiscovery(service, object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                    if (valid() && endpoint == null && info.serviceId == service && peers.add(id)) {
                        trace("endpoint found")
                        listener.peer(id, info.endpointName.take(64), true)
                    }
                }
                override fun onEndpointLost(id: String) {
                    if (valid()) { trace("endpoint lost"); peers.remove(id); listener.peer(id, "", false) }
                }
            }, DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
            task.addOnSuccessListener { if(valid()) trace("search started") }
            task.addOnFailureListener { if (valid()) fail(it) }
        } catch (e: Exception) { fail(e) }
    }
    override fun connect(id: String) {
        if (endpoint != null || id !in peers) return
        timeout.enter(PairingTimeout.Stage.CONNECTION)
        trace("connection requested")
        endpoint = id; client.stopDiscovery()
        val ticket = generation
        client.requestConnection(label, id, lifecycle ?: return)
            .addOnSuccessListener { if(ticket==generation) trace("connection request submitted") }
            .addOnFailureListener { if (ticket == generation) fail(it) }
    }
    override fun confirm() {
        val id = endpoint ?: return
        if (accepted || !hasCode) return
        accepted = true; trace("accepted locally")
        val ticket = generation
        client.acceptConnection(id, receiver ?: return)
            .addOnFailureListener { if (ticket == generation) fail(it) }
    }
    override fun send(bytes: ByteArray) {
        require(bytes.size <= 32768)
        check(connected && accepted)
        val ticket = generation
        client.sendPayload(endpoint!!, Payload.fromBytes(bytes))
            .addOnFailureListener { if (ticket == generation) fail(it) }
    }
}
