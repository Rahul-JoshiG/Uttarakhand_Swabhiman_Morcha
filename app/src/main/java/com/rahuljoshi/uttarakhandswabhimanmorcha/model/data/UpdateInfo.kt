package com.rahuljoshi.uttarakhandswabhimanmorcha.model.data

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
