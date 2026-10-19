package com.maswadkar.lambandtiger.ads

/** One attempt per completed match, at least two minutes between displayed ads. */
internal class InterstitialGate(
    private val now: () -> Long,
    private val lastShown: () -> Long,
    private val recordShown: (Long) -> Unit,
    private val attempted: MutableSet<String> = mutableSetOf(),
) {
    private var pending: Pair<String, () -> Unit>? = null

    fun begin(key: String, current: String?, ready: Boolean, proceed: () -> Unit): Boolean {
        if (pending != null || key != current) return false
        val first = attempted.add(key)
        if (!first || !ready || (lastShown() != 0L && now() - lastShown() < 120_000L)) {
            proceed()
            return false
        }
        pending = key to proceed
        return true
    }

    fun shown() { recordShown(now()) }
    fun finish(key: String, current: String?) {
        if (pending?.first != key) return
        val action = pending
        pending = null
        if (action != null && action.first == current) action.second()
    }
    fun cancel() { pending = null }
}
