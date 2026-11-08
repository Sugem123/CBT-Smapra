package com.smapra.exambro.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.smapra.exambro.util.PlatformCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent = ComponentName(context, ExamDeviceAdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponent)

    fun startKioskMode(activity: Activity) {
        hideSystemUI(activity)

        if (isDeviceOwner) {
            startLockTaskAsDeviceOwner(activity)
        } else {
            startScreenPinning(activity)
        }
    }

    fun stopKioskMode(activity: Activity) {
        showSystemUI(activity)

        try {
            activity.stopLockTask()
        } catch (_: Exception) {
            // Not in lock task mode
        }
    }

    private fun startLockTaskAsDeviceOwner(activity: Activity) {
        try {
            devicePolicyManager.setLockTaskPackages(
                adminComponent,
                arrayOf(context.packageName),
            )
            activity.startLockTask()
        } catch (_: Exception) {
            startScreenPinning(activity)
        }
    }

    private fun startScreenPinning(activity: Activity) {
        try {
            activity.startLockTask()
        } catch (_: Exception) {
            // Screen pinning requires user confirmation via system dialog
        }
    }

    private fun hideSystemUI(activity: Activity) {
        if (PlatformCompat.isApi30) {
            val controller = activity.window.insetsController ?: return
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }

    private fun showSystemUI(activity: Activity) {
        if (PlatformCompat.isApi30) {
            activity.window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun isInLockTaskMode(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun preventStatusBarExpansion(activity: Activity) {
        hideSystemUI(activity)
    }
}
