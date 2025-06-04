package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.FragmentHomeBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth.AuthViewModel
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mHomeViewModel by viewModels<HomeViewModel>()
    private val mDashboardViewModel by activityViewModels<DashboardViewModel>()
    private val mAuthViewModel by activityViewModels<AuthViewModel>()

    private var mCurrentUser: User? = null
    private var isToastShowing = false
    private var pendingCallType: String? = null


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCallType?.let { type ->
                mHomeViewModel.callService(type, requireContext())
            }
        } else {
            showSnackbar("Phone call permission is required to make calls")
        }
        pendingCallType = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setOnClickListener()
        setUpEmergencyNumbers()
        setUpSocialMedia()
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: in $TAG")
        //observers for current User
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mDashboardViewModel.currentUser.collect { user ->
                    Log.d(TAG, "observe: user state changed in $TAG: $user")
                    mCurrentUser = user
                    updateUIForUserState(user)
                }
            }
        }

        //observer for call services and social media account
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mHomeViewModel.actionResult.collect { result ->
                    when (result) {
                        is HomeViewModel.ActionResult.Success -> {
                            try {
                                startActivity(result.intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start activity: ${e.message}", e)
                                showSnackbar("Failed")
                            }
                        }

                        is HomeViewModel.ActionResult.Error -> {
                            showSnackbar(result.message)
                        }

                        null -> {
                        }
                    }
                    mHomeViewModel.resetActionResult()
                }
            }
        }
    }

    private fun updateUIForUserState(user: User?) {
        if (user != null) {
            Log.d(TAG, "updateUIForUserState: User is authenticated: ${user.name}")
            binding.joinMember.visibility = View.GONE
        } else {
            Log.d(TAG, "updateUIForUserState: User is not authenticated")
            binding.joinMember.visibility = View.VISIBLE
        }
    }

    private fun setOnClickListener() {
        Log.d(TAG, "setOnClickListener all click listener in home fragment")
        binding.addCompalintLl.setOnClickListener { navigateTo("Write") }
        binding.myComplaintsLl.setOnClickListener { navigateTo("Show") }
        binding.joinMember.setOnClickListener { navigateTo("join") }
    }


    private fun navigateTo(where: String) {
        Log.d(TAG, "navigateTo: $where in $TAG")
        val user = mCurrentUser
        val action = when (where) {
            "Write" -> if (user == null)
                HomeFragmentDirections.actionHomeFragmentToNewComplaintFragment(user)
            else
                HomeFragmentDirections.actionHomeFragmentToWriteComplaintFragment(user)

            "Show" -> HomeFragmentDirections.actionHomeFragmentToComplaintFragment(user)
            "join" -> if (user == null) HomeFragmentDirections.actionHomeFragmentToRegisterActivity() else null
            else -> null
        }

        action?.let { findNavController().navigate(it) }
    }


    private fun showSnackbar(message: String) {
        if (!isToastShowing) {
            isToastShowing = true
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                isToastShowing = false
            }, 2000)
        }
    }

    private fun setUpEmergencyNumbers() {
        Log.d(TAG, "setUpEmergencyNumbers")
        binding.policeLl.setOnClickListener { callService(Constant.POLICE) }
        binding.ambulanceLl.setOnClickListener { callService(Constant.AMBULANCE) }
        binding.emergencyLl.setOnClickListener { callService(Constant.EMERGENCY) }
        binding.contactUsLl.setOnClickListener { callService(Constant.SUPPORT) }

    }

    private fun callService(type: String) {
        Log.d(TAG, "callService: call service from $TAG")
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                mHomeViewModel.callService(type, requireContext())
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE) -> {
                showSnackbar("Phone call permission is required to make emergency calls")
                pendingCallType = type
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }

            else -> {
                pendingCallType = type
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    private fun setUpSocialMedia() {
        Log.d(TAG, "setUpSocialMedia")
        binding.facebook.setOnClickListener { openSocialAccount(Constant.FACEBOOK_LINK) }
        binding.twitter.setOnClickListener { showSnackbar("Comming Soon...") }
        binding.instagram.setOnClickListener { openSocialAccount(Constant.INSTAGRAM_LINK) }
    }

    private fun openSocialAccount(url: String) {
        Log.d(TAG, "openSocialAccount: social media account from $TAG")
        mHomeViewModel.openSocialAccount(url, requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}