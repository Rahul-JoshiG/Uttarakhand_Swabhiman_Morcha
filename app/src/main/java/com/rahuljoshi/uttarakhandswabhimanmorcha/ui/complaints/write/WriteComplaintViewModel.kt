package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.complaints.write

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NetworkHelper
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.Repository
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class WriteComplaintViewModel @Inject constructor(
    private val repository: Repository,
    networkHelper: NetworkHelper
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = networkHelper.isConnected

    fun submitComplaint(
        title: String,
        description: String,
        fileUrl: String?,
        onSuccess: (String, Complaint) -> Unit,
        onFailure: (Exception) -> Unit,
        currentUser: User?
    ) {
        // Early exit if user is null (safety check)
        if (currentUser == null) {
            onFailure(IllegalArgumentException("User data is missing"))
            return
        }

        var updatedUrl = ""
        updatedUrl = fileUrl ?: "N/A"

        val complaint = Complaint(
            complaintId = "",
            complaintTitle = title,
            complaintDescription = description,
            userId = currentUser.uid,
            userTempId = null,
            isAnonymous = false,
            timestamp = Timestamp.now(),
            district = currentUser.district,
            tehsil = currentUser.tehsil,
            village = currentUser.village,
            fileUrl = updatedUrl,
            personName = currentUser.name,
            phoneNumber = currentUser.phoneNumber,
            deviceId = ShardPref.getDeviceId(Constant.DEVICE_ID),
            ipAddress = ShardPref.getIpId(Constant.IP_ADDRESS)
        )

        viewModelScope.launch {
            try {
                val docId = repository.submitComplaint(complaint)
                Log.d(TAG, "submitComplaint: Complaint submitted with ID = $docId")

                onSuccess(docId, complaint)
            } catch (e: Exception) {
                Log.e(TAG, "submitComplaint: Failed with exception: ${e.message}", e)
                onFailure(e)
            }
        }
    }

    private val _status = MutableLiveData<Boolean>()
    val status: LiveData<Boolean> = _status

    private val _uploadState = MutableLiveData<Resource<String>>()
    val uploadState: LiveData<Resource<String>> get() = _uploadState

    fun uploadFile(
        filePath: String,
        complaintName: String,
        userName: String,
        onResult: (Resource<String>) -> Unit
    ) {
        Log.d(TAG, "uploadFile: uploading file")
        _status.value = true
        viewModelScope.launch {
            val result = repository.uploadFileToDrive(
                filePath = filePath,
                complaintName = complaintName,
                userName = userName
            )
            Log.d(TAG, "uploadFile: result = $result")
            onResult(result)  // Callback used here
            _status.value = false
        }
    }


    internal fun deleteFile(file: String) {
        Log.d(TAG, "deleteFile: deleting upload file in ${TAG}")
        viewModelScope.launch {
            val result = repository.deleteFile(file)
            if (result is Resource.Success) {
                Log.d("Delete", "File deleted successfully")
            } else {
                Log.e("Delete", "Error: ${result.message}")
            }
        }
    }

    fun saveBitmapToCache(bitmap: Bitmap, context: Context, callback: (Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val file =
                        File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file
                }
                callback(Uri.fromFile(file))
            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    fun sendComplaintEmail(complaint: Complaint) {
        Log.d(TAG, "sendComplaintEmail: in $TAG")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "sendComplaintEmail: trying to send email in $TAG")
                repository.sendComplaintEmail(complaint)
                Log.d(TAG, "sendComplaintEmail: Email sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "sendComplaintEmail: Failed to send email - ${e.message}")
                Log.e(TAG, "sendComplaintEmail: Stack trace - ${e.stackTraceToString()}")
                // You might want to notify the UI about the email failure
                // For example, through a LiveData or callback
            }
        }
    }

    companion object {
        private const val TAG = "WriteComplaintViewModel"
    }
}
