package com.maswadkar.lambandtiger.friend

import java.io.ByteArrayOutputStream

internal enum class Kind { HELLO, PAIR, PROOF, AUTH_OK, SYNC, SIDE, SETUP, READY, CANCEL_READY,
    ACTION, PREPARE, PREPARED, COMMIT, COMMITTED, SETTLED, SETTLED_ACK, OPEN, PAUSE, CONTROL, RESUME,
    DRAW_OFFER, DRAW_ACCEPT, DRAW_CANCEL, REMATCH, HEARTBEAT }
internal data class Packet(val kind: Kind, val session: String = "", val match: String = "", val sender: String = "",
    val request: String = uid(), val sequence: Long = 0, val count: Int = 0, val digest: String = "",
    val fields: List<String> = emptyList(), val data: ByteArray = byteArrayOf()) {
    fun bytes() = with(Bin) { pack {
        writeInt(1); text(kind.name); text(session); text(match); text(sender); text(request)
        writeLong(sequence); writeInt(count); text(digest); writeInt(fields.size); fields.forEach { text(it) }; blob(data)
    } }
    companion object {
        fun decode(bytes: ByteArray) = with(Bin) { read(bytes) {
            require(readInt() == 1)
            val kind = Kind.valueOf(text()); val session = text(); val match = text(); val sender = text(); val request = text()
            val seq = readLong(); val count = readInt(); require(seq >= 0 && count >= 0)
            val digest = text(); val n = readInt(); require(n in 0..16)
            Packet(kind,session,match,sender,request,seq,count,digest,List(n) { text() },blob())
        } }
    }
}

/** Stop-and-wait framing: each <= 24KiB fragment is acknowledged before the next.
 * A new channel belongs to exactly one SDK-authenticated endpoint. Resource failure blocks, never truncates history. */
internal class FramedChannel(private val send: (ByteArray) -> Unit, private val receive: (ByteArray) -> Unit) {
    private val queue = ArrayDeque<Pair<String, ByteArray>>()
    private var offset = 0
    private var incomingId = ""
    private var incomingTotal = 0
    private var incoming = ByteArrayOutputStream()
    private var lastId = ""
    private var lastOffset = -1
    private var lastValue = byteArrayOf()
    fun enqueue(bytes: ByteArray) {
        check(queue.size < 64) { "Backpressure" }
        queue.addLast(uid() to bytes)
        if (queue.size == 1) retransmit()
    }
    fun retransmit() {
        val (id, bytes) = queue.firstOrNull() ?: return
        val end = minOf(bytes.size, offset + 24*1024)
        send(with(Bin) { pack { writeInt(1); writeBoolean(false); text(id); writeInt(offset); writeInt(bytes.size); blob(bytes.copyOfRange(offset,end)) } })
    }
    fun accept(bytes: ByteArray) {
        require(bytes.size <= 32768)
        with(Bin) { read(bytes) {
            require(readInt() == 1)
            val ack = readBoolean(); val id = text(); val at = readInt(); val total = readInt(); val part = blob()
            if (ack) {
                require(part.isEmpty())
                val head = queue.firstOrNull()
                if (head?.first == id && at == offset && total == head.second.size) {
                    offset = minOf(total, offset + 24*1024)
                    if (offset == total) { queue.removeFirst(); offset = 0 }
                    retransmit()
                }
            } else {
                require(total > 0 && at >= 0 && part.isNotEmpty() && part.size <= 24*1024 && at.toLong()+part.size <= total)
                if (id == lastId) {
                    require(total==lastValue.size && at+part.size<=lastValue.size && lastValue.copyOfRange(at,at+part.size).contentEquals(part))
                    acknowledge(id,at,total); return@read
                }
                if (at == 0 && incomingId != id) {
                    require(incomingId.isEmpty()) { "Interleaved transfer" }
                    require(total.toLong() <= Runtime.getRuntime().maxMemory()/4) { "Recovery resource limit" }
                    incomingId = id; incomingTotal = total; incoming = ByteArrayOutputStream()
                }
                require(id == incomingId && incomingTotal == total)
                if (at == incoming.size()) incoming.write(part)
                else require(at < incoming.size() && at + part.size <= incoming.size() && incoming.toByteArray().copyOfRange(at,at+part.size).contentEquals(part))
                val complete = incoming.size() == total
                if (complete) {
                    val value = incoming.toByteArray()
                    incomingId = ""; incoming = ByteArrayOutputStream(); lastId = id; lastOffset = at; lastValue = value
                    // Receipt is only transport framing, never a durable gameplay receipt.
                    acknowledge(id,at,total); receive(value)
                } else acknowledge(id,at,total)
            }
        } }
    }
    private fun acknowledge(id: String, at: Int, total: Int) = send(with(Bin) { pack {
        writeInt(1); writeBoolean(true); text(id); writeInt(at); writeInt(total); blob(byteArrayOf())
    } })
}
