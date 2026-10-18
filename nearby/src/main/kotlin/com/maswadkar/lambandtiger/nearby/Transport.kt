package com.maswadkar.lambandtiger.nearby

/** Transport knows bytes and authenticated endpoints, never game moves. Call on the main thread. */
interface Transport {
    interface Listener {
        fun peer(id: String, name: String, found: Boolean)
        fun confirmation(name: String, code: String)
        fun connected()
        fun bytes(value: ByteArray)
        fun stopped(reason: String)
    }
    fun start(host: Boolean, listener: Listener)
    fun connect(id: String)
    fun confirm()
    fun send(bytes: ByteArray)
    fun stop()
}
