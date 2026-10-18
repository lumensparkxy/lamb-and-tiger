package com.lumensparkxy.lambandtiger.nearbyharness

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.common.api.ApiException
import java.util.UUID

/** Disposable, foreground-only radio experiment. Every reconnect requires fresh confirmation. */
internal class LinkHarness(private val activity: Activity, parent: LinearLayout,
    private val startWithPermissions: (Boolean) -> Unit) {
    private val client = Nearby.getConnectionsClient(activity)
    private val handler = Handler(Looper.getMainLooper())
    private val service = "com.lumensparkxy.lambandtiger.nearbyharness.v1"
    private val label = "${Build.MODEL.take(24)}-${UUID.randomUUID().toString().take(4)}"
    private var generation = 0
    private var endpoint: String? = null
    private var connected = false
    private var accepted = false
    private var active = false
    private var started = 0L
    private val ledger = SampleLedger()
    private val history = ArrayDeque<String>()
    private val status = TextView(activity)
    private val code = TextView(activity)
    private val peers = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val log = TextView(activity)
    private val confirm: Button
    private val reject: Button
    private val send: Button
    private var receiver: PayloadCallback? = null

    init {
        val panel = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        parent.addView(panel, 2)
        panel.addView(TextView(activity).apply { text = "Local name: $label\nSamples only — no game or saved-peer recovery." })
        panel.addView(status)
        button(panel, "Host") { startWithPermissions(true) }
        button(panel, "Join") { startWithPermissions(false) }
        button(panel, "Stop / Disconnect") { stop("Stopped; choose Host or Join to retry") }
        panel.addView(peers)
        panel.addView(code)
        confirm = button(panel, "Codes match — Accept") { accept() }
        reject = button(panel, "Reject") {
            endpoint?.let { client.rejectConnection(it) }
            stop("Rejected; no samples sent")
        }
        send = button(panel, "Send sample") { sendSample() }
        panel.addView(log)
        status.text = "Idle — choose Host or Join"
        render()
    }

    private fun button(parent: LinearLayout, label: String, action: () -> Unit) = Button(activity).apply {
        text = label
        minHeight = (48 * activity.resources.displayMetrics.density).toInt()
        parent.addView(this)
        setOnClickListener { action() }
    }
    private fun event(message: String) {
        val elapsed = if (started == 0L) 0L else SystemClock.elapsedRealtime() - started
        history.addLast("${elapsed}ms: $message")
        while (history.size > 20) history.removeFirst()
        log.text = history.joinToString("\n")
    }
    private fun render() {
        confirm.isEnabled = active && endpoint != null && !accepted && !connected && code.text.isNotBlank()
        reject.isEnabled = active && endpoint != null && !connected
        send.isEnabled = connected && accepted && ledger.pending.size < 10
    }
    fun blocked(message: String) { stop(message) }
    fun stop(reason: String) {
        generation++
        active = false
        connected = false
        accepted = false
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        if (ledger.pending.isNotEmpty()) event("${ledger.pending.size} samples unacknowledged; delivery uncertain, no replay")
        ledger.clear()
        endpoint = null
        receiver = null
        code.text = ""
        peers.removeAllViews()
        status.text = reason
        event(reason)
        render()
    }
    fun pause() { if (active) stop("Paused on background/lock. Reconnect and confirm on both devices.") }
    fun start(host: Boolean) {
        stop("Starting ${if (host) "host" else "discovery"}")
        started = SystemClock.elapsedRealtime()
        active = true
        val ticket = generation
        fun valid() = active && generation == ticket
        val payloads = object : PayloadCallback() {
            override fun onPayloadReceived(id: String, payload: Payload) {
                if (!valid() || !connected || !accepted || endpoint != id) return
                val message = payload.asBytes()?.let(SampleMessage::decode)
                if (message == null) { event("Rejected invalid sample bytes"); return }
                if (message.kind == "ACK") {
                    event(if (ledger.acknowledge(message.id)) "ACK received ${message.id}" else "Unknown/duplicate ACK ignored")
                    render()
                } else {
                    event(if (ledger.receive(message.id)) "RECEIVED ${message.id}" else "Duplicate sample ${message.id}")
                    client.sendPayload(id, Payload.fromBytes(SampleMessage("ACK", message.id).encode()))
                        .addOnFailureListener { if (valid()) event("ACK send failed: ${error(it)}") }
                }
            }
            override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {
                if (valid() && endpoint == id && update.status == PayloadTransferUpdate.Status.FAILURE)
                    event("Payload transfer failed; receipt unconfirmed")
            }
        }
        receiver = payloads
        val lifecycle = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
                if (!valid()) { client.rejectConnection(id); return }
                if (endpoint != null && endpoint != id) { client.rejectConnection(id); return }
                if (info.authenticationDigits.isBlank()) {
                    client.rejectConnection(id)
                    stop("Missing authentication code; connection rejected")
                    return
                }
                endpoint = id
                client.stopAdvertising(); client.stopDiscovery(); peers.removeAllViews()
                code.text = "Peer: ${info.endpointName.take(40)}\nCompare this code on BOTH screens: ${info.authenticationDigits}"
                status.text = "Awaiting explicit matching-code confirmation"
                event("Connection initiated; compare codes")
                render()
            }
            override fun onConnectionResult(id: String, resolution: ConnectionResolution) {
                if (!valid() || endpoint != id) return
                if (!resolution.status.isSuccess || !accepted) {
                    stop("Connection not accepted: ${resolution.status.statusCode}"); return
                }
                connected = true
                code.text = ""
                status.text = "Connected — both accepted. Samples enabled."
                event("CONNECTED after mutual confirmation")
                render()
            }
            override fun onDisconnected(id: String) {
                if (valid() && endpoint == id) stop("Disconnected. Reconnect and confirm again.")
            }
        }
        val task = if (host) {
            client.startAdvertising(label, service, lifecycle,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
        } else {
            client.startDiscovery(service, object : EndpointDiscoveryCallback() {
                private val buttons = mutableMapOf<String, Button>()
                override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                    if (!valid() || endpoint != null || info.serviceId != service || buttons.containsKey(id)) return
                    event("FOUND ${info.endpointName.take(40)}")
                    buttons[id] = button(peers, "Connect to ${info.endpointName.take(40)}") {
                        if (!valid() || endpoint != null) return@button
                        endpoint = id
                        peers.removeAllViews()
                        client.stopDiscovery()
                        status.text = "Requesting connection; wait for codes"
                        client.requestConnection(label, id, lifecycle)
                            .addOnFailureListener { if (valid()) stop("Request failed: ${error(it)}") }
                    }
                }
                override fun onEndpointLost(id: String) {
                    if (!valid()) return
                    buttons.remove(id)?.let { peers.removeView(it) }
                    event("Endpoint lost")
                }
            }, DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build())
        }
        task.addOnSuccessListener {
            if (valid() && endpoint == null) {
                status.text = if (host) "Hosting — waiting for a joiner" else "Discovering — select a host below"
                event(if (host) "Advertising started" else "Discovery started")
            }
        }.addOnFailureListener { if (valid()) stop("Start failed: ${error(it)}") }
        handler.postDelayed({
            if (valid() && !connected) stop("60-second window ended without a confirmed connection. Retry explicitly.")
        }, 60_000)
        render()
    }
    private fun accept() {
        val id = endpoint ?: return
        val callback = receiver ?: return
        if (!confirm.isEnabled) return
        accepted = true
        val ticket = generation
        status.text = "Accepted locally — waiting for the other device"
        render()
        client.acceptConnection(id, callback).addOnFailureListener {
            if (generation == ticket) stop("Accept failed: ${error(it)}")
        }
    }
    private fun sendSample() {
        val id = endpoint ?: return
        if (!connected || !accepted || ledger.pending.size >= 10) return
        val message = SampleMessage("SAMPLE", UUID.randomUUID().toString())
        ledger.pending.add(message.id)
        val ticket = generation
        event("SEND ${message.id}; awaiting peer ACK")
        render()
        client.sendPayload(id, Payload.fromBytes(message.encode())).addOnSuccessListener {
            if (generation == ticket) event("SDK accepted send ${message.id}; not proof of receipt")
        }.addOnFailureListener {
            if (generation == ticket) event("Send failed ${message.id}: ${error(it)}; receipt uncertain")
        }
        handler.postDelayed({
            if (generation == ticket && message.id in ledger.pending)
                event("No ACK after 10s for ${message.id}; receipt uncertain")
        }, 10_000)
    }
    private fun error(e: Exception) = if (e is ApiException) "${ConnectionsStatusCodes.getStatusCodeString(e.statusCode)} (${e.statusCode})" else e.javaClass.simpleName
}
