package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NetworkHelper
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintEntity
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.ComplaintLocalRepository
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.Repository
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject;

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: Repository,
    private val complaintLocalRepository: ComplaintLocalRepository,
    networkHelper: NetworkHelper
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = networkHelper.isConnected

    private val _status = MutableLiveData<Boolean>()
    val status: LiveData<Boolean> = _status

    private val _registrationMessage = MutableLiveData<String>()
    val registrationMessage: LiveData<String> = _registrationMessage

    fun currentUser(): String? {
        return repository.currentUserId()
    }

    private val _resetPasswordState = MutableLiveData<Resource<Unit>>()
    val resetPasswordState: LiveData<Resource<Unit>> = _resetPasswordState
    internal fun forgetPassword(email: String) {
        Log.d(TAG, "resetPassword: reset password for $email in ${TAG}")
        viewModelScope.launch {
            _resetPasswordState.value = repository.forgetPassword(email)
        }
    }

    private val _registerState = MutableLiveData<Boolean>()
    val registerState: LiveData<Boolean> = _registerState

    internal fun createUser(email: String, password: String, user: User) {
        Log.d(TAG, "createUser: create user from view model")
        _status.value = true
        _registrationMessage.value = "Creating account..."
        viewModelScope.launch {
            try {
                val result = repository.createUserUsingEmailAndPassword(email, password)
                if (result is Resource.Success) {
                    val uid = currentUser()
                    if (uid != null) {
                        val updateUser = user.copy(uid = uid)
                        uploadTheUser(updateUser)
                    } else {
                        Log.e(TAG, "createUser: User ID is null after successful registration")
                        _status.value = false
                        _registerState.value = false
                        _registrationMessage.value = "Registration failed. Please try again."
                    }
                } else {
                    Log.e(TAG, "createUser: Registration failed - ${result.message}")
                    _status.value = false
                    _registerState.value = false
                    _registrationMessage.value = "Registration failed. Please try again."
                }
            } catch (e: Exception) {
                Log.e(TAG, "createUser: Exception during registration", e)
                _status.value = false
                _registerState.value = false
                _registrationMessage.value = "Registration failed. Please try again."
            }
        }
    }

    private fun uploadTheUser(user: User) {
        _status.value = true
        _registrationMessage.value = "Saving user details..."
        Log.d(TAG, "uploadTheUser: uploading the user from view model")
        viewModelScope.launch {
            try {
                val response = repository.storeUserInFirestore(user)
                when (response) {
                    is Resource.Success -> {
                        _registrationMessage.value = "Sending confirmation email..."
                        try {
                            repository.sendJoinMemberEmail(user)
                            _status.value = false
                            _registerState.value = true
                            _registrationMessage.value = "Registration successful!"
                        } catch (e: Exception) {
                            Log.e(TAG, "uploadTheUser: Failed to send registration email", e)
                            _status.value = false
                            _registerState.value = false
                            _registrationMessage.value = "Failed to send confirmation email. Please try again."
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "uploadTheUser: Failed to store user in Firestore - ${response.message}")
                        try {
                            repository.auth.currentUser?.delete()?.await()
                        } catch (e: Exception) {
                            Log.e(TAG, "uploadTheUser: Failed to clean up Firebase Auth user", e)
                        }
                        _status.value = false
                        _registerState.value = false
                        _registrationMessage.value = "Failed to save user details. Please try again."
                    }
                    else -> {
                        Log.e(TAG, "uploadTheUser: Unknown response type")
                        _status.value = false
                        _registerState.value = false
                        _registrationMessage.value = "Registration failed. Please try again."
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadTheUser: Exception during user upload", e)
                _status.value = false
                _registerState.value = false
                _registrationMessage.value = "Registration failed. Please try again."
            }
        }
    }

    private val _loginState = MutableLiveData<Boolean>()
    val loginState: LiveData<Boolean> = _loginState
    internal fun signIn(email: String, password: String) {
        Log.d(TAG, "signInUser: sign in user from view model")
        _status.value = true  // Show loading
        viewModelScope.launch {
            // First check if email exists in Firestore
            val emailExists = repository.checkEmailExistence(email)
            if (!emailExists) {
                _status.value = false
                _loginState.value = false
                return@launch
            }

            // Proceed with sign in if email exists
            val result = repository.signIn(email, password)
            //refreshToken()
            when (result) {
                is Resource.Success -> {
                    _status.value = false
                    _loginState.value = true // Allow navigation
                }

                is Resource.Error -> {  // Handle login errors
                    _status.value = false
                    _loginState.value = false
                }

                else -> {
                    _status.value = false
                    _loginState.value = false
                }
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut: sign out from ${TAG}")
        viewModelScope.launch {
            try {
                //removeToken()
                repository.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "signOut: Error during sign out", e)
            }
        }
    }
    
    
    fun isValidEmail(email: String): Boolean {
        Log.d(TAG, "isValidEmail: $email in $TAG")
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun checkLengthOfPassword(password: String): Boolean {
        Log.d(TAG, "checkLengthOfPassword: in $TAG")
        return password.length >= 6
    }

    fun checkEmptiness(email: String, password: String): Boolean {
        Log.d(TAG, "checkEmptiness: in $TAG")
        return email.isEmpty() || password.isEmpty()
    }

    private val _syncStatus = MutableLiveData<Boolean>()
    val syncStatus: LiveData<Boolean> get() = _syncStatus

    fun syncLocalComplaintsToFirestore() {
        Log.d(TAG, "syncLocalComplaintsToFirestore: in $TAG")
        val userId = currentUser()

        viewModelScope.launch {
            try {
                _syncStatus.postValue(true) // Show progress dialog
                val localComplaints = complaintLocalRepository.getAllComplaints()
                
                if (localComplaints.isEmpty()) {
                    Log.d(TAG, "No local complaints to sync")
                    _syncStatus.postValue(false)
                    return@launch
                }

                var uploadedCount = 0
                val totalCount = localComplaints.size
                var retryCount = 0
                val maxRetries = 3

                while (uploadedCount < totalCount && retryCount < maxRetries) {
                    for (entity in localComplaints) {
                        try {
                            val complaint = entity.toComplaint(userId.toString())
                            val success = repository.uploadComplaintAndSetId(complaint)
                            
                            if (success) {
                                uploadedCount++
                                Log.d(TAG, "Successfully uploaded complaint ${uploadedCount}/${totalCount}")
                            } else {
                                Log.e(TAG, "Failed to upload complaint: ${complaint.complaintId}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error uploading complaint: ${e.message}")
                        }
                    }
                    
                    if (uploadedCount < totalCount) {
                        retryCount++
                        Log.w(TAG, "Retry attempt $retryCount: $uploadedCount/$totalCount complaints uploaded")
                        // Wait for 2 seconds before retrying
                        kotlinx.coroutines.delay(2000)
                    }
                }

                if (uploadedCount == totalCount) {
                    complaintLocalRepository.deleteAllComplaints()
                    Log.d(TAG, "All complaints synced and local DB cleared.")
                } else {
                    Log.w(TAG, "Failed to upload all complaints after $maxRetries attempts. $uploadedCount/$totalCount uploaded.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync: ${e.message}")
            } finally {
                _syncStatus.postValue(false) // Hide progress dialog
            }
        }
    }

    fun ComplaintEntity.toComplaint(userId: String): Complaint {
        return Complaint(
            complaintTitle = this.complaintTitle,
            complaintDescription = this.complaintDescription,
            complaintId = this.complaintId,
            deviceId = this.deviceId,
            ipAddress = this.ipAddress,
            phoneNumber = this.phoneNumber,
            personName = this.personName,
            timestamp = this.timestamp,
            district = this.district,
            tehsil = this.tehsil,
            village = this.village,
            isAnonymous = this.isAnonymous,
            userTempId = this.userTempId,
            userId = userId,
            fileUrl = this.fileUrl,
        )
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}