package com.rahuljoshi.uttarakhandswabhimanmorcha.viewmodels

import androidx.lifecycle.ViewModel
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NetworkHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(networkHelper: NetworkHelper) : ViewModel() {
    val isConnected: StateFlow<Boolean> = networkHelper.isConnected
}