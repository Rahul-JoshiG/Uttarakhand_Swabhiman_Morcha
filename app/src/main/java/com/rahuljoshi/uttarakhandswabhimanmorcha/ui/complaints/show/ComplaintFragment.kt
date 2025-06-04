package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.complaints.show

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.FragmentComplaintBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NoInternetHandler
import com.rahuljoshi.uttarakhandswabhimanmorcha.interfaces.OnClickListeners
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.local.ComplaintEntity
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ComplaintFragment : Fragment(), OnClickListeners {
    private var _binding: FragmentComplaintBinding? = null
    private val binding: FragmentComplaintBinding get() = _binding!!

    private val mViewModel: ComplaintViewModel by viewModels()
    private lateinit var mComplaintAdapter: ComplaintAdapter

    private val args: ComplaintFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComplaintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = args.user?.uid
        val userTempId = ShardPref.getUserTempId()

        Log.d(TAG, "onViewCreated: uid in $TAG = $uid")
        Log.d(TAG, "onViewCreated: userTempId in $TAG = $userTempId")

        if (uid == null) {
            Log.d(TAG, "onViewCreated: fetch anonymous complaints in $TAG")
            mViewModel.observeLocalComplaints()
        } else {
            Log.d(TAG, "onViewCreated: fetch complaints in $TAG")
            mViewModel.observeComplaints(uid.toString())
        }

        observeLoadingState()
        observeComplaintsData()
        setOnClickListener()
        setUpRecyclerView()
        /*setUpToolbar()
        setUpSpinner()
        setupSpinnerListeners()
        filteringComplaints()
        checkInternetConnection()*/
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

    //showing all complaints
    private fun observeComplaintsData() {
        Log.d(TAG, "observeComplaintsData: in $TAG")
        // Collect flow and update UI
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.complaintState.collect { complaints ->
                    Log.d(TAG, "Received ${complaints.size} complaints")
                    if (complaints.isEmpty()) {
                        showAndHide(0)
                        updateRecyclerViewData(emptyList())
                        showTotalComplaints(0)
                    } else {
                        showAndHide(complaints.size)
                        updateRecyclerViewData(complaints)
                        showTotalComplaints(complaints.size)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.complaintLocalState.collect { complaints ->
                    Log.d(TAG, "Received ${complaints.size} complaints")

                    val complaintList = complaints.toComplaintList()

                    if (complaintList.isEmpty()) {
                        showAndHide(0)
                        updateRecyclerViewData(emptyList())
                        showTotalComplaints(0)
                    } else {
                        showAndHide(complaintList.size)
                        updateRecyclerViewData(complaintList)
                        showTotalComplaints(complaintList.size)
                    }
                }
            }
        }
    }

    //showing filtered complaints
    /* private fun observeFilterComplaintData() {
         Log.d(TAG, "observeFilterComplaintData: in ${TAG}")
         lifecycleScope.launch {
             repeatOnLifecycle(Lifecycle.State.STARTED) {
                 mComplaintViewModel.filteredList.collect { filterComplaint ->
                     Log.d(
                         TAG,
                         "observeFilterComplaintData: filter complaint $filterComplaint in ${TAG}"
                     )
                     if (filterComplaint.isEmpty()) {
                         showAndHide(0)
                         updateRecyclerViewData(emptyList())
                         showTotalComplaints(0)
                     } else {
                         showAndHide(filterComplaint.size)
                         updateRecyclerViewData(filterComplaint)
                         showTotalComplaints(filterComplaint.size)
                     }
                 }
             }
         }
     }*/

    //showing loader while fetching data
    private fun observeLoadingState() {
        Log.d(TAG, "observeLoadingState: in ${TAG}")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.isLoading.collect { loading ->
                    binding.apply {
                        if (loading) {
                            loadingLayout.visibility = VISIBLE
                            myComplaintsRecyclerView.visibility = GONE
                            emptyState.visibility = GONE
                        } else {
                            loadingLayout.visibility = GONE
                        }
                    }
                }
            }
        }
    }

    //showing and hide the no data found image based on size
    private fun showAndHide(i: Int) {
        Log.d(TAG, "showAndHide: in ${TAG}")
        binding.apply {
            progressBar.visibility = GONE
            if (i == 0) {
                emptyState.visibility = VISIBLE
                myComplaintsRecyclerView.visibility = GONE
            } else {
                emptyState.visibility = GONE
                myComplaintsRecyclerView.visibility = VISIBLE
            }
        }
    }

    //set upping spinner values
    /*private fun setUpSpinner() {
        Log.d(TAG, "setUpSpinner: set up spinner for filters in ${TAG}")

        val sortByList = listOf("Sort By", "Newest to Oldest", "Oldest to Newest")
        val levelByList = listOf("Level By", "City Level", "District Level", "State Level")//level
        val typeByList = listOf("Status By", "Solved", "Unsolved", "Demand")//type

        binding.sortBy.adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_dropdown_item,
            sortByList
        )
        binding.levelBy.adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_dropdown_item,
            levelByList
        )
        binding.typeBy.adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_dropdown_item,
            typeByList
        )
    }*/

    //checking internet connection
    /*private fun checkInternetConnection() {
        Log.d(TAG, "checkInternetConnection: checking internet connection in ${TAG}")
        NoInternetHandler.attach(
            context = requireContext(),
            lifecycleScope = lifecycleScope,
            isConnectedFlow = mComplaintViewModel.isConnected,
            rootView = binding.root
        )
    }*/

    //applying filter on complaints
    /* private fun filteringComplaints() {
         Log.d(TAG, "filteringComplaints: Filtering complaints in ${TAG}")

         val selectedLevel = binding.levelBy.selectedItem?.toString()
         val selectedType = binding.typeBy.selectedItem?.toString()
         val selectedSort = binding.sortBy.selectedItem?.toString()

         val isDefaultLevel = selectedLevel == "Level By"
         val isDefaultType = selectedType == "Status By"
         val isDefaultSort = selectedSort == "Sort By"

         val level = when (selectedLevel) {
             "City Level" -> "City Level"
             "District Level" -> "District Level"
             "State Level" -> "State Level"
             else -> null
         }

         val type = when (selectedType) {
             "Unsolved" -> "Unsolved"
             "Solved" -> "Solved"
             "Demand" -> "Demand"
             else -> null
         }

         // Default to newest even if "Sort By" is selected
         val sortBy = when (selectedSort) {
             "Newest to Oldest" -> "newest"
             "Oldest to Newest" -> "oldest"
             else -> "newest"
         }

         Log.d(TAG, "Selected Filters: Type = $type, Level = $level, SortBy = $sortBy in ${TAG}")

         // If all are default, fetch all complaints without filters
         if (isDefaultLevel && isDefaultType && isDefaultSort) {
             observeComplaintsData()
         } else {
             mComplaintViewModel.applyFilterInComplaints(type, level, sortBy)
             observeFilterComplaintData()
         }
     }*/

    //set up spinners listening
    /*private fun setupSpinnerListeners() {
        binding.typeBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filteringComplaints()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.levelBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filteringComplaints()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.sortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filteringComplaints()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }*/

    //all clicks listener
    private fun setOnClickListener() {
        Log.d(TAG, "setOnClickListener: Setting click listeners in ComplaintFragment")
        binding.addNewComplaint.setOnClickListener {
            if (args.user?.uid.isNullOrEmpty()) {
                findNavController().navigate(
                    ComplaintFragmentDirections.actionComplaintFragmentToNewComplaintFragment(
                        args.user
                    )
                )
            } else {
                findNavController().navigate(
                    ComplaintFragmentDirections.actionComplaintFragmentToWriteComplaintFragment(
                        args.user
                    )
                )
            }
        }
    }

    //set upping recycler view
    private fun setUpRecyclerView() {
        Log.d(TAG, "setUpRecyclerView: Updating RecyclerView with complaints")
        mComplaintAdapter = ComplaintAdapter(emptyList(), this)
        binding.myComplaintsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mComplaintAdapter
        }
    }

    //updating recycler view data
    private fun updateRecyclerViewData(complaints: List<Complaint>) {
        Log.d(TAG, "updateRecyclerViewData: updating list for $complaints in ${TAG}")
        mComplaintAdapter.updateList(complaints)
    }

    //showing total complaints
    @SuppressLint("SetTextI18n")
    private fun showTotalComplaints(size: Int) {
        Log.d(TAG, "showTotalComplaints: Total complaints = $size")
        if (size == 0) {
            showAndHide(0)
            binding.totalComplaints.text = "Total Complaints: $size"
        } else {
            binding.totalComplaints.text = "Total Complaints: $size"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onComplaintClickListener(complaint: Complaint) {
        super.onComplaintClickListener(complaint)

    }

    fun ComplaintEntity.toComplaint(): Complaint {
        return Complaint(
            complaintTitle = this.complaintTitle,
            complaintDescription = this.complaintDescription,
            complaintId = this.complaintId,
            deviceId = this.deviceId,
            ipAddress = this.ipAddress,
            phoneNumber = this.phoneNumber,
            personName = this.personName,
            timestamp = this.timestamp,
            district = this.district,
            tehsil = this.tehsil,
            village = this.village,
            isAnonymous = this.isAnonymous,
            userTempId = this.userTempId,
            userId = this.userId,
            fileUrl = this.fileUrl,
        )
    }

    fun List<ComplaintEntity>.toComplaintList(): List<Complaint> {
        return this.map { it.toComplaint() }
    }

    companion object {
        private const val TAG = "ComplaintFragment"
    }
}