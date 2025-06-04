package com.rahuljoshi.uttarakhandswabhimanmorcha.model.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Converters

@Database(entities = [ComplaintEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class ComplaintDatabase : RoomDatabase() {
    abstract fun complaintDao(): ComplaintDao
}
