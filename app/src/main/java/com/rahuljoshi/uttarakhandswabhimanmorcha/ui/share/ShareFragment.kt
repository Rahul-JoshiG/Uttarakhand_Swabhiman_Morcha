package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.share

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.FragmentShareBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import com.rahuljoshi.uttarakhandswabhimanmorcha.viewmodels.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareFragment : Fragment() {
    private var _binding: FragmentShareBinding? = null
    private val binding: FragmentShareBinding get() = _binding!!

    private val viewModel by viewModels<ShareViewModel>()
    private val mUpdateViewModel by activityViewModels<UpdateViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize any non-view related setup here
        Log.d(TAG, "onCreate: Initializing non-view related setup")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
    }

    private fun initUi() {
        Log.d(TAG, "initUi: ")
        // First try to get URL from SharedPreferences
        val storedUrl = ShardPref.getDownloadLink(Constant.DOWNLOAD_LINK)
        if (storedUrl.isNotEmpty()) {
            generateQRCode(storedUrl)
        }

        /*// Also observe for updates
        mUpdateViewModel.updateInfo.observe(viewLifecycleOwner) { updateInfo ->
            updateInfo?.let {
                Log.d(TAG, "Download URL from UpdateInfo: ${it.downloadUrl}")
                generateQRCode(it.downloadUrl)
            }
        }
        // Trigger update check to get the latest download URL
        mUpdateViewModel.checkForUpdate()*/
    }

    private fun generateQRCode(url: String) {
        lifecycleScope.launch {
            try {
                val qr = viewModel.generateQRCodeBitmapAsync(url)
                Log.d(TAG, "QR Code generated successfully")
                
                // Load the QR code into the ImageView
                binding.appQrCode.load(qr) {
                    crossfade(true)
                    placeholder(R.drawable.ums_logo)
                    error(R.drawable.not_found)
                    listener(
                        onSuccess = { _, _ -> 
                            Log.d(TAG, "QR code loaded successfully")
                        },
                        onError = { _, result -> 
                            Log.e(TAG, "Error loading QR code: ${result.throwable}")
                            // Hide progress and show QR code (with error image)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating QR code", e)
                // Hide progress and show QR code (with error image)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ShareFragment"
    }
}