package dev.aiaerial.signal.data.local

import androidx.room.TypeConverter
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.Vendor

class Converters {

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType =
        runCatching { EventType.valueOf(value) }.getOrDefault(EventType.UNKNOWN)

    @TypeConverter
    fun fromVendor(value: Vendor): String = value.name

    @TypeConverter
    fun toVendor(value: String): Vendor =
        runCatching { Vendor.valueOf(value) }.getOrDefault(Vendor.GENERIC)
}
