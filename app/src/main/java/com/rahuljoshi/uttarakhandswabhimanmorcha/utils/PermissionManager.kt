package com.rahuljoshi.uttarakhandswabhimanmorcha.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionManager {

    // Camera permission
    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    // Storage permissions based on API level
    private val STORAGE_PERMISSIONS_TIRAMISU_AND_ABOVE = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    )

    private val STORAGE_PERMISSIONS_BELOW_TIRAMISU = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val STORAGE_PERMISSIONS_API_29_TO_32 = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if storage permissions are granted based on API level
     */
    fun isStoragePermissionGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                val readPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                val writePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                readPermission && writePermission
            }
        }
    }

    /**
     * Get required storage permissions based on API level
     */
    fun getRequiredStoragePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                STORAGE_PERMISSIONS_TIRAMISU_AND_ABOVE
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                STORAGE_PERMISSIONS_API_29_TO_32
            }
            else -> {
                STORAGE_PERMISSIONS_BELOW_TIRAMISU
            }
        }
    }

    /**
     * Request camera permission
     */
    fun requestCameraPermission(
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        permissionLauncher.launch(CAMERA_PERMISSION)
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(
        permissionLauncher: ActivityResultLauncher<Array<String>>
    ) {
        permissionLauncher.launch(getRequiredStoragePermissions())
    }

    /**
     * Check if all permissions in array are granted
     */
    fun areAllPermissionsGranted(permissions: Map<String, Boolean>): Boolean {
        return permissions.all { it.value }
    }

    /**
     * Get permission rationale message
     */
    fun getPermissionRationaleMessage(permission: String): String {
        return when (permission) {
            CAMERA_PERMISSION -> "Camera permission is required to take photos"
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES -> "Storage permission is required to select files"
            else -> "This permission is required for the app to function properly"
        }
    }

    /**
     * Check if should show rationale for permission
     */
    fun shouldShowRationale(fragment: Fragment, permission: String): Boolean {
        return fragment.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * Check multiple permissions at once
     */
    fun checkMultiplePermissions(context: Context, permissions: Array<String>): Map<String, Boolean> {
        return permissions.associateWith { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}