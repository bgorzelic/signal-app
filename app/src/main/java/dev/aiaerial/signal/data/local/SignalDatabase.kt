package dev.aiaerial.signal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.aiaerial.signal.data.model.NetworkEvent

@Database(
    entities = [NetworkEvent::class, ScanSnapshot::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun networkEventDao(): NetworkEventDao
    abstract fun scanSnapshotDao(): ScanSnapshotDao

    companion object {
        /**
         * Migration registry. Add new migrations here as schema evolves.
         *
         * Example for v1 → v2 (adding a 'notes' column):
         *   val MIGRATION_1_2 = Migration(1, 2) {
         *       it.execSQL("ALTER TABLE network_events ADD COLUMN notes TEXT")
         *   }
         *
         * Then register in DatabaseModule: .addMigrations(MIGRATION_1_2)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scan_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        ssid TEXT,
                        bssid TEXT,
                        rssi INTEGER,
                        networkCount INTEGER NOT NULL,
                        dataJson TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
        )
    }
}
