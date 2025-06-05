package com.rahuljoshi.uttarakhandswabhimanmorcha.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant.KEY_IS_ANONYMOUS
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant.KEY_IS_USER_LOGGED_IN
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant.KEY_SKIP_BUTTON
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant.KEY_USER_TEMP_ID
import java.util.UUID

object ShardPref {
    private const val TAG = "ShardPref"
    private const val PREF_NAME = "UttarakhandSwabhimanPrefs"

    private lateinit var mSharedPreferences: SharedPreferences

    fun initPreference(context: Context) {
        Log.d(TAG, "initPreference: ")
        mSharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Anonymous user flag
    fun setIsAnonymous(value: Boolean) {
        Log.d(TAG, "setIsAnonymous: $value")
        mSharedPreferences.edit { putBoolean(KEY_IS_ANONYMOUS, value) }
    }

    fun isAnonymous(): Boolean {
        val result = mSharedPreferences.getBoolean(KEY_IS_ANONYMOUS, true)
        Log.d(TAG, "isAnonymous: $result")
        return result
    }

    // Generate and store temp ID ONLY if user skips login
    fun setSkipButtonClicked(value: Boolean = true) {
        Log.d(TAG, "setSkipButtonClicked: $value")
        mSharedPreferences.edit {
            putBoolean(KEY_SKIP_BUTTON, value)
            putBoolean(KEY_IS_ANONYMOUS, value)

            // Only generate UUID if skip is true and temp ID not yet created
            if (value && !mSharedPreferences.contains(KEY_USER_TEMP_ID)) {
                val tempId = UUID.randomUUID().toString()
                putString(KEY_USER_TEMP_ID, tempId)
                Log.d(TAG, "Anonymous temp ID generated: $tempId")
            }
        }
    }

    fun setIsSync(key: String, value: Boolean) {
        Log.d(TAG, "setIsSync: $key in $TAG")
        mSharedPreferences.edit() { putBoolean(key, value) }
    }

    fun getIsSync(key: String): Boolean {
        Log.d(TAG, "setIsSync: $key in $TAG")
        return mSharedPreferences.getBoolean(key, true)
    }

    // Temporary ID for anonymous user
    fun getUserTempId(): String {
        val id = mSharedPreferences.getString(KEY_USER_TEMP_ID, "") ?: ""
        Log.d(TAG, "getUserTempId: $id")
        return id
    }

    // User logged in
    fun setIsUserLoggedIn(value: Boolean) {
        Log.d(TAG, "setIsUserLoggedIn: $value")
        mSharedPreferences.edit { putBoolean(KEY_IS_USER_LOGGED_IN, value) }
    }

    fun isUserLoggedIn(): Boolean {
        val result = mSharedPreferences.getBoolean(KEY_IS_USER_LOGGED_IN, false)
        Log.d(TAG, "isUserLoggedIn: $result")
        return result
    }

    // Clear all data (e.g. on logout)
    fun clearData() {
        Log.d(TAG, "clearData: ")
        mSharedPreferences.edit { clear() }
    }

    fun setDeviceId(key: String, value: String) {
        Log.d(TAG, "setDeviceId: $key and $value")
        mSharedPreferences.edit { putString(key, value) }
    }

    fun setIpId(key: String, value: String) {
        Log.d(TAG, "setDeviceId: $key and $value")
        mSharedPreferences.edit { putString(key, value) }
    }

    fun getDeviceId(key: String): String {
        Log.d(TAG, "getDeviceId: $key")
        return mSharedPreferences.getString(key, "") ?: ""
    }

    fun getIpId(key: String): String {
        Log.d(TAG, "getDeviceId: $key")
        return mSharedPreferences.getString(key, "") ?: ""
    }


    fun setDownloadLink(key: String, value: String) {
        Log.d(TAG, "setDownloadLink: in $TAG for $key and $value")
        mSharedPreferences.edit() { putString(key, value) }
    }

    fun getDownloadLink(key: String): String {
        Log.d(TAG, "getDownloadLink: for $key in $TAG")
        return mSharedPreferences.getString(key, "") ?: ""
    }
}
