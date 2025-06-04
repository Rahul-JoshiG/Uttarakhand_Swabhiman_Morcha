package com.rahuljoshi.uttarakhandswabhimanmorcha

import android.app.Application
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UttarakhandSwabhimanMorcha: Application() {

    override fun onCreate() {
        super.onCreate()
        ShardPref.initPreference(this)
    }
}