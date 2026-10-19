package com.maswadkar.lambandtiger.nearby

import org.junit.Assert.*
import org.junit.Test
import java.io.*

class BluetoothLinkTest {
    @Test fun bothPlayersConfirmBeforePayloadAndCodesAgree() {
        val aOut = ArrayDeque<ByteArray>(); val bOut = ArrayDeque<ByteArray>()
        var aCode = ""; var bCode = ""; var aConnected = 0; var bConnected = 0
        var received = byteArrayOf()
        val a = BluetoothLink("A", aOut::add, { _, c -> aCode=c }, { aConnected++ }, {}, ByteArray(32) { 1 })
        val b = BluetoothLink("B", bOut::add, { _, c -> bCode=c }, { bConnected++ }, { received=it }, ByteArray(32) { 2 })
        a.receive(b.hello); b.receive(a.hello)
        assertEquals(aCode,bCode); assertEquals(6,aCode.length)
        assertThrows(IllegalStateException::class.java) { a.sendPayload(byteArrayOf(42)) }
        a.confirm(); b.receive(aOut.removeFirst())
        assertEquals(0,aConnected); assertEquals(0,bConnected)
        b.confirm(); a.receive(bOut.removeFirst())
        assertEquals(1,aConnected); assertEquals(1,bConnected)
        a.confirm(); b.confirm(); assertTrue(aOut.isEmpty()); assertTrue(bOut.isEmpty())
        a.sendPayload(byteArrayOf(42)); b.receive(aOut.removeFirst()); assertArrayEquals(byteArrayOf(42),received)
    }
    @Test fun rejectsEarlyPayloadConfirmationAndRepeatedHello() {
        val a = BluetoothLink("A", {}, { _, _ -> }, {}, {}, ByteArray(32) { 1 })
        val b = BluetoothLink("B", {}, { _, _ -> }, {}, {}, ByteArray(32) { 2 })
        assertThrows(IllegalArgumentException::class.java) { a.receive(byteArrayOf(2)) }
        assertThrows(IllegalStateException::class.java) { a.receive(byteArrayOf(3,42)) }
        assertThrows(IllegalArgumentException::class.java) { a.receive(a.hello) }
        a.receive(b.hello)
        assertThrows(IllegalArgumentException::class.java) { a.receive(b.hello) }
    }
    @Test fun frameBoundsAndTruncationFailBeforePayloadDelivery() {
        for (size in listOf(-1,0,32770,Int.MAX_VALUE)) {
            val data = ByteArrayOutputStream().also { DataOutputStream(it).writeInt(size) }.toByteArray()
            assertThrows(IllegalArgumentException::class.java) { BluetoothFrames.read(DataInputStream(ByteArrayInputStream(data))) }
        }
        val short = ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(20); writeByte(1) } }.toByteArray()
        assertThrows(EOFException::class.java) { BluetoothFrames.read(DataInputStream(ByteArrayInputStream(short))) }
        val payload = ByteArray(32769) { 7 }
        val full = ByteArrayOutputStream(); BluetoothFrames.write(DataOutputStream(full),payload)
        assertArrayEquals(payload,BluetoothFrames.read(DataInputStream(ByteArrayInputStream(full.toByteArray()))))
    }
}
