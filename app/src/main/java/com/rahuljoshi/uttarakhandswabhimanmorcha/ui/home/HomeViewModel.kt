package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NetworkHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    networkHelper: NetworkHelper
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = networkHelper.isConnected

    // Sealed class to represent the result of callService or openSocialAccount
    sealed class ActionResult {
        data class Success(val intent: Intent) : ActionResult()
        data class Error(val message: String) : ActionResult()
    }

    private val _actionResult = MutableStateFlow<ActionResult?>(null)
    val actionResult: StateFlow<ActionResult?> get() = _actionResult

    fun callService(type: String, context: Context) {
        Log.d(TAG, "callService: Preparing to dial $type in $TAG")
        viewModelScope.launch {
            try {
                // Resolve intent on a background thread to avoid potential I/O delays
                val intent = withContext(Dispatchers.IO) {
                    Intent(Intent.ACTION_CALL, Uri.parse("$type")).apply {
                        // Check if there's an app to handle the call intent
                        if (context.packageManager.queryIntentActivities(this, 0).isEmpty()) {
                            throw ActivityNotFoundException("No dialer app found for $type")
                        }
                    }
                }
                _actionResult.value = ActionResult.Success(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "callService: Failed to dial $type", e)
                _actionResult.value = ActionResult.Error("No dialer app found")
            }
        }
    }

    fun openSocialAccount(url: String, context: Context) {
        Log.d(TAG, "openSocialAccount: Preparing to open $url in $TAG")
        viewModelScope.launch {
            try {
                // Resolve intent on a background thread to avoid potential I/O delays
                val intent = withContext(Dispatchers.IO) {
                    Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                        // Check if there's an app to handle the URL intent
                        if (context.packageManager.queryIntentActivities(this, 0).isEmpty()) {
                            throw ActivityNotFoundException("No app found for $url")
                        }
                    }
                }
                _actionResult.value = ActionResult.Success(intent)
            } catch (e: Exception) {
                Log.e(TAG, "openSocialAccount: Failed to open $url", e)
                _actionResult.value = ActionResult.Error("Failed to open the link")
            }
        }
    }

    // Reset the action result after it's handled
    fun resetActionResult() {
        _actionResult.value = null
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}