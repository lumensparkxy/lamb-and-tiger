package com.maswadkar.lambandtiger

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.maswadkar.lambandtiger.data.*
import com.maswadkar.lambandtiger.game.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class StorageTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun name() = "test-${java.util.UUID.randomUUID()}.sqlite"

    @Test
    fun restartAndBothHumanRoles() {
        for (side in Side.entries) {
            val name = name()
            var m: SoloMatch
            MatchStore(context, name).use { store ->
                m = store.start(side, Difficulty.HARD, null)
                m =
                    store.apply(
                        m,
                        Action.Place(Node.B),
                        if (side == Side.LAMB) Actor.HUMAN else Actor.COMPUTER,
                    )
            }
            MatchStore(context, name).use { store ->
                val loaded = store.load()!!
                assertEquals(1, loaded.state.eventCount)
                assertEquals(side, loaded.human)
                assertEquals(Difficulty.HARD, loaded.difficulty)
                assertEquals(m.id, loaded.id)
            }
        }
    }

    @Test
    fun everyCrashBoundaryRecoversExactlyOnce() {
        for (point in SavePoint.entries) {
            val name = name()
            var armed = false
            MatchStore(context, name) {
                    if (armed && it == point) throw java.io.IOException("Injected $point")
                }
                .use { store ->
                    val m = store.start(Side.LAMB, Difficulty.EASY, null)
                    armed = true
                    try {
                        store.apply(m, Action.Place(Node.B), Actor.HUMAN)
                        fail("Fault not reached")
                    } catch (_: java.io.IOException) {}
                }
            MatchStore(context, name).use { store ->
                if (point == SavePoint.AFTER_INTENT || point == SavePoint.DURING_COMMIT)
                    try {
                        store.load()
                        fail("Pending was playable")
                    } catch (_: PendingSave) {}
                val m = store.recover()!!
                assertEquals(if (point == SavePoint.BEFORE_INTENT) 0 else 1, m.state.eventCount)
                assertEquals(m.revision, store.recover()!!.revision)
                assertEquals(m.state.events, store.load()!!.state.events)
            }
        }
    }

    @Test
    fun undoAndStaleActions() {
        MatchStore(context, name()).use { store ->
            val original = store.start(Side.LAMB, Difficulty.EASY, null)
            var m = store.apply(original, Action.Place(Node.B), Actor.HUMAN)
            m = store.apply(m, Action.Step(Node.A, Node.C), Actor.COMPUTER)
            val undone = store.undo(m)
            assertEquals(0, undone.state.eventCount)
            assertEquals(1, undone.state.repetitions.values.sum())
            assertEquals(3, undone.revision)
            try {
                store.apply(original, Action.Place(Node.C), Actor.HUMAN)
                fail()
            } catch (_: IllegalArgumentException) {}
            assertEquals(3, store.load()!!.revision)
        }
    }

    @Test
    fun replacementAndPreferenceIsolation() {
        val name = name()
        MatchStore(context, name).use { store ->
            val m = store.start(Side.LAMB, Difficulty.MEDIUM, null)
            store.setPreference("dark", "true")
            assertEquals(m.id, store.load()!!.id)
            try {
                store.start(Side.TIGER, Difficulty.EASY, null)
                fail()
            } catch (_: IllegalArgumentException) {}
            assertEquals(m.id, store.load()!!.id)
            // Reserve an independent slot as a sentinel; Solo replacement must not touch it.
            val file = File(context.noBackupFilesDir, name)
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL(
                    "INSERT INTO matches(id,payload) VALUES('nearby-sentinel',?)",
                    arrayOf(byteArrayOf(7, 8, 9)),
                )
                db.execSQL("INSERT INTO slots(mode,match_id) VALUES('NEARBY','nearby-sentinel')")
            }
            val next = store.start(Side.TIGER, Difficulty.EXPERT, m)
            assertNotEquals(m.id, next.id)
            assertEquals("true", store.preference("dark", "false"))
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery(
                        "SELECT m.payload FROM matches m JOIN slots s ON s.match_id=m.id WHERE s.mode='NEARBY'",
                        null,
                    )
                    .use {
                        assertTrue(it.moveToFirst())
                        assertArrayEquals(byteArrayOf(7, 8, 9), it.getBlob(0))
                    }
            }
        }
    }

    @Test
    fun damagedAndNewerSavesStayPreserved() {
        val name = name()
        val file = File(context.noBackupFilesDir, name)
        MatchStore(context, name).use { it.start(Side.LAMB, Difficulty.EASY, null) }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("UPDATE matches SET payload=?", arrayOf(byteArrayOf(0, 0, 0, 99)))
        }
        MatchStore(context, name).use { store ->
            try {
                store.load()
                fail()
            } catch (_: IllegalArgumentException) {}
            try {
                store.recover()
                fail()
            } catch (_: IllegalArgumentException) {}
        }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.version = 2
        }
        MatchStore(context, name).use { store ->
            try {
                store.load()
                fail()
            } catch (_: IllegalStateException) {}
        }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use {
            assertEquals(2, it.version)
        }
    }
}
