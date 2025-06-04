package com.rahuljoshi.uttarakhandswabhimanmorcha.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ProgressBar
import androidx.lifecycle.LifecycleCoroutineScope
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object NoInternetHandler {

    fun attach(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        isConnectedFlow: StateFlow<Boolean>,
        rootView: View
    ) {
        val noInternetLayout = rootView.findViewById<View>(R.id.no_internet_layout)
        val retryButton = rootView.findViewById<Button>(R.id.btnRetry)
        val progressBar = rootView.findViewById<ProgressBar>(R.id.progressBar)

        lifecycleScope.launch {
            isConnectedFlow.collect { isConnected ->
                noInternetLayout.visibility = if (isConnected) View.GONE else View.VISIBLE
            }
        }

        retryButton.setOnClickListener {
            progressBar.visibility = VISIBLE
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            val isOnline =
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (isOnline) {
                progressBar.visibility = GONE
                noInternetLayout.visibility = GONE
            }
        }
    }
}

