package com.rahuljoshi.uttarakhandswabhimanmorcha.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val complaintId: String = "",
    val complaintTitle: String = "",
    val complaintDescription: String = "",
    val userId: String? = null,
    val userTempId: String? = null,
    val isAnonymous: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
    val district: String = "",
    val tehsil: String = "",
    val village: String = "",
    val fileUrl: String? = null,
    val personName: String? = null,
    val phoneNumber: String? = null,
    val deviceId: String = "",
    val ipAddress: String = ""
)
