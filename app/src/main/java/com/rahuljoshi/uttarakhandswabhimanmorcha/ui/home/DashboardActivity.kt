package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.ActivityDashboardBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.SyncDialogLayoutBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.helper.NoInternetHandler
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.UpdateInfo
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.User
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth.AuthViewModel
import com.rahuljoshi.uttarakhandswabhimanmorcha.ui.auth.LoginActivity
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.Constant
import com.rahuljoshi.uttarakhandswabhimanmorcha.utils.ShardPref
import com.rahuljoshi.uttarakhandswabhimanmorcha.viewmodels.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var mBinding: ActivityDashboardBinding

    private val mViewModel by viewModels<DashboardViewModel>()
    private val mAuthViewModel by viewModels<AuthViewModel>()
    private val mUpdateViewModel by viewModels<UpdateViewModel>()

    private lateinit var progressDialog: AlertDialog
    private var progressDownloadDialog: AlertDialog? = null

    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mViewModel.loadCurrentUserId()
        mUpdateViewModel.checkForUpdate()

        checkLatestVersion()
        setUpObserver()
        setUpNavigationDrawer()
        setUserDetailsInDrawer()
        observeUserDetails()
        navigationDrawerListener()
        checkInternetConnection()
        setupProgressDialog()
    }

    private fun checkLatestVersion() {
        Log.d(TAG, "checkLatestVersion: ")
        mUpdateViewModel.updateInfo.observe(this) { info ->
            info?.let {
                showUpdateDialog(this, it)
            }
        }
        mUpdateViewModel.downloadProgress.observe(this) { progress ->
            if (progressDownloadDialog == null) {
                val dialogView = layoutInflater.inflate(R.layout.download_dialog_layout, null)
                progressBar = dialogView.findViewById(R.id.progressBar)
                progressText = dialogView.findViewById(R.id.progressText)

                progressDownloadDialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .show()
            }

            progressBar?.progress = progress
            progressText?.text = "$progress%"

            if (progress >= 100) {
                progressDownloadDialog?.dismiss()
                progressDownloadDialog = null
                progressBar = null
                progressText = null
            }
        }

    }

    private fun showUpdateDialog(context: Context, updateInfo: UpdateInfo) {
        AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("Version ${updateInfo.versionName}\n\n${updateInfo.releaseNotes}")
            .setCancelable(!updateInfo.forceUpdate)
            .setPositiveButton("Update") { _, _ ->
                mUpdateViewModel.downloadApk(updateInfo) { file ->
                    file?.let { promptInstall(context, it) }
                }
            }
            .setNegativeButton(if (updateInfo.forceUpdate) "" else "Later", null)
            .show()
    }

    private fun promptInstall(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    private fun setUpObserver() {
        Log.d(TAG, "setUpObserver: ")
        mAuthViewModel.syncStatus.observe(this) { isSyncing ->
            if (!isSyncing) {
                hideProgressDialog()
            }
        }
    }


    private fun showDialogBox() {
        Log.d(TAG, "showDialogBox: in $TAG")
        val dialogBinding = SyncDialogLayoutBinding.inflate(LayoutInflater.from(this))

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Handle Accept button click
        dialogBinding.btnAccept.setOnClickListener {
            if (dialogBinding.checkboxDontAskAgain.isChecked) {
                ShardPref.setIsSync(Constant.IS_SYNC, false)
            }

            mAuthViewModel.syncLocalComplaintsToFirestore()
            showProgressDialog("Syncing your complaints to cloud...")
            dialog.dismiss()
        }

        // Handle Reject button click
        dialogBinding.btnReject.setOnClickListener {
            // Check if "Don't ask again" is checked and save preference
            if (dialogBinding.checkboxDontAskAgain.isChecked) {
                ShardPref.setIsSync(Constant.IS_SYNC, false)
                dialog.dismiss()
            }

            dialog.dismiss()
        }

        dialog.show()
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

    private fun checkInternetConnection() {
        Log.d(TAG, "checkInternetConnection: checking internet connection in ${TAG}")
        NoInternetHandler.attach(
            context = this,
            lifecycleScope = lifecycleScope,
            isConnectedFlow = mViewModel.isConnected,
            rootView = mBinding.root
        )
    }

    private fun navigationDrawerListener() {
        Log.d(TAG, "navigationDrawerListener: in $TAG")
        mBinding.navView.setNavigationItemSelectedListener { menuItem ->
            val navController = getNavController()
            val handled = when (menuItem.itemId) {
                R.id.log_out -> {
                    showDialogAndLogOut()
                    true
                }

                R.id.nav_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }

                R.id.nav_about_us -> {
                    navController.navigate(R.id.aboutUsFragment)
                    true
                }

                R.id.nav_share -> {
                    navController.navigate(R.id.shareFragment)
                    true
                }

                else -> false
            }
            if (handled) {
                mBinding.drawerLayout.closeDrawer(mBinding.navView, true)
            }
            handled
        }
    }

    private fun showDialogAndLogOut() {
        Log.d(TAG, "showDialogAndLogOut: dialog for logout in $TAG")
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.log_out))
            .setMessage("Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                mAuthViewModel.signOut()
                startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setUpNavigationDrawer() {
        Log.d(TAG, "setUpNavigationDrawer: in $TAG")
        setSupportActionBar(mBinding.appBarMain.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val drawerLayout: DrawerLayout = mBinding.drawerLayout
        val navView: NavigationView = mBinding.navView
        val navController = getNavController()

        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, _, _ ->
            supportActionBar?.title = resources.getString(R.string.app_name)
        }
    }

    private fun observeUserDetails() {
        Log.d(TAG, "observeUserDetails: observe user details in ${TAG}")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.currentUser.collect { user ->
                    Log.d(TAG, "observeUserDetails: $user in $TAG")
                    if (user != null) {
                        showUserDetails(user)
                        if (ShardPref.getIsSync(Constant.IS_SYNC)) {
                            showDialogBox()
                        }
                    }
                }
            }
        }
    }

    private fun showUserDetails(user: User) {
        Log.d(TAG, "showUserDetails: showing $user details in $TAG")
        val headerView = mBinding.navView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.nav_person_name)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.nav_person_email)
        tvUserName.text = user.name
        tvUserEmail.text = user.email
    }

    private fun setUserDetailsInDrawer() {
        Log.d(TAG, "setUserDetailsInDrawer: ")
        lifecycleScope.launch(Dispatchers.Default) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.currentUserId.collect {
                    Log.d(TAG, "setUserDetailsInDrawer: uid = $it in ${TAG}")
                    mViewModel.fetchUserDetails(it)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = getNavController()
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun getNavController() =
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_dashboard) as NavHostFragment).navController

    companion object {
        private const val TAG = "DashboardActivity"
    }
}
