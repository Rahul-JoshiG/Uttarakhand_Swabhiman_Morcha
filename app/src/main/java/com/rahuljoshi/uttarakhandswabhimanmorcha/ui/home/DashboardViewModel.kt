package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NetworkHelper
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.Repository
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: Repository,
    networkHelper: NetworkHelper
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = networkHelper.isConnected

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private var isUserListenerStarted = false

    fun loadCurrentUserId() {
        Log.d(TAG, "loadCurrentUserId: loading user id in ${TAG}")
        viewModelScope.launch {
            val uid = repository.currentUserId()
            if (!uid.isNullOrBlank()) {
                Log.d(TAG, "loadCurrentUserId: uid = $uid")
                _currentUserId.value = uid
                if (!isUserListenerStarted) {
                    fetchUserDetails(uid)
                    isUserListenerStarted = true
                }
            } else {
                Log.e(TAG, "loadCurrentUserId: uid is null or blank")
            }
        }
    }

    internal fun fetchUserDetails(uid: String) {
        Log.d(TAG, "fetchUserDetails: for $uid in ${TAG}")
        viewModelScope.launch {
            repository.fetchCurrentUser(uid).collect { result ->
                when (result) {
                    is Resource.Success<*> -> {
                        val user = result.data
                        if (user != null) {
                            _currentUser.value = user
                            Log.d(
                                TAG,
                                "User fetched successfully: ${user.name}"
                            )
                        } else {
                            Log.e(TAG, "User data is null")
                        }
                    }

                    is Resource.Error<*> -> {
                        Log.e(TAG, "Failed to fetch user: ${result.message}")
                    }

                    is Resource.Loading<*> -> {
                        Log.d(TAG, "Loading user data...")
                    }
                }
            }
        }
    }
    companion object {
        private const val TAG = "DashboardViewModel"
    }
}