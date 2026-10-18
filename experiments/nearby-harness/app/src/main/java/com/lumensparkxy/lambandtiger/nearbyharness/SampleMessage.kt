package com.lumensparkxy.lambandtiger.nearbyharness

/** Experiment-only messages. No game action is executed or persisted. */
internal data class SampleMessage(val kind: String, val id: String) {
    fun encode(): ByteArray {
        require(kind in setOf("SAMPLE", "ACK"))
        require(id.matches(Regex("[0-9a-f-]{36}")))
        return "LT-HARNESS-1|$kind|$id".toByteArray(Charsets.UTF_8)
    }
    companion object {
        fun decode(bytes: ByteArray): SampleMessage? {
            if (bytes.size !in 1..128) return null
            val parts = bytes.toString(Charsets.UTF_8).split('|')
            if (parts.size != 3 || parts[0] != "LT-HARNESS-1") return null
            val message = SampleMessage(parts[1], parts[2])
            return runCatching { if (message.encode().contentEquals(bytes)) message else null }.getOrNull()
        }
    }
}

internal class SampleLedger {
    val pending = linkedSetOf<String>()
    private val received = mutableSetOf<String>()
    fun receive(id: String): Boolean = received.add(id)
    fun acknowledge(id: String): Boolean = pending.remove(id)
    fun clear() { pending.clear(); received.clear() }
}
