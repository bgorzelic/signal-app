package dev.aiaerial.signal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.aiaerial.signal.data.model.NetworkEvent

@Database(
    entities = [NetworkEvent::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun networkEventDao(): NetworkEventDao
}
