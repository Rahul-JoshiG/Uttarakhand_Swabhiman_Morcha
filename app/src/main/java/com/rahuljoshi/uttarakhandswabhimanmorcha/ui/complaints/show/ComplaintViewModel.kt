package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.complaints.show

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NetworkHelper
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintEntity
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.ComplaintLocalRepository
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.repository.Repository
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintViewModel @Inject constructor(
    private val repository: Repository,
    private val complaintLocalRepository: ComplaintLocalRepository,
    networkHelper: NetworkHelper
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = networkHelper.isConnected

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _complaintState = MutableStateFlow<List<Complaint>>(emptyList())
    val complaintState: StateFlow<List<Complaint>> = _complaintState

    fun observeComplaints(uid: String) {
        Log.d(TAG, "observeComplaints: observing complaints for $uid in $TAG")
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchAllComplaintsRealtime(uid).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        if (result.data != null) {
                            _complaintState.value = result.data
                            Log.d(TAG, "observeComplaints: ${result.data} in $TAG ")
                            _isLoading.value = false
                        } else {
                            Log.d(TAG, "Firestore returned empty list")
                            // Optionally: Don’t update state if you want to avoid flickering UI
                            _complaintState.value = emptyList()
                        }
                    }

                    is Resource.Error -> {
                        Log.d(TAG, "Error fetching data: ${result.message}")
                        _complaintState.value = emptyList()
                        _isLoading.value = false
                    }

                    is Resource.Loading<*> -> TODO()
                }
            }
        }
    }

    fun observeAnonymousComplaints(userTempId: String) {
        Log.d(TAG, "observeComplaints: observing complaints $userTempId in $TAG")
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchAllAnonymousComplaintsRealtime(userTempId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        if (result.data != null) {
                            _complaintState.value = result.data
                            Log.d(TAG, "observeComplaints: ${result.data} in $TAG ")
                            _isLoading.value = false
                        } else {
                            Log.d(TAG, "Firestore returned empty list")
                            // Optionally: Don’t update state if you want to avoid flickering UI
                            _complaintState.value = emptyList()
                        }
                    }

                    is Resource.Error -> {
                        Log.d(TAG, "Error fetching data: ${result.message}")
                        _complaintState.value = emptyList()
                        _isLoading.value = false
                    }

                    is Resource.Loading<*> -> TODO()
                }
            }
        }
    }

    private val _complaintLocalState = MutableStateFlow<List<ComplaintEntity>>(emptyList())
    val complaintLocalState: StateFlow<List<ComplaintEntity>> = _complaintLocalState

    fun observeLocalComplaints() {
        Log.d(TAG, "observeComplaints: observing local complaints in $TAG")
        viewModelScope.launch {
            _isLoading.value = true
            val response = complaintLocalRepository.getAllComplaints()
            if (!response.isEmpty()) {
                _complaintLocalState.value = response
                Log.d(TAG, "observeComplaints: ${response.size} in $TAG ")
                _isLoading.value = false
            } else {
                _complaintLocalState.value = emptyList()
                _isLoading.value = false
            }
        }
    }


    /*private val _filteredList = MutableStateFlow<List<Complaint>>(emptyList())
    val filteredList: StateFlow<List<Complaint>> = _filteredList

    fun applyFilterInComplaints(
        type: String? = null,
        level: String? = null,
        sortBy: String = "newest",
    ) {
        Log.d(TAG, "applyFilterInComplaints: applying filter $type, $level, $sortBy in $TAG")
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val currentComplaints = complaintState.value

            var filteredList =
                if (sortBy == "newest") currentComplaints.sortedByDescending { it.timestamp.seconds }
                else currentComplaints.sortedBy { it.timestamp.seconds }

            if (type != null) {
                filteredList = filteredList.filter { it.complaintType == type }
            }
            if (level != null) {
                filteredList = filteredList.filter { it.complaintLevel == level }
            }
            if (type != null && level != null) {
                filteredList =
                    filteredList.filter { it.complaintType == type && it.complaintLevel == level }
            }

            _filteredList.value = filteredList
            _isLoading.value = false
        }
    }*/

    companion object {
        private const val TAG = "ComplaintViewModel"
    }
}