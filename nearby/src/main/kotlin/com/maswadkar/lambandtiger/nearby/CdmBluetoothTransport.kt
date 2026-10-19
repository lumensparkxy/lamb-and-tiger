package com.maswadkar.lambandtiger.nearby

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Android CDM selects the joining endpoint. Only secure RFCOMM APIs are used. */
@SuppressLint("MissingPermission") // Activity gates CONNECT/ADVERTISE; revocation is caught and fails closed.
class CdmBluetoothTransport(context: Context) : Transport {
    companion object { val SERVICE_UUID: UUID = UUID.fromString("9b951718-2ba4-42c8-a8b4-e5601b67f350") }
    private val context = context.applicationContext
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newCachedThreadPool()
    private val lock = Any()
    @Volatile private var generation = 0
    private var server: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null
    private var writer: Future<*>? = null
    private var queue: ArrayBlockingQueue<ByteArray>? = null
    private var link: BluetoothLink? = null
    private var listener: Transport.Listener? = null
    private var selected: String? = null
    private var connecting = false
    private val connectionLabel = "Lamb and Tiger ${UUID.randomUUID().toString().take(4)}"
    val label get() = runCatching { adapter?.name }.getOrNull() ?: connectionLabel
    private val timeout = PairingTimeout({ delay, task -> main.postDelayed({ task() }, delay) }, ::fail)

    fun selectCompanion(address: String?) { selected = address }
    override fun stop() {
        synchronized(lock) {
            generation++; runCatching { server?.close() }; runCatching { socket?.close() }
            server = null; socket = null
        }
        writer?.cancel(true); writer = null; queue = null; link = null; connecting = false
        timeout.cancel()
    }
    fun close() { stop(); io.shutdownNow() }
    private fun fail(reason: String) { stop(); listener?.stopped(reason) }
    private fun post(ticket: Int, task: () -> Unit) { main.post { if (ticket == generation) task() } }

    override fun start(host: Boolean, listener: Transport.Listener) {
        stop(); this.listener = listener
        val ticket = generation
        try {
            val radio = adapter
            if (radio == null || !radio.isEnabled) { fail("BluetoothUnavailable"); return }
            timeout.enter(PairingTimeout.Stage.SEARCH)
            if (!host) {
                val address = selected
                if (address == null || !CompanionPairing.isAssociated(context, address)) { fail("CompanionRequired"); return }
                listener.peer(address, radio.getRemoteDevice(address).name?.take(64) ?: "Bluetooth phone", true)
                return
            }
            io.execute {
                try {
                    val listening = radio.listenUsingRfcommWithServiceRecord("Lamb and Tiger", SERVICE_UUID)
                    synchronized(lock) {
                        if (ticket != generation) { listening.close(); return@execute }
                        server = listening
                    }
                    val incoming = listening.accept()
                    listening.close()
                    synchronized(lock) {
                        if (ticket != generation) { incoming.close(); return@execute }
                        server = null; socket = incoming
                    }
                    post(ticket) { open(ticket, incoming) }
                } catch (_: Exception) { post(ticket) { fail("BluetoothConnectionFailed") } }
            }
        } catch (_: Exception) { fail("BluetoothPermissionOrAvailability") }
    }

    override fun connect(id: String) {
        if (connecting || id != selected || !CompanionPairing.isAssociated(context, id)) return
        connecting = true; timeout.enter(PairingTimeout.Stage.CONNECTION)
        val ticket = generation
        io.execute {
            try {
                val outgoing = adapter!!.getRemoteDevice(id).createRfcommSocketToServiceRecord(SERVICE_UUID)
                synchronized(lock) {
                    if (ticket != generation) { outgoing.close(); return@execute }
                    socket = outgoing
                }
                // CDM owns scanning. Calling adapter.cancelDiscovery would need broad SCAN permission.
                outgoing.connect()
                post(ticket) { open(ticket, outgoing) }
            } catch (_: Exception) { post(ticket) { fail("BluetoothConnectionFailed") } }
        }
    }
    private fun open(ticket: Int, connection: BluetoothSocket) {
        try {
            check(connection.remoteDevice.bondState == BluetoothDevice.BOND_BONDED)
            timeout.enter(PairingTimeout.Stage.CONNECTION)
            val pending = ArrayBlockingQueue<ByteArray>(64)
            queue = pending
            val session = BluetoothLink(connectionLabel, { frame ->
                if (ticket == generation && !pending.offer(frame)) fail("Backpressure")
            }, { name, code ->
                timeout.enter(PairingTimeout.Stage.CONFIRMATION); listener?.confirmation(name, code)
            }, { timeout.cancel(); listener?.connected() }, { listener?.bytes(it) })
            link = session
            pending.add(session.hello)
            writer = io.submit {
                try {
                    val output = DataOutputStream(connection.outputStream)
                    while (ticket == generation) BluetoothFrames.write(output, pending.take())
                } catch (_: Exception) { post(ticket) { fail("BluetoothWriteFailed") } }
            }
            io.execute {
                try {
                    val input = DataInputStream(connection.inputStream)
                    while (ticket == generation) {
                        val frame = BluetoothFrames.read(input)
                        // Bounded read-to-main handoff: a peer cannot flood the main queue.
                        val delivered = java.util.concurrent.CountDownLatch(1)
                        main.post {
                            try {
                                if (ticket == generation) runCatching { session.receive(frame) }
                                    .onFailure { fail("InvalidBluetoothFrame") }
                            } finally { delivered.countDown() }
                        }
                        if (!delivered.await(10, java.util.concurrent.TimeUnit.SECONDS)) error("MainThreadTimeout")
                    }
                } catch (_: Exception) { post(ticket) { fail("Disconnected") } }
            }
        } catch (_: Exception) { fail("BluetoothBondRequired") }
    }
    override fun confirm() { link?.confirm() }
    override fun send(bytes: ByteArray) { checkNotNull(link).sendPayload(bytes.copyOf()) }
}
