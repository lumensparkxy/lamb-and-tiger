package com.maswadkar.lambandtiger.data

import com.maswadkar.lambandtiger.game.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

data class SoloMatch(
    val id: String,
    val human: Side,
    val difficulty: Difficulty,
    val revision: Long,
    val state: GameState,
)

/** Explicit V1 storage encoding. It is not a Kotlin object serialization or a wire protocol. */
object MatchCodec {
    private fun DataOutputStream.text(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.text(): String {
        val n = readInt()
        require(n in 0..available())
        val bytes = ByteArray(n)
        readFully(bytes)
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }

    private fun DataOutputStream.position(p: Position) {
        writeInt(p.tigers)
        writeInt(p.lambs)
        text(p.turn.name)
        writeInt(p.reserve)
        writeInt(p.captured)
    }

    private fun DataInputStream.position() =
        Position(readInt(), readInt(), Side.valueOf(text()), readInt(), readInt())

    private fun DataOutputStream.action(a: Action) {
        when (a) {
            is Action.Place -> {
                writeByte(1)
                writeByte(a.to.name.single() - 'A')
            }
            is Action.Step -> {
                writeByte(2)
                writeByte(a.from.name.single() - 'A')
                writeByte(a.to.name.single() - 'A')
            }
            is Action.Capture -> {
                writeByte(3)
                writeByte(a.from.name.single() - 'A')
                writeByte(a.over.name.single() - 'A')
                writeByte(a.to.name.single() - 'A')
            }
        }
    }

    private fun DataInputStream.node(): Node {
        val id = readUnsignedByte()
        require(id in 0..22)
        return Node.valueOf(('A' + id).toString())
    }

    private fun DataInputStream.action(): Action =
        when (readUnsignedByte()) {
            1 -> Action.Place(node())
            2 -> Action.Step(node(), node())
            3 -> Action.Capture(node(), node(), node())
            else -> error("Unknown action")
        }

    fun encodeAction(a: Action): ByteArray =
        ByteArrayOutputStream()
            .also { DataOutputStream(it).use { out -> out.action(a) } }
            .toByteArray()

    fun decodeAction(bytes: ByteArray): Action =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            input.action().also { require(input.available() == 0) }
        }

    private fun writeState(out: DataOutputStream, s: GameState, nearby: Boolean = false) {
        require(!s.isFixture) { "Fixture cannot be saved" }
        out.text(s.ruleset)
        out.writeInt(s.eventCount)
        for (e in s.events) when (e) {
            is Event.Played -> out.action(e.action)
            Event.Agreement -> { require(nearby) { "Solo agreement forbidden" }; out.writeByte(4) }
        }
        out.position(s.position)
        out.text(s.outcome.name)
        val entries =
            s.repetitions.entries.sortedWith(
                compareBy(
                    { it.key.tigers },
                    { it.key.lambs },
                    { it.key.turn.name },
                    { it.key.reserve },
                    { it.key.captured },
                )
            )
        out.writeInt(entries.size)
        for ((p, n) in entries) {
            out.position(p)
            out.writeInt(n)
        }
    }

    private fun readState(input: DataInputStream, nearby: Boolean = false): GameState {
        val ruleset = input.text()
        val count = input.readInt()
        require(count >= 0 && count <= input.available())
        val events = List(count) {
            input.mark(1)
            if (input.readUnsignedByte() == 4) { require(nearby); Event.Agreement }
            else { input.reset(); Event.Played(input.action()) }
        }
        val state =
            (GameEngine.replay(events, ruleset, allowAgreement = nearby) as? Transition.Applied)?.state
                ?: error("Invalid history")
        require(state.position == input.position()) { "Snapshot mismatch" }
        require(state.outcome.name == input.text()) { "Outcome mismatch" }
        val n = input.readInt()
        require(n >= 0 && n <= count + 1)
        val reps = HashMap<Position, Int>()
        repeat(n) {
            val p = input.position()
            val c = input.readInt()
            require(reps.put(p, c) == null) { "Duplicate repetition key" }
        }
        require(reps == state.repetitions) { "Repetition mismatch" }
        return state
    }

    fun canonical(s: GameState): ByteArray =
        ByteArrayOutputStream()
            .also {
                DataOutputStream(it).use { out ->
                    out.writeInt(1)
                    writeState(out, s)
                }
            }
            .toByteArray()

    fun nearbyState(s: GameState): ByteArray = ByteArrayOutputStream().also {
        DataOutputStream(it).use { out -> out.writeInt(2); writeState(out, s, true) }
    }.toByteArray()

    fun readNearbyState(bytes: ByteArray): GameState = DataInputStream(ByteArrayInputStream(bytes)).use {
        require(it.readInt() == 2)
        readState(it, true).also { _ -> require(it.available() == 0) }
    }

    fun digest(s: GameState): String =
        MessageDigest.getInstance("SHA-256").digest(canonical(s)).joinToString("") {
            "%02x".format(it)
        }

    fun encode(m: SoloMatch): ByteArray =
        ByteArrayOutputStream()
            .also {
                DataOutputStream(it).use { out ->
                    require(m.revision >= 0)
                    out.writeInt(1)
                    out.text(m.id)
                    out.text(m.human.name)
                    out.text(m.difficulty.name)
                    out.writeLong(m.revision)
                    writeState(out, m.state)
                }
            }
            .toByteArray()

    fun decode(bytes: ByteArray): SoloMatch =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == 1) { "Unsupported save version" }
            val id = input.text()
            require(java.util.UUID.fromString(id).toString() == id)
            val human = Side.valueOf(input.text())
            val difficulty = Difficulty.valueOf(input.text())
            val rev = input.readLong()
            require(rev >= 0)
            val state = readState(input)
            require(rev >= state.eventCount) { "Revision precedes history" }
            val result = SoloMatch(id, human, difficulty, rev, state)
            require(input.available() == 0) { "Trailing save data" }
            result
        }
}
