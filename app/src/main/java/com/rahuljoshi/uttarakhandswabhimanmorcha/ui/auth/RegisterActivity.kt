package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.ActivityRegisterBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NoInternetHandler
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.DistrictAndTehsil
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityRegisterBinding
    private val mViewModel by viewModels<AuthViewModel>()

    private var isToasting = false
    private lateinit var progressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mBinding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        applyInsets()
        setupProgressDialog()
        setUpSpinners()
        setupTextWatchers()
        setOnClickListener()
        checkInternetConnection()
    }

    private fun checkInternetConnection() {
        Log.d(TAG, "checkInternetConnection: checking internet connection in $TAG")
        NoInternetHandler.attach(
            context = this,
            lifecycleScope = lifecycleScope,
            isConnectedFlow = mViewModel.isConnected,
            rootView = mBinding.root
        )
    }

    private fun setupTextWatchers() {
        mBinding.personName.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) mBinding.tilName.error = null
        }

        mBinding.phoneNumber.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) mBinding.tilPhone.error = null
        }

        mBinding.emailId.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) mBinding.tilEmail.error = null
        }

        mBinding.password.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) mBinding.tilPassword.error = null
        }

        mBinding.village.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) mBinding.tilVillage.error = null
        }
    }


    private fun setUpSpinners() {
        Log.d(TAG, "setUpSpinners: in $TAG")
        val districts = DistrictAndTehsil.getDistricts()
        val districtAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.districtSpinner.adapter = districtAdapter

        mBinding.districtSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val blockList = DistrictAndTehsil.getTehsils(districts[position])
                    mBinding.blockSpinner.adapter = ArrayAdapter(
                        this@RegisterActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        blockList
                    )
                    mBinding.blockSpinner.visibility = VISIBLE
                    mBinding.blockProgressBar.visibility = GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }
    }

    private fun setOnClickListener() {
        Log.d(
            TAG,
            "setOnClickListener: all clicks are in $TAG"
        )
        mBinding.register.setOnClickListener {
            validateInputsAndSubmit()
        }

        mBinding.cancelButton.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
            finish()
        }
    }

    private fun validateInputsAndSubmit() {
        Log.d(TAG, "validateInputsAndSubmit: ")

        val personName = mBinding.personName.text?.trim().toString()
        val personEmail = mBinding.emailId.text?.trim().toString()
        val personPassword = mBinding.password.text?.trim().toString()
        val phoneNumber = mBinding.phoneNumber.text?.trim().toString()
        val district = mBinding.districtSpinner.selectedItem?.toString() ?: ""
        val block = mBinding.blockSpinner.selectedItem?.toString() ?: ""
        val village = mBinding.village.text?.trim().toString()

        var isValid = true

        // Clear previous errors
        mBinding.tilName.error = null
        mBinding.tilPhone.error = null
        mBinding.tilVillage.error = null
        mBinding.tilEmail.error = null
        mBinding.tilPassword.error = null

        // Name
        if (personName.isEmpty()) {
            mBinding.tilName.error = "Name is required"
            isValid = false
        }

        // Email
        if (personEmail.isEmpty()) {
            mBinding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(personEmail).matches()) {
            mBinding.tilEmail.error = "Enter valid email address"
            isValid = false
        }

        // Password
        if (personPassword.length < 6) {
            mBinding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        // Phone number
        if (phoneNumber.length != 10 || !phoneNumber.all { it.isDigit() }) {
            mBinding.tilPhone.error = "Enter valid 10-digit phone number"
            isValid = false
        }

        // Village
        if (village.isEmpty()) {
            mBinding.tilVillage.error = "Village name is required"
            isValid = false
        }

        // District
        if (district.isEmpty() || district == "Select District") {
            showSnackbar("Please select a district")
            isValid = false
        }

        // Block
        if (block.isEmpty() || block == "Select Block") {
            showSnackbar("Please select a block")
            isValid = false
        }

        if (!isValid) return

        observerRegistrationState(
            fullName = personName,
            emailId = personEmail,
            password = personPassword,
            phoneNumber = phoneNumber,
            districtName = district,
            tehsil = block,
            village = village
        )
    }


    @SuppressLint("SetTextI18n")
    private fun observerRegistrationState(
        fullName: String,
        emailId: String,
        password: String,
        phoneNumber: String,
        districtName: String,
        tehsil: String,
        village: String,

        ) {
        Log.d(TAG, "observerRegistrationState: all fields are filled now going further")

        val user = User(
            uid = "",
            name = fullName,
            email = emailId,
            phoneNumber = phoneNumber,
            district = districtName,
            tehsil = tehsil,
            village = village,
            isAuthenticate = true,
            timestamp = Timestamp.now()
        )

        mViewModel.createUser(emailId, password, user)
        mViewModel.status.observe(this) { isLoading ->
            if (isLoading) {
                mBinding.register.text = ""
                mBinding.progressBar.visibility = VISIBLE
            } else {
                mBinding.register.text = "Register"
                mBinding.progressBar.visibility = GONE
            }
        }

        mViewModel.registerState.observe(this) { success ->
            if (!success) {
                showSnackbar("Email already occupied. Please try with another email.")
            } else {
                showProgressDialog("Syncing your complaints to cloud...")
                mViewModel.syncLocalComplaintsToFirestore()
            }
        }

        mViewModel.syncStatus.observe(this) { isSyncing ->
            if (!isSyncing) {
                hideProgressDialog()
                navigateToActivity(DashboardActivity::class.java)
            }
        }

    }

    private fun navigateToActivity(activity: Class<*>) {
        Log.d(TAG, "navigateToDashBoardActivity: ")
        val intent = Intent(this, activity)
        startActivity(intent)
        finish()
    }


    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showSnackbar(message: String) {
        Log.d(TAG, "showSnackbar: $message in $TAG")
        if (!isToasting) {
            isToasting = true
            Snackbar.make(mBinding.root, message, Snackbar.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                isToasting = false
            }, 2000)
        }
    }

    private fun setupProgressDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_progress, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        progressDialog = builder.create()
    }

    private fun showProgressDialog(message: String) {
        val dialogView = progressDialog.findViewById<TextView>(R.id.progressMessage)
        dialogView?.text = message
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}