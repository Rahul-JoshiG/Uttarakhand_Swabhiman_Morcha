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
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintEntity
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.ComplaintLocalRepository
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
class NewComplaintViewModel @Inject constructor(
    private val repository: Repository,
    private val complaintRepository: ComplaintLocalRepository,
    networkHelper: NetworkHelper
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = networkHelper.isConnected
    
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

    fun submitComplaint(
        personName: String,
        phoneNumber: String,
        complaintTitle: String,
        complaintDescription: String,
        district: String?,
        block: String?,
        village: String,
        fileUrl: String?,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        var updatedUrl = ""
        updatedUrl = fileUrl ?: "N/A"

        val complaint = ComplaintEntity(
            complaintId = "",
            complaintTitle = complaintTitle,
            complaintDescription = complaintDescription,
            userId = null,
            userTempId = ShardPref.getUserTempId(),
            isAnonymous = false,
            timestamp = Timestamp.now(),
            district = district.toString(),
            tehsil = block.toString(),
            village = village,
            fileUrl = updatedUrl,
            personName = personName,
            phoneNumber = phoneNumber,
            deviceId = ShardPref.getDeviceId(Constant.DEVICE_ID),
            ipAddress = ShardPref.getIpId(Constant.IP_ADDRESS)
        )

        viewModelScope.launch {
            try {
                val tempComplaintId = System.currentTimeMillis().toString()
                complaintRepository.insertComplaint(complaint)
                Log.d(TAG, "submitComplaint: Complaint submitted with ID = $tempComplaintId")
                onSuccess(tempComplaintId)
            } catch (e: Exception) {
                Log.e(TAG, "submitComplaint: Failed with exception: ${e.message}", e)
                onFailure(e)
            }
        }
    }

    private val _status = MutableLiveData<Boolean>()
    val status: LiveData<Boolean> = _status

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
        private const val TAG = "NewComplaintViewModel"
    }
}