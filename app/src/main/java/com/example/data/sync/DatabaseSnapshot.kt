package com.example.data.sync

import com.example.data.local.OrthoDatabase
import com.example.data.telegram.DriveLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Serialises every user-owned Room table to JSON and restores it again.
 *
 * Deliberately generic — it reads the table list out of `sqlite_master` and walks
 * columns by cursor metadata rather than through typed DAOs. Two reasons:
 *
 *  1. It covers every table the app has, including ones added later, without a
 *     matching code change in the sync layer. Patients, surgeries, timeline
 *     events, clinical notes, bookmarks, learned suggestions and the imported
 *     bone atlas are all carried by the same mechanism.
 *  2. It cannot drift out of step with the entity classes, which is exactly the
 *     failure mode that turns a restore into silent data loss.
 *
 * Restore is INSERT OR REPLACE inside one transaction, per-row keyed by the
 * table's own primary key, so a partial snapshot merges instead of clobbering.
 */
class DatabaseSnapshot(private val database: OrthoDatabase) {

    /** Tables owned by the sync layer itself; snapshotting them would recurse. */
    private val excluded = setOf(
        "remote_objects",
        "transfer_queue",
        "sync_tombstones",
        "room_master_table",
        "sqlite_sequence",
        "android_metadata"
    )

    suspend fun export(destination: File): SnapshotSummary = withContext(Dispatchers.IO) {
        val db = database.openHelper.readableDatabase
        val root = JSONObject()
        root.put("format", FORMAT_VERSION)
        root.put("createdAt", System.currentTimeMillis())

        val tables = JSONObject()
        var totalRows = 0
        for (table in tableNames()) {
            val rows = JSONArray()
            db.query("SELECT * FROM `$table`").use { cursor ->
                val columns = cursor.columnNames
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    columns.forEachIndexed { index, column ->
                        when {
                            cursor.isNull(index) -> row.put(column, JSONObject.NULL)
                            else -> row.put(column, cursor.getString(index))
                        }
                    }
                    rows.put(row)
                    totalRows++
                }
            }
            tables.put(table, rows)
        }
        root.put("tables", tables)

        destination.parentFile?.mkdirs()
        destination.writeText(root.toString())
        DriveLog.i(TAG, "Snapshot exported: $totalRows rows across ${tables.length()} tables")
        SnapshotSummary(
            tableCount = tables.length(),
            rowCount = totalRows,
            sizeBytes = destination.length(),
            createdAt = root.getLong("createdAt")
        )
    }

    /**
     * Merges [source] into the live database. Existing rows with the same primary
     * key are replaced; rows present locally but absent from the snapshot are
     * kept, because a snapshot taken on another device before a local capture
     * must never erase that capture.
     */
    suspend fun import(source: File): SnapshotSummary = withContext(Dispatchers.IO) {
        require(source.exists()) { "Snapshot file does not exist" }
        val root = JSONObject(source.readText())
        val format = root.optInt("format", 0)
        require(format <= FORMAT_VERSION) {
            "Snapshot was written by a newer version of Ortho Tips (format $format)"
        }
        val tables = root.optJSONObject("tables") ?: JSONObject()
        val known = tableNames().toSet()
        val db = database.openHelper.writableDatabase

        var totalRows = 0
        var importedTables = 0
        db.beginTransaction()
        try {
            val names = tables.keys()
            while (names.hasNext()) {
                val table = names.next()
                if (table !in known) {
                    DriveLog.w(TAG, "Snapshot table '$table' is not in this schema; skipped")
                    continue
                }
                val rows = tables.optJSONArray(table) ?: continue
                if (rows.length() == 0) continue
                importedTables++
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val columns = row.keys().asSequence().toList()
                    if (columns.isEmpty()) continue
                    val placeholders = columns.joinToString(",") { "?" }
                    val columnList = columns.joinToString(",") { "`$it`" }
                    val values = columns.map { column ->
                        if (row.isNull(column)) null else row.getString(column)
                    }.toTypedArray()
                    db.execSQL(
                        "INSERT OR REPLACE INTO `$table` ($columnList) VALUES ($placeholders)",
                        values
                    )
                    totalRows++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        DriveLog.i(TAG, "Snapshot imported: $totalRows rows into $importedTables tables")
        SnapshotSummary(
            tableCount = importedTables,
            rowCount = totalRows,
            sizeBytes = source.length(),
            createdAt = root.optLong("createdAt", 0)
        )
    }

    /** Reads snapshot metadata without applying it, for the conflict prompt. */
    suspend fun inspect(source: File): SnapshotSummary? = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(source.readText())
            val tables = root.optJSONObject("tables") ?: JSONObject()
            var rows = 0
            tables.keys().forEach { rows += tables.optJSONArray(it)?.length() ?: 0 }
            SnapshotSummary(
                tableCount = tables.length(),
                rowCount = rows,
                sizeBytes = source.length(),
                createdAt = root.optLong("createdAt", 0)
            )
        }.getOrNull()
    }

    private fun tableNames(): List<String> {
        val db = database.openHelper.readableDatabase
        val names = mutableListOf<String>()
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                    "AND name NOT LIKE 'room_%'"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                if (name !in excluded) names.add(name)
            }
        }
        return names.sorted()
    }

    companion object {
        private const val TAG = "DatabaseSnapshot"
        const val FORMAT_VERSION = 1
    }
}

data class SnapshotSummary(
    val tableCount: Int,
    val rowCount: Int,
    val sizeBytes: Long,
    val createdAt: Long
)
