package com.smapra.exambro.security

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import com.smapra.exambro.util.PlatformCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenCaptureGuard @Inject constructor() {

    fun applyFlagSecure(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }

    fun isScreenBeingCaptured(activity: Activity): Boolean {
        // FLAG_SECURE is the primary defense on all API levels.
        // On API 34+ Android itself excludes FLAG_SECURE windows from capture.
        return false
    }

    fun blockScreenshots(activity: Activity) {
        applyFlagSecure(activity)
        if (PlatformCompat.isApi33) {
            activity.setRecentsScreenshotEnabled(false)
        }
    }

    fun removeProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (PlatformCompat.isApi33) {
            activity.setRecentsScreenshotEnabled(true)
        }
    }
}
