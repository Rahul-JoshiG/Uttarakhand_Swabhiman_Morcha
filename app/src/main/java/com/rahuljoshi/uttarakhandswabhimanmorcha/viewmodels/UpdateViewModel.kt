package com.rahuljoshi.uttarakhandswabhimanmorcha.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.UpdateInfo
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.UpdateRepository
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _updateInfo = MutableLiveData<UpdateInfo?>()
    val updateInfo: LiveData<UpdateInfo?> = _updateInfo

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> = _downloadProgress

    val currentVersionCode = context.packageManager
        .getPackageInfo(context.packageName, 0).versionCode


    fun checkForUpdate() {
        viewModelScope.launch {
            val result = repository.fetchLatestVersion()
            when(result){
                is Resource.Success ->{
                    val latest = result.data
                    if (latest != null && latest.versionCode > currentVersionCode) {
                        _updateInfo.value = latest
                    }
                }

                is Resource.Error<*> -> TODO()
                is Resource.Loading<*> -> TODO()
            }
        }
    }

    fun downloadApk(updateInfo: UpdateInfo, onFinish: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "update.apk")
                val url = URL(updateInfo.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val totalSize = connection.contentLength
                val input = BufferedInputStream(url.openStream())
                val output = FileOutputStream(file)
                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    _downloadProgress.postValue((total * 100 / totalSize).toInt())
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    onFinish(file)
                }
            } catch (e: Exception) {
                Log.e("UpdateViewModel", "Download failed", e)
                withContext(Dispatchers.Main) {
                    onFinish(null)
                }
            }
        }
    }
}
