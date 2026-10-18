package com.maswadkar.lambandtiger.friend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/** Independent Nearby slot; never rewrites or deletes the Solo database. */
internal class FriendStore(context: Context) : FriendPersistence, AutoCloseable {
    private val helper = object : SQLiteOpenHelper(context, File(context.noBackupFilesDir, "nearby.sqlite").path, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE identity(id TEXT NOT NULL)")
            db.execSQL("INSERT INTO identity VALUES(?)", arrayOf(uid()))
            db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY,payload BLOB NOT NULL)")
            db.execSQL("CREATE TABLE slots(kind TEXT PRIMARY KEY,id TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unsupported Nearby migration") }
        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Newer Nearby database") }
    }
    override val player get() = helper.readableDatabase.rawQuery("SELECT id FROM identity", null).use { check(it.moveToFirst()); it.getString(0) }
    override fun load(): FriendRecord? = helper.readableDatabase.rawQuery(
        "SELECT r.payload FROM records r JOIN slots s ON s.id=r.id ORDER BY CASE s.kind WHEN 'recovery' THEN 0 ELSE 1 END LIMIT 1", null
    ).use { if (it.moveToFirst()) FriendRecord.decode(it.getBlob(0)) else null }
    override fun save(record: FriendRecord, promote: Boolean) {
        val db = helper.writableDatabase
        val bytes = record.bytes()
        db.beginTransaction()
        try {
            db.execSQL("INSERT OR REPLACE INTO records VALUES(?,?)", arrayOf(record.match.id, bytes))
            if ((record.tx != null && record.tx.stage != Stage.ACKED) || (!record.started && record.requests.keys.any { it.startsWith("ready:") }))
                db.execSQL("INSERT OR REPLACE INTO slots VALUES('recovery',?)", arrayOf(record.match.id))
            else db.execSQL("DELETE FROM slots WHERE kind='recovery' AND id=?", arrayOf(record.match.id))
            if (promote) db.execSQL("INSERT OR REPLACE INTO slots VALUES('current',?)", arrayOf(record.match.id))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    override fun close() = helper.close()
}
