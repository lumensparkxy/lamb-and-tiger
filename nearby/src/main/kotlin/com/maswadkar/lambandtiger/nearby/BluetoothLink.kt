package com.maswadkar.lambandtiger.nearby

import java.io.*
import java.security.MessageDigest
import java.security.SecureRandom

/** Framing over Android's authenticated, encrypted RFCOMM socket; not a crypto protocol. */
internal object BluetoothFrames {
    const val MAX_PAYLOAD = 32768
    fun read(input: DataInputStream): ByteArray {
        val size = input.readInt()
        require(size in 1..MAX_PAYLOAD + 1) { "InvalidFrameLength" }
        return ByteArray(size).also(input::readFully)
    }
    fun write(output: DataOutputStream, frame: ByteArray) {
        require(frame.size in 1..MAX_PAYLOAD + 1)
        output.writeInt(frame.size); output.write(frame); output.flush()
    }
}

/** Both people must confirm the same code before any app protocol payload is exposed. */
internal class BluetoothLink(
    name: String,
    private val send: (ByteArray) -> Unit,
    private val confirmation: (String, String) -> Unit,
    private val connected: () -> Unit,
    private val bytes: (ByteArray) -> Unit,
    private val nonce: ByteArray = ByteArray(32).also(SecureRandom()::nextBytes),
) {
    private var peerNonce: ByteArray? = null
    private var localConfirmed = false
    private var peerConfirmed = false
    private var ready = false
    val hello: ByteArray = ByteArrayOutputStream().also { buffer ->
        DataOutputStream(buffer).use { out ->
            out.writeByte(1); out.writeInt(0x4c544231) // LTB1: Bluetooth link version, separate from game protocol.
            out.writeUTF(name.take(64)); out.write(nonce)
        }
    }.toByteArray()
    fun receive(frame: ByteArray) {
        require(frame.size in 1..BluetoothFrames.MAX_PAYLOAD + 1)
        val input = DataInputStream(ByteArrayInputStream(frame))
        when (input.readUnsignedByte()) {
            1 -> {
                require(peerNonce == null && input.readInt() == 0x4c544231)
                val name = input.readUTF(); require(name.length in 1..64)
                val remote = ByteArray(32).also(input::readFully)
                require(input.available() == 0 && !remote.contentEquals(nonce))
                peerNonce = remote
                val ordered = listOf(nonce, remote).sortedBy { value -> value.joinToString("") { "%02x".format(it) } }
                val digest = MessageDigest.getInstance("SHA-256").digest(ordered[0] + ordered[1])
                val number = ((digest[0].toLong() and 255) shl 24) or ((digest[1].toLong() and 255) shl 16) or
                    ((digest[2].toLong() and 255) shl 8) or (digest[3].toLong() and 255)
                confirmation(name, (number % 1_000_000).toString().padStart(6, '0'))
            }
            2 -> { require(peerNonce != null && frame.size == 1 && !peerConfirmed); peerConfirmed = true; finish() }
            3 -> { check(ready); bytes(frame.copyOfRange(1, frame.size)) }
            else -> error("UnsupportedBluetoothFrame")
        }
    }
    fun confirm() {
        if (peerNonce == null || localConfirmed) return
        localConfirmed = true; send(byteArrayOf(2)); finish()
    }
    private fun finish() {
        if (localConfirmed && peerConfirmed && !ready) { ready = true; connected() }
    }
    fun sendPayload(value: ByteArray) {
        check(ready); require(value.size <= BluetoothFrames.MAX_PAYLOAD)
        send(byteArrayOf(3) + value)
    }
}
