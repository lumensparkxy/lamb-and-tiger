package com.maswadkar.lambandtiger.nearby

/** Each pairing stage gets its own budget; obsolete callbacks cannot end a later stage. */
internal class PairingTimeout(
    private val schedule: (Long, () -> Unit) -> Unit,
    private val expired: (String) -> Unit,
) {
    enum class Stage(val reason: String) {
        SEARCH("DiscoveryTimeout"), CONNECTION("ConnectionTimeout"), CONFIRMATION("ConfirmationTimeout")
    }
    private var revision = 0L

    fun enter(stage: Stage) {
        val ticket = ++revision
        schedule(60_000) {
            if (ticket == revision) {
                revision++
                expired(stage.reason)
            }
        }
    }

    fun cancel() { revision++ }
}
