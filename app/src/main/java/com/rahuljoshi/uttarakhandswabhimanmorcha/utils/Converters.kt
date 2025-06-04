package com.rahuljoshi.uttarakhandswabhimanmorcha.utils

import androidx.room.TypeConverter
import com.google.firebase.Timestamp

class Converters {

    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.seconds
    }

    @TypeConverter
    fun toTimestamp(seconds: Long?): Timestamp? {
        return seconds?.let { Timestamp(it, 0) }
    }
}
