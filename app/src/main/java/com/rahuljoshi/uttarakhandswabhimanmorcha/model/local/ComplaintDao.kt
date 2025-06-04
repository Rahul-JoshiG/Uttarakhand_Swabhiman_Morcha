package com.rahuljoshi.uttarakhandswabhimanmorcha.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ComplaintDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Query("DELETE FROM complaints")
    suspend fun deleteAllComplaints()

    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    suspend fun getAllComplaints(): List<ComplaintEntity>
}
