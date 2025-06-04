package com.rahuljoshi.uttarakhandswabhimanmorcha.model.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import java.io.Serializable

@Keep
data class Complaint(
    val complaintId: String = "",
    val complaintTitle: String = "",
    val complaintDescription: String = "",
    val userId: String? = null,
    val userTempId: String? = null,
    val isAnonymous: Boolean = true,
    val timestamp: Timestamp = Timestamp.now(),
    val district: String = "",
    val tehsil: String = "",
    val village: String = "",
    val fileUrl: String? = null,
    val personName: String? = null,
    val phoneNumber: String? = null,
    val deviceId: String = "",
    val ipAddress: String = ""
): Serializable

