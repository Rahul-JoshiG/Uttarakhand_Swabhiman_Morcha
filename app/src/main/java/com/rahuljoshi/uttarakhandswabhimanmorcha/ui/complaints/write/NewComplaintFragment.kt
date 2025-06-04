package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.complaints.write

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.FragmentNewComplaintBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NoInternetHandler
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.DistrictAndTehsil
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.PermissionManager
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import com.rahuljoshi.uttarakhandswabhimanmorcha.wrapper.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class NewComplaintFragment : Fragment() {
    private var _binding: FragmentNewComplaintBinding? = null
    private val binding: FragmentNewComplaintBinding get() = _binding!!

    private val mViewModel: NewComplaintViewModel by viewModels()
    private val navArgs by navArgs<NewComplaintFragmentArgs>()

    private var selectedFileUri: Uri? = null
    private var isUploadInProgress = false
    private var isToasting = false

    private var mFileName: String? = null
    private var mFileUrl: String? = null
    private var mFileId: String? = null
    private var progressDialog: AlertDialog? = null

    // Permission launcher for storage
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (PermissionManager.areAllPermissionsGranted(permissions)) {
            selectFileFromDevice()
        } else {
            showSnackbar(PermissionManager.getPermissionRationaleMessage(permissions.keys.first()))
        }
    }

    // Permission launcher for camera
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showSnackbar(PermissionManager.getPermissionRationaleMessage(PermissionManager.CAMERA_PERMISSION))
        }
    }

    private fun openCamera() {
        val takeImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(takeImage)
        } catch (e: Exception) {
            Log.d(
                TAG,
                "openCamera: caught error ${e.localizedMessage}"
            )
            showSnackbar("Camera not available")
        }
    }

    private fun selectFileFromDevice() {
        val mimeTypes = arrayOf(
            "image/*",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        filePickerLauncher.launch(mimeTypes)
    }

    // File picker
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                selectedFileUri = it
                mFileName = getFileName(it)
                showUploadingDialog(it)
            } ?: showSnackbar("No file selected")
        }

    // Camera
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                mViewModel.saveBitmapToCache(it, requireContext()) { uri ->
                    uri?.let {
                        selectedFileUri = it
                        mFileName = "captured_image_${System.currentTimeMillis()}.jpg"
                        showUploadingDialog(it)
                    } ?: showSnackbar("Failed to save image")
                }
            } ?: showSnackbar("Failed to capture image")
        } else {
            showSnackbar("Camera cancelled")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewComplaintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListener()
        setUpSpinners()
        setupTextWatchers()
        checkInternetConnection()
    }

    private fun checkInternetConnection() {
        Log.d(TAG, "checkInternetConnection: checking internet connection in ${TAG}")
        NoInternetHandler.attach(
            context = requireContext(),
            lifecycleScope = lifecycleScope,
            isConnectedFlow = mViewModel.isConnected,
            rootView = binding.root
        )
    }

    private fun setupTextWatchers() {
        binding.personName.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) binding.tilName.error = null
        }

        binding.phoneNumber.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) binding.tilPhone.error = null
        }

        binding.complaintTitle.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) binding.tilSubject.error = null
        }

        binding.complaintDescription.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) binding.tilDescription.error = null
        }

        binding.village.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrEmpty()) binding.tilVillage.error = null
        }
    }


    private fun setUpSpinners() {
        Log.d(TAG, "setUpSpinners: in $TAG")
        val districts = DistrictAndTehsil.getDistricts()
        val districtAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, districts)
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.districtSpinner.adapter = districtAdapter

        binding.districtSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val blockList = DistrictAndTehsil.getTehsils(districts[position])
                    binding.blockSpinner.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        blockList
                    )
                    binding.blockSpinner.visibility = VISIBLE
                    binding.blockProgressBar.visibility = GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }

            }

    }

    private fun setOnClickListener() {
        Log.d(TAG, "setOnClickListener: all clicks are in $TAG")
        binding.submitComplaint.setOnClickListener {
            validateInputsAndSubmit()
        }
        binding.fileUpload.setOnClickListener {
            showDialogBoxForStorageSelection()
        }
        binding.deleteFile.setOnClickListener {
            removeSelectedFile()
        }
    }

    private fun validateInputsAndSubmit() {
        val personName = binding.personName.text?.trim().toString()
        val phoneNumber = binding.phoneNumber.text?.trim().toString()
        val complaintTitle = binding.complaintTitle.text?.trim().toString()
        val complaintDescription = binding.complaintDescription.text?.trim().toString()
        val district = binding.districtSpinner.selectedItem?.toString()
        val block = binding.blockSpinner.selectedItem?.toString()
        val village = binding.village.text?.trim().toString()

        var isValid = true

        // Clear previous errors
        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilSubject.error = null
        binding.tilDescription.error = null
        binding.tilVillage.error = null

        if (personName.isEmpty()) {
            binding.tilName.error = "Name is required"
            isValid = false
        }
        if (phoneNumber.length != 10) {
            binding.tilPhone.error = "Enter valid 10-digit phone number"
            isValid = false
        }
        if (complaintTitle.isEmpty()) {
            binding.tilSubject.error = "Title is required"
            isValid = false
        }
        if (complaintDescription.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            isValid = false
        }
        if (village.isEmpty()) {
            binding.tilVillage.error = "Village name is required"
            isValid = false
        }

        if (district?.isEmpty() == true || district == "Select District") {
            showSnackbar("Please select a district")
            isValid = false
        }

        if (block?.isEmpty() == true || block == "Select Block") {
            showSnackbar("Please select a block")
            isValid = false
        }

        if (!isValid) return

        binding.apply {
            submitComplaint.apply {
                text = ""
                isEnabled = false
            }
            progressBar.visibility = VISIBLE
        }
        updateSubmitButtonState(isSubmitting = true)

        // All inputs valid — submit
        mViewModel.submitComplaint(
            personName,
            phoneNumber,
            complaintTitle,
            complaintDescription,
            district,
            block,
            village,
            mFileUrl,
            onSuccess = { complaintId ->
                // Create complaint object for email
                val complaint = Complaint(
                    complaintId = complaintId,
                    complaintTitle = complaintTitle,
                    complaintDescription = complaintDescription,
                    userId = null,
                    userTempId = ShardPref.getUserTempId(),
                    isAnonymous = true,
                    timestamp = Timestamp.now(),
                    district = district.toString(),
                    tehsil = block.toString(),
                    village = village,
                    fileUrl = mFileUrl ?: "N/A",
                    personName = personName,
                    phoneNumber = phoneNumber,
                    deviceId = ShardPref.getDeviceId(Constant.DEVICE_ID),
                    ipAddress = ShardPref.getIpId(Constant.IP_ADDRESS)
                )

                // Send email asynchronously
                mViewModel.sendComplaintEmail(complaint)

                showSnackbar("Complaint submitted successfully")
                Log.d(TAG, "Complaint ID: $complaintId")
                binding.apply {
                    submitComplaint.apply {
                        text = "Submit"
                        isEnabled = true
                    }
                    progressBar.visibility = GONE
                }

                findNavController().navigateUp()
            },
            onFailure = { error ->
                showSnackbar("Error submitting complaint: ${error.message}")
                binding.apply {
                    submitComplaint.apply {
                        text = "Submit"
                        isEnabled = true
                    }
                    progressBar.visibility = GONE
                }
            }
        )
    }

    private fun updateSubmitButtonState(isSubmitting: Boolean) {
        binding.submitComplaint.apply {
            text = if (isSubmitting) "" else "Submit"
            isEnabled = !isSubmitting
        }
        binding.progressBar.visibility = if (isSubmitting) VISIBLE else GONE
    }

    private fun resetUi() {
        updateSubmitButtonState(isSubmitting = false)
    }

    private fun showSnackbar(message: String) {
        Log.d(TAG, "showSnackbar: $message in $TAG")
        if (!isToasting) {
            isToasting = true
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                isToasting = false
            }, 2000)
        }
    }

    private fun removeSelectedFile() {
        mFileId?.let { mViewModel.deleteFile(it) }
        binding.apply {
            fileName.text = ""
            fileTextDeleteLl.visibility = GONE
            fileDisplayCard.visibility = GONE
        }
        mFileUrl = null
        mFileId = null
        selectedFileUri = null
        mFileName = null
    }

    private fun showUploadingDialog(uri: Uri) {
        Log.d(TAG, "showUploadingDialog: ")
        binding.apply {
            fileDisplayCard.visibility = VISIBLE
            fileName.text = mFileName
        }
        if (isUploadInProgress) return
        isUploadInProgress = true

        progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Uploading")
            .setMessage("Please wait while your file is being uploaded...")
            .setCancelable(false)
            .create()
        progressDialog?.show()

        val filePath = getRealPathFromUri(uri)
        if (filePath == null) {
            showSnackbar("Failed to get file path")
            progressDialog?.dismiss()
            isUploadInProgress = false
            binding.fileDisplayCard.visibility = GONE
            return
        }

        val user = navArgs.user
        val folderName = if (user == null) FOLDER_ANONYMOUS else FOLDER_AUTHENTICATED

        mViewModel.uploadFile(
            filePath = filePath,
            complaintName = binding.complaintTitle.text.toString().ifEmpty { "Untitled" },
            userName = folderName,
        ) { resource ->
            progressDialog?.dismiss()
            isUploadInProgress = false
            when (resource) {
                is Resource.Success -> {
                    mFileUrl = resource.data
                    mFileId = extractFileIdFromUrl(resource.data ?: "")
                    binding.apply {
                        fileName.text = mFileName
                        fileTextDeleteLl.visibility = VISIBLE
                        fileDisplayCard.visibility = VISIBLE
                    }
                    showSnackbar("File uploaded successfully")
                }

                is Resource.Error -> {
                    showSnackbar("Failed to upload file: ${resource.message}")
                    binding.fileDisplayCard.visibility = GONE
                }

                is Resource.Loading<*> -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    private fun extractFileIdFromUrl(url: String): String? {
        return try {
            val regex = Regex("/d/(.*?)/")
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting file ID: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex("_display_name")
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "unknown_file"
                cursor.close()
                name
            } else {
                "unknown_file"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name: ${e.message}")
            "unknown_file"
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, mFileName ?: "tempFile")
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error copying file: ${e.message}")
            null
        }
    }

    private fun showDialogBoxForStorageSelection() {
        val options = arrayOf("Camera", "Browser")
        AlertDialog.Builder(requireContext()).setTitle("Select Option")
            .setSingleChoiceItems(options, -1) { dialog, which ->
                when (options[which]) {
                    "Camera" -> {
                        if (PermissionManager.isCameraPermissionGranted(requireContext())) {
                            openCamera()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    "Browser" -> {
                        if (PermissionManager.isStoragePermissionGranted(requireContext())) {
                            selectFileFromDevice()
                        } else {
                            PermissionManager.requestStoragePermissions(
                                requestStoragePermissionLauncher
                            )
                        }
                    }
                }
                dialog.dismiss()
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "NewComplaintFragment"
        private const val TOAST_DELAY = 2000L
        private const val FOLDER_ANONYMOUS = "AnonymousUserFiles"
        private const val FOLDER_AUTHENTICATED = "AuthenticateUserFiles"
    }
}