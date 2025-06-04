package com.rahuljoshi.uttarakhandswabhimanmorcha.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.ActivitySplashBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth.AuthViewModel
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth.LoginActivity
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivitySplashBinding
    private val mAuthViewModel by viewModels<AuthViewModel>()

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: start in $TAG")
        if (mAuthViewModel.currentUser() != null) {
            openActivity(DashboardActivity::class.java)
        } else {
            openActivity(LoginActivity::class.java)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        applyInsets()

    }

    private fun openActivity(activity: Class<*>) {
        Log.d(TAG, "openActivity: opening $activity")
        val intent = Intent(this@SplashActivity, activity)
        startActivity(intent)
        finish()
    }

    private fun applyInsets() {
        Log.d(TAG, "applyInsets: ")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}