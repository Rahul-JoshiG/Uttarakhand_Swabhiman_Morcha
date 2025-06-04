package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.ActivityLoginBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NoInternetHandler
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home.DashboardActivity
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.DeviceInternet
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityLoginBinding
    private var isToasting = false
    private val mAuthViewModel by viewModels<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        setInsets()
        setOnClickListener()
        setupObservers()
        checkInternetConnection()
    }

    private fun checkInternetConnection() {
        Log.d(TAG, "checkInternetConnection: checking internet connection in $TAG")
        NoInternetHandler.attach(
            context = this,
            lifecycleScope = lifecycleScope,
            isConnectedFlow = mAuthViewModel.isConnected,
            rootView = mBinding.root
        )
    }

    private fun setOnClickListener() {
        mBinding.loginBtn.setOnClickListener {
            Log.d(TAG, "Login button clicked on user login page")
            checkValidationAndNavigateToDashboard()
        }

        mBinding.skipBtn.setOnClickListener {
            Log.d(TAG, "Navigate to RegisterActivity for user registration")

            lifecycleScope.launch {
                val ip = DeviceInternet.getDeviceIpAddress(this@LoginActivity) ?: ""
                val deviceId = DeviceInternet.getDeviceId(this@LoginActivity)

                ShardPref.setIsAnonymous(true)
                ShardPref.setSkipButtonClicked(true)
                ShardPref.setIsUserLoggedIn(false)
                ShardPref.setDeviceId(Constant.DEVICE_ID, deviceId)
                ShardPref.setIpId(Constant.IP_ADDRESS, ip)
                showWarningMessage()
            }
        }


        mBinding.forgetPassword.setOnClickListener {
            Log.d(TAG, "setOnClickListener: ")
            val email = mBinding.emailId.text.toString().trim()
            if (email.isEmpty()) {
                Snackbar.make(mBinding.root, "Please enter email", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mAuthViewModel.forgetPassword(email)

        }
    }

    private fun CoroutineScope.showWarningMessage() {
        Log.d(TAG, "showWarningMessage: ")
        MaterialAlertDialogBuilder(this@LoginActivity)
            .setTitle("Warning")
            .setMessage(
                "Complaints made without registration are saved only on your device.\n" +
                        "Uninstalling or clearing app data will erase them.\n" +
                        "Please register to securely back up your complaints.\n" +
                        "Thank You."
            )
            .setCancelable(false)
            .setPositiveButton("I, understand") { dialog, _ ->
                dialog.dismiss()
                navigateToActivity(DashboardActivity::class.java)
            }
            .create()
            .show()
    }

    private fun setupObservers() {
        mAuthViewModel.status.observe(this) { isLoading ->
            mBinding.loginBtn.isEnabled = !isLoading
            mBinding.loginBtn.text = if (isLoading) "" else "Login"
            mBinding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        mAuthViewModel.loginState.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                lifecycleScope.launch {
                    ShardPref.setIsAnonymous(false)
                    ShardPref.setSkipButtonClicked(false)
                    ShardPref.setIsUserLoggedIn(true)
                    ShardPref.setDeviceId(
                        Constant.DEVICE_ID,
                        DeviceInternet.getDeviceId(this@LoginActivity)
                    )
                    ShardPref.setIpId(
                        Constant.IP_ADDRESS,
                        DeviceInternet.getDeviceIpAddress(this@LoginActivity) ?: ""
                    )
                    navigateToActivity(DashboardActivity::class.java)
                    finish()
                }
            } else {
                showSnackBar("Please try again")
            }
        }

        mAuthViewModel.resetPasswordState.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    showSnackBar("Password reset email sent. Please check your inbox.")
                }

                is Resource.Error -> {
                    showSnackBar(result.message ?: "Failed to send reset email")
                }

                else -> {}
            }
        }
    }


    private fun showSnackBar(message: String) {
        Log.d(TAG, "showSnackBar: show snackBar $TAG")
        if (!isToasting) {
            isToasting = true
            Snackbar.make(mBinding.root, message, Snackbar.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                isToasting = false
            }, 3000)
        }
    }

    private fun checkValidationAndNavigateToDashboard() {
        Log.d(TAG, "Validating login credentials in login screen")

        val email = mBinding.emailId.text.toString().trim()
        val password = mBinding.password.text.toString().trim()

        if (!mAuthViewModel.isValidEmail(email)) {
            showSnackBar("Enter valid email")
            return
        }

        if (!mAuthViewModel.checkLengthOfPassword(password)) {
            showSnackBar("Minimum length of password must be six")
            return
        }

        if (mAuthViewModel.checkEmptiness(email, password)) {
            showSnackBar("Please enter email and password")
            return
        }

        mBinding.loginBtn.isEnabled = false
        mBinding.loginBtn.text = ""
        mBinding.progressBar.visibility = View.VISIBLE

        mAuthViewModel.signIn(email, password)

    }

    private fun navigateToActivity(activity: Class<*>) {
        Log.d(TAG, "Navigating to ${activity.simpleName}")
        val intent = Intent(this, activity)
        startActivity(intent)
    }

    private fun setInsets() {
        Log.d(TAG, "Applying window insets")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
