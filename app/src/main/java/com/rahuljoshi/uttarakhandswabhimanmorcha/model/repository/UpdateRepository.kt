package com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.UpdateInfo
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun fetchLatestVersion() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "fetchLatestVersion: trying $TAG")
            val doc = firestore.collection("app_metadata")
                .document("latest_version")
                .get()
                .await()

            val updateInfo = UpdateInfo(
                versionCode = doc.getLong("versionCode")?.toInt() ?: 0,
                versionName = doc.getString("versionName") ?: "",
                releaseNotes = doc.getString("releaseNotes") ?: "",
                downloadUrl = doc.getString("downloadUrl") ?: "",
                forceUpdate = doc.getBoolean("forceUpdate") ?: true
            )

            Resource.Success(updateInfo)
        } catch (e: Exception) {
            Log.d(TAG, "fetchLatestVersion: caught ${e.message} in $TAG")
            Resource.Error("${e.stackTrace}")
        }
    }

    companion object {
        private const val TAG = "UpdateRepository"
    }
}