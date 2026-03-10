package dev.aiaerial.signal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.aiaerial.signal.data.model.NetworkEvent

@Database(
    entities = [NetworkEvent::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun networkEventDao(): NetworkEventDao

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
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            // Add migrations here as schema evolves, e.g.:
            // MIGRATION_1_2,
        )
    }
}
