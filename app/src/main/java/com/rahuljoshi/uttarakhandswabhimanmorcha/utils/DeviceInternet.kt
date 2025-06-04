package com.rahuljoshi.uttarakhandswabhimanmorcha.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object DeviceInternet {

    @SuppressLint("DefaultLocale")
    suspend fun getDeviceIpAddress(context: Context): String? {
        val ipFromPublicApi = getPublicIpAddress()
        if (!ipFromPublicApi.isNullOrEmpty()) return ipFromPublicApi

        // fallback to local IP if public fails (e.g., no internet)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    private suspend fun getPublicIpAddress(): String? = withContext(Dispatchers.IO){
        return@withContext try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.inputStream.bufferedReader().use {
                it.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}