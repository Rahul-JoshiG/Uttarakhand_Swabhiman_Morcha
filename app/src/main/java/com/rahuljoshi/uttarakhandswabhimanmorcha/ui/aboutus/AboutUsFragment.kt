package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.aboutus

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.FragmentAboutUsBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Section
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class AboutUsFragment : Fragment() {
    private var _binding: FragmentAboutUsBinding? = null
    private val binding : FragmentAboutUsBinding get() = _binding!!

    private val viewModel: AboutUsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutUsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val locale = Locale("hi")
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        initUi()
    }

    private fun initUi() {
        Log.d(TAG, "initUi: ")
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val sections = listOf(
            Section(getString(R.string.about_us_title), getString(R.string.about_us)),
            Section(getString(R.string.our_goal_title), getString(R.string.our_goal)),
            Section(getString(R.string.why_us_title), getString(R.string.why_us)),
            Section(getString(R.string.why_we_are_different_title), getString(R.string.why_we_are_different)),
            Section(getString(R.string.our_belief_title), getString(R.string.our_belief))
        )

        recyclerView.adapter = SectionAdapter(sections)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object{
        private const val TAG = "AboutUsFragment"
    }
}