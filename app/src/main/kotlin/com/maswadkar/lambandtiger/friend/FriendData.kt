package com.maswadkar.lambandtiger.friend

import com.maswadkar.lambandtiger.data.MatchCodec
import com.maswadkar.lambandtiger.game.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal fun uid() = UUID.randomUUID().toString()
internal fun randomSecret() = ByteArray(32).also { SecureRandom().nextBytes(it) }.hex()
internal fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
internal fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
internal fun proof(secret: String, context: String): String = Mac.getInstance("HmacSHA256").run {
    init(SecretKeySpec(secret.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), "HmacSHA256"))
    doFinal(context.toByteArray(Charsets.UTF_8)).hex()
}
internal fun equalSecret(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

internal object Bin {
    fun pack(block: DataOutputStream.() -> Unit) = ByteArrayOutputStream().also { DataOutputStream(it).use(block) }.toByteArray()
    fun <T> read(bytes: ByteArray, block: DataInputStream.() -> T): T = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        input.block().also { require(input.available() == 0) { "Trailing data" } }
    }
    fun DataOutputStream.blob(value: ByteArray) { writeInt(value.size); write(value) }
    fun DataInputStream.blob(): ByteArray {
        val count = readInt(); require(count in 0..available())
        return ByteArray(count).also(::readFully)
    }
    fun DataOutputStream.text(value: String) = blob(value.toByteArray(Charsets.UTF_8))
    fun DataInputStream.text(): String = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(blob())).toString()
}

internal data class FriendMatch(val id: String, val host: String, val guest: String,
    val hostSide: Side, val state: GameState) {
    fun side(player: String): Side { require(player == host || player == guest); return if (player == host) hostSide else hostSide.other() }
    fun bytes(): ByteArray = with(Bin) { pack {
        writeInt(1); text(GameEngine.RULESET); text(id); text(host); text(guest); text(hostSide.name)
        blob(MatchCodec.nearbyState(state))
    } }
    fun digest() = hash(bytes())
    companion object {
        fun decode(bytes: ByteArray) = with(Bin) { read(bytes) {
            require(readInt() == 1 && text() == GameEngine.RULESET)
            val id = text(); val host = text(); val guest = text()
            listOf(id, host, guest).forEach { require(UUID.fromString(it).toString() == it) }
            require(host != guest)
            FriendMatch(id, host, guest, Side.valueOf(text()), MatchCodec.readNearbyState(blob()))
        } }
    }
}

internal enum class Stage { PREPARED, COMMITTED, SETTLED, ACKED }
internal data class Transaction(val request: String, val sequence: Long, val actor: String, val operation: String,
    val base: String, val candidate: FriendMatch, val stage: Stage = Stage.PREPARED) {
    fun identity() = hash(with(Bin) { pack {
        text(request); writeLong(sequence); text(actor); text(operation); text(base); blob(candidate.bytes())
    } })
    fun receipt(role: String) = hash((identity() + "|" + role).toByteArray())
}
internal data class FriendRecord(val match: FriendMatch, val secret: String, val sequence: Long = 0,
    val started: Boolean = false, val tx: Transaction? = null, val requests: Map<String, String> = emptyMap()) {
    fun bytes(): ByteArray = with(Bin) { pack {
        writeInt(1); blob(match.bytes()); text(secret); writeLong(sequence); writeBoolean(started)
        writeBoolean(tx != null)
        tx?.let { text(it.request); writeLong(it.sequence); text(it.actor); text(it.operation); text(it.base)
            blob(it.candidate.bytes()); text(it.stage.name) }
        writeInt(requests.size); requests.toSortedMap().forEach { (k,v) -> text(k); text(v) }
    } }
    companion object {
        fun decode(bytes: ByteArray): FriendRecord = with(Bin) { read(bytes) {
            require(readInt() == 1)
            val match = FriendMatch.decode(blob()); val secret = text(); require(secret.matches(Regex("[0-9a-f]{64}")))
            val sequence = readLong(); require(sequence >= 0); val started = readBoolean()
            val tx = if (readBoolean()) Transaction(text(), readLong(), text(), text(), text(), FriendMatch.decode(blob()), Stage.valueOf(text())) else null
            val n = readInt(); require(n in 0..available()/8)
            val requests = mutableMapOf<String,String>(); repeat(n) { require(requests.put(text(),text()) == null) }
            FriendRecord(match, secret, sequence, started, tx, requests).also {
                if(tx != null) {
                    require(tx.candidate.id==match.id && tx.candidate.host==match.host && tx.candidate.guest==match.guest)
                    require(tx.actor in listOf(match.host,match.guest))
                    if(tx.stage==Stage.PREPARED) {
                        require(tx.sequence==sequence+1 && tx.base==match.digest())
                        val expected=when {
                            tx.operation.startsWith("START:") -> {
                                require(!started && requests["ready:"+tx.operation.removePrefix("START:")]==tx.candidate.digest())
                                GameEngine.newGame()
                            }
                            tx.operation.startsWith("ACTION:") -> {
                                require(started && tx.candidate.hostSide==match.hostSide && match.side(tx.actor)==match.state.sideToMove)
                                val action=(tx.candidate.state.events.lastOrNull() as? Event.Played)?.action ?: error("Invalid candidate")
                                require(tx.operation=="ACTION:"+MatchCodec.encodeAction(action).hex())
                                (GameEngine.apply(match.state,action) as? Transition.Applied)?.state ?: error("Illegal journal")
                            }
                            tx.operation.startsWith("DRAW:") -> {
                                val parts=tx.operation.split(':'); require(parts.size==4 && parts[2]!=parts[3] && parts[3]==tx.actor)
                                require(requests["offer:"+parts[1]]==parts[2]+"|"+match.digest())
                                (GameEngine.completeAgreedDraw(match.state) as? Transition.Applied)?.state ?: error("Invalid draw journal")
                            }
                            else -> error("Unknown journal operation")
                        }
                        require(MatchCodec.nearbyState(expected).contentEquals(MatchCodec.nearbyState(tx.candidate.state)))
                    } else require(started && tx.sequence==sequence && tx.candidate.bytes().contentEquals(match.bytes()))
                }
            }
        } }
    }
}

/** save must atomically persist the journal and (only on commit) promote the Nearby slot. */
internal interface FriendPersistence {
    val player: String
    fun load(): FriendRecord?
    fun save(record: FriendRecord, promote: Boolean = false)
}
