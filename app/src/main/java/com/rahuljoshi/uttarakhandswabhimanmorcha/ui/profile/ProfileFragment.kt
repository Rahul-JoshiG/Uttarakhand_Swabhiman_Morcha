package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.FragmentProfileBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home.DashboardViewModel
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel by activityViewModels<DashboardViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        checkSyncSwitcher()
    }

    private fun checkSyncSwitcher() {
        Log.d(TAG, "checkSyncSwitcher: in $TAG")
        binding.syncSwitch.isChecked = ShardPref.getIsSync(Constant.IS_SYNC)

        binding.syncSwitch.setOnCheckedChangeListener({ _, isChecked ->
            if (isChecked) {
                ShardPref.setIsSync(Constant.IS_SYNC, true)
            } else {
                ShardPref.setIsSync(Constant.IS_SYNC, false)
            }
        })


    }

    private fun setupClickListeners() {
        // Uncomment and use this if you implement sign out later
        binding.joinMember.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToRegisterActivity()
            findNavController().navigate(action)
        }
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: in $TAG")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.currentUser.collect { resource ->
                    resource?.let { user ->
                        showUserDetails(user)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showUserDetails(user: User?) {
        Log.d(TAG, "showUserDetails: user = $user")

        binding.apply {
            if (user == null) {
                tvUserName.text = "Guest User"
                tvUserEmail.text = "N/A"
                tvDistrict.text = "N/A"
                tvTehsil.text = "N/A"
                tvVillage.text = "N/A"
                cloudCard.visibility = GONE
                approval.text = "Guest"
                joinMember.visibility = VISIBLE
            } else {
                tvUserName.text = user.name
                tvUserEmail.text = user.email
                tvDistrict.text = user.district
                tvTehsil.text = user.tehsil
                tvVillage.text = user.village
                approval.text = "Member"
                joinMember.visibility = GONE
                cloudCard.visibility = VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}