package com.rahuljoshi.uttarakhandswabhimanmorcha.model.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Keep
@Parcelize
@TypeParceler<Timestamp, TimestampParceler>
data class User(
    val uid: String? = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String =  "",
    val district: String = "",
    val tehsil: String = "",
    val village: String = "",
    val isAuthenticate: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
) : Parcelable

object TimestampParceler : Parceler<Timestamp> {
    override fun create(parcel: Parcel): Timestamp {
        return Timestamp(parcel.readLong(), parcel.readInt())
    }

    override fun Timestamp.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(seconds)
        parcel.writeInt(nanoseconds)
    }
}
