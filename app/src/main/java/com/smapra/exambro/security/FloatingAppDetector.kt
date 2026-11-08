package com.smapra.exambro.security

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.smapra.exambro.util.PlatformCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SecurityViolation(val message: String) {
    data object MultiWindowDetected : SecurityViolation("Multi-window mode detected")
    data object PipModeDetected : SecurityViolation("Picture-in-Picture mode detected")
    data class FloatingAppDetected(val appName: String) : SecurityViolation("Floating app detected: $appName")
    data class OverlayDetected(val packageName: String) : SecurityViolation("Overlay window detected from: $packageName")
    data class SuspiciousAccessibility(val serviceName: String) : SecurityViolation("Suspicious accessibility service: $serviceName")
    data object AppSwitchDetected : SecurityViolation("App switching detected during exam")
    data object ScreenCaptureDetected : SecurityViolation("Screen recording detected")
}

@Singleton
class FloatingAppDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val knownOverlayCategories = listOf(
        "floating", "overlay", "bubble", "popup", "chat.head",
        "screen.record", "screenrecord", "mirror", "cast",
        "remote.desktop", "teamviewer", "anydesk", "vnc",
    )

    fun detectViolations(isInMultiWindowMode: Boolean, isInPipMode: Boolean): List<SecurityViolation> {
        val violations = mutableListOf<SecurityViolation>()

        // Multi-window: API 24+
        if (PlatformCompat.isApi24 && isInMultiWindowMode) {
            violations.add(SecurityViolation.MultiWindowDetected)
        }

        // PiP: API 26+
        if (PlatformCompat.isApi26 && isInPipMode) {
            violations.add(SecurityViolation.PipModeDetected)
        }

        violations.addAll(checkOverlayApps())
        violations.addAll(checkRunningProcesses())
        violations.addAll(checkSuspiciousAccessibilityServices())

        if (violations.isNotEmpty()) {
            Log.w("SecurityCheck", "Violations found: ${violations.map { it.message }}")
        }

        return violations
    }

    @Suppress("QueryPermissionsNeeded")
    private fun checkOverlayApps(): List<SecurityViolation> {
        val violations = mutableListOf<SecurityViolation>()

        // AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW requires API 23+
        if (!PlatformCompat.isApi23) return violations

        val pm = context.packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            return violations
        }

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return violations

        val systemPrefixes = listOf(
            "com.android.", "com.google.", "com.miui.", "com.xiaomi.",
            "com.qualcomm.", "com.samsung.", "com.sec.", "android",
            "com.mediatek.", "com.huawei.", "com.oppo.", "com.coloros.",
            "com.oplus.", "com.vivo.", "com.bbk.",
        )

        for (app in installedApps) {
            if (app.packageName == context.packageName) continue

            // Skip system/OEM packages — they aren't cheating tools
            val pkgLower = app.packageName.lowercase()
            if (systemPrefixes.any { pkgLower.startsWith(it) }) continue

            val hasOverlayPermission = try {
                val mode = if (PlatformCompat.isApi29) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        app.uid,
                        app.packageName,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        app.uid,
                        app.packageName,
                    )
                }
                mode == AppOpsManager.MODE_ALLOWED
            } catch (_: Exception) {
                false
            }

            if (hasOverlayPermission) {
                val appLabel = try {
                    pm.getApplicationLabel(app).toString().lowercase()
                } catch (_: Exception) {
                    ""
                }
                val isSuspicious = knownOverlayCategories.any { category ->
                    appLabel.contains(category) || pkgLower.contains(category)
                }
                if (isSuspicious) {
                    violations.add(SecurityViolation.OverlayDetected(app.packageName))
                }
            }
        }

        return violations
    }

    private fun checkRunningProcesses(): List<SecurityViolation> {
        val violations = mutableListOf<SecurityViolation>()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return violations

        val systemPrefixes = listOf(
            "com.android.", "com.google.", "com.miui.", "com.xiaomi.",
            "com.qualcomm.", "com.samsung.", "com.sec.", "android",
            "com.mediatek.", "com.huawei.", "com.oppo.", "com.coloros.",
            "com.oplus.", "com.vivo.", "com.bbk.", "system",
        )

        @Suppress("DEPRECATION")
        val runningApps = activityManager.runningAppProcesses ?: return violations
        for (process in runningApps) {
            if (process.processName == context.packageName) continue
            if (systemPrefixes.any { process.processName.lowercase().startsWith(it) }) continue
            if (process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                val pkgLower = process.processName.lowercase()
                val isSuspicious = knownOverlayCategories.any { category ->
                    pkgLower.contains(category)
                }
                if (isSuspicious) {
                    violations.add(SecurityViolation.FloatingAppDetected(process.processName))
                }
            }
        }

        return violations
    }

    private val suspiciousKeywords = listOf(
            "remote.desktop", "teamviewer", "anydesk", "vnc",
            "screenrecord", "screen.record", "screen_record",
            "spy", "keylog", "inject",
        )

    private fun checkSuspiciousAccessibilityServices(): List<SecurityViolation> {
        val violations = mutableListOf<SecurityViolation>()

        val enabledServices = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        } catch (_: Exception) {
            null
        } ?: return violations

        enabledServices.split(":").filter { it.isNotBlank() }.forEach { service ->
            val serviceLower = service.lowercase()
            if (service.contains(context.packageName)) return@forEach
            // Skip well-known system accessibility services
            if (serviceLower.contains("talkback") ||
                serviceLower.contains("com.google.android") ||
                serviceLower.contains("com.samsung") ||
                serviceLower.contains("com.miui") ||
                serviceLower.contains("com.xiaomi") ||
                serviceLower.contains("com.android")
            ) return@forEach

            val isSuspicious = suspiciousKeywords.any { keyword ->
                serviceLower.contains(keyword)
            }
            if (isSuspicious) {
                violations.add(SecurityViolation.SuspiciousAccessibility(service))
            }
        }

        return violations
    }

    fun checkRecentAppUsage(): List<SecurityViolation> {
        val violations = mutableListOf<SecurityViolation>()
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager != null && !activityManager.isInLockTaskMode) {
                // Device tidak dalam lock mode
            }
        } catch (_: Exception) {}
        return violations
    }
}
