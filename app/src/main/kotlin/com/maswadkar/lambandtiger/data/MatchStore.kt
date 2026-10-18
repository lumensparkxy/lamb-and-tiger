package com.maswadkar.lambandtiger.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.maswadkar.lambandtiger.game.*
import java.io.File
import java.util.UUID

class PendingSave : IllegalStateException("Pending operation requires recovery")

enum class Actor {
    HUMAN,
    COMPUTER,
}

enum class SavePoint {
    BEFORE_INTENT,
    AFTER_INTENT,
    DURING_COMMIT,
    AFTER_COMMIT,
}

class MatchStore(
    context: Context,
    name: String = "matches.sqlite",
    private val fault: (SavePoint) -> Unit = {},
) : AutoCloseable {
    private val helper =
        object :
            SQLiteOpenHelper(context, File(context.noBackupFilesDir, name).absolutePath, null, 1) {
            override fun onConfigure(db: SQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE matches(id TEXT PRIMARY KEY, payload BLOB NOT NULL)")
                db.execSQL(
                    "CREATE TABLE slots(mode TEXT PRIMARY KEY CHECK(mode IN ('SOLO','NEARBY')), match_id TEXT NOT NULL REFERENCES matches(id))"
                )
                db.execSQL(
                    "CREATE TABLE pending(id TEXT PRIMARY KEY, match_id TEXT UNIQUE NOT NULL REFERENCES matches(id), base_revision INTEGER NOT NULL, base_digest TEXT NOT NULL, kind TEXT NOT NULL, actor TEXT NOT NULL, payload BLOB NOT NULL)"
                )
                db.execSQL("CREATE TABLE preferences(name TEXT PRIMARY KEY,value TEXT NOT NULL)")
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                error("No approved migration")
            }

            override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                error("Newer save format")
            }
        }
    private val db
        get() = helper.writableDatabase

    private fun <T> transaction(block: () -> T): T {
        val database = db
        database.beginTransaction()
        try {
            val v = block()
            database.setTransactionSuccessful()
            return v
        } finally {
            database.endTransaction()
        }
    }

    private fun readCurrent(): SoloMatch? =
        db.rawQuery(
                "SELECT m.payload FROM matches m JOIN slots s ON s.match_id=m.id WHERE s.mode='SOLO'",
                null,
            )
            .use { if (it.moveToFirst()) MatchCodec.decode(it.getBlob(0)) else null }

    @Synchronized
    fun hasSlot(): Boolean =
        db.rawQuery("SELECT 1 FROM slots WHERE mode='SOLO'", null).use { it.moveToFirst() }

    @Synchronized
    fun load(): SoloMatch? {
        val match = readCurrent() ?: return null
        if (hasPending(match.id)) throw PendingSave()
        return match
    }

    private fun hasPending(id: String) =
        db.rawQuery("SELECT 1 FROM pending WHERE match_id=?", arrayOf(id)).use { it.moveToFirst() }

    private fun checkCurrent(m: SoloMatch): SoloMatch {
        val stored = readCurrent() ?: error("Missing save")
        require(
            stored.id == m.id &&
                stored.revision == m.revision &&
                MatchCodec.digest(stored.state) == MatchCodec.digest(m.state)
        ) {
            "Stale match"
        }
        if (hasPending(m.id)) throw PendingSave()
        return stored
    }

    private fun write(m: SoloMatch) {
        db.execSQL(
            "INSERT OR REPLACE INTO matches(id,payload) VALUES(?,?)",
            arrayOf(m.id, MatchCodec.encode(m)),
        )
    }

    @Synchronized
    fun start(
        human: Side,
        difficulty: Difficulty,
        expected: SoloMatch?,
        replaceInvalid: Boolean = false,
    ): SoloMatch = transaction {
        val old =
            try {
                readCurrent()
            } catch (e: Exception) {
                if (!replaceInvalid) throw e else null
            }
        if (!replaceInvalid)
            require(old?.id == expected?.id && old?.revision == expected?.revision) {
                "Save changed during setup"
            }
        val next =
            SoloMatch(UUID.randomUUID().toString(), human, difficulty, 0, GameEngine.newGame())
        // Insert a new record; do not mutate the protected slot until the transaction succeeds.
        write(next)
        db.execSQL("INSERT OR REPLACE INTO slots(mode,match_id) VALUES('SOLO',?)", arrayOf(next.id))
        db.execSQL("DELETE FROM pending WHERE match_id NOT IN (SELECT match_id FROM slots)")
        db.execSQL("DELETE FROM matches WHERE id NOT IN (SELECT match_id FROM slots)")
        db.execSQL(
            "INSERT OR REPLACE INTO preferences(name,value) VALUES('difficulty',?)",
            arrayOf(difficulty.name),
        )
        next
    }

    @Synchronized
    fun apply(match: SoloMatch, action: Action, actor: Actor): SoloMatch {
        val current = checkCurrent(match)
        require(
            current.state.sideToMove ==
                if (actor == Actor.HUMAN) current.human else current.human.other()
        ) {
            "Wrong actor"
        }
        require(GameEngine.apply(current.state, action) is Transition.Applied) { "Illegal action" }
        return mutate(current, "ACTION", actor.name, MatchCodec.encodeAction(action))
    }

    @Synchronized
    fun undo(match: SoloMatch): SoloMatch {
        val current = checkCurrent(match)
        val prefix =
            GameEngine.undoPrefix(current.state, current.human) ?: error("Undo unavailable")
        return mutate(
            current,
            "UNDO",
            Actor.HUMAN.name,
            java.nio.ByteBuffer.allocate(4).putInt(prefix).array(),
        )
    }

    private fun mutate(
        current: SoloMatch,
        kind: String,
        actor: String,
        payload: ByteArray,
    ): SoloMatch {
        val op = UUID.randomUUID().toString()
        fault(SavePoint.BEFORE_INTENT)
        transaction {
            db.execSQL(
                "INSERT INTO pending(id,match_id,base_revision,base_digest,kind,actor,payload) VALUES(?,?,?,?,?,?,?)",
                arrayOf(
                    op,
                    current.id,
                    current.revision,
                    MatchCodec.digest(current.state),
                    kind,
                    actor,
                    payload,
                ),
            )
        }
        fault(SavePoint.AFTER_INTENT)
        return finishPending(current)
    }

    private fun finishPending(current: SoloMatch): SoloMatch {
        val next = transaction {
            val result =
                db.rawQuery(
                        "SELECT base_revision,base_digest,kind,actor,payload FROM pending WHERE match_id=?",
                        arrayOf(current.id),
                    )
                    .use { c ->
                        require(c.moveToFirst())
                        require(
                            c.getLong(0) == current.revision &&
                                c.getString(1) == MatchCodec.digest(current.state)
                        ) {
                            "Pending base mismatch"
                        }
                        val result: GameState =
                            when (c.getString(2)) {
                                "ACTION" -> {
                                    val actor = Actor.valueOf(c.getString(3))
                                    require(
                                        current.state.sideToMove ==
                                            if (actor == Actor.HUMAN) current.human
                                            else current.human.other()
                                    )
                                    (GameEngine.apply(
                                            current.state,
                                            MatchCodec.decodeAction(c.getBlob(4)),
                                        ) as? Transition.Applied)
                                        ?.state ?: error("Invalid pending action")
                                }
                                "UNDO" -> {
                                    require(c.getString(3) == Actor.HUMAN.name)
                                    val bytes = c.getBlob(4)
                                    require(bytes.size == 4)
                                    val prefix = java.nio.ByteBuffer.wrap(bytes).int
                                    require(
                                        prefix ==
                                            GameEngine.undoPrefix(current.state, current.human)
                                    ) {
                                        "Invalid Undo intent"
                                    }
                                    (GameEngine.replay(current.state.events.take(prefix))
                                            as Transition.Applied)
                                        .state
                                }
                                else -> error("Unknown pending operation")
                            }
                        current.copy(revision = Math.addExact(current.revision, 1), state = result)
                    }
            // UPDATE preserves the foreign-key references from slots and pending.
            db.execSQL(
                "UPDATE matches SET payload=? WHERE id=?",
                arrayOf(MatchCodec.encode(result), result.id),
            )
            fault(SavePoint.DURING_COMMIT)
            db.execSQL("DELETE FROM pending WHERE match_id=?", arrayOf(current.id))
            result
        }
        fault(SavePoint.AFTER_COMMIT)
        return next
    }

    @Synchronized
    fun recover(): SoloMatch? {
        val current = readCurrent() ?: return null
        return if (hasPending(current.id)) finishPending(current) else current
    }

    @Synchronized
    fun preference(name: String, default: String): String =
        db.rawQuery("SELECT value FROM preferences WHERE name=?", arrayOf(name)).use {
            if (it.moveToFirst()) it.getString(0) else default
        }

    @Synchronized
    fun setPreference(name: String, value: String) {
        db.execSQL(
            "INSERT OR REPLACE INTO preferences(name,value) VALUES(?,?)",
            arrayOf(name, value),
        )
    }

    override fun close() {
        helper.close()
    }
}
