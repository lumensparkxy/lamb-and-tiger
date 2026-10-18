package com.maswadkar.lambandtiger.nearby

/** Deliberately inert until the two-phone feasibility gate is satisfied. */
interface Transport {
    val available: Boolean
}

object DeferredTransport : Transport {
    override val available = false
}
