package com.smapra.exambro.security

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.smapra.exambro.util.PlatformCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ThreatAppInfo(
    val packageName: String,
    val appName: String,
    val reason: String,
)

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
    // Daftar hitam nama paket aplikasi floating / multitasking populer dari Google Play Store
    private val blacklistedPackages = setOf(
        "com.lwi.android.flapps",                // Floating Apps (multitasking)
        "com.lwi.android.flappslite",            // Floating Apps Lite
        "com.applay.overlay",                    // Overlays - Float Everywhere
        "com.leo.floating.browser",              // Floating Browser
        "com.udicorn.floatingbrowser",           // Floating Browser
        "me.flips.floatingbrowser",              // Float Browser
        "com.mura.floatingbrowser",              // Floating Web Browser
        "com.mian.floating.browser",             // Floating Multi Browser
        "com.bubbles.browser",                   // Bubble Browser
        "com.screen.floating.browser",           // Floating Screen Browser
        "com.float.browser.web",                 // Floating Browser Web
        "com.weber.floating",                    // Floating Web
        "com.airbrowser",                        // Air Browser
        "com.linkbubble.playstore",              // Link Bubble
        "com.flynx",                             // Flynx - Smart Floating Browser
        "com.floating.tube",                     // Float Tube - Multitasking
        "com.free.float.tube",                   // Floating Tube
        "com.tubefloat.app",                     // Tube Float
        "com.playtube.floating",                 // Play Tube - Floating Video
        "free.tube.premium.advanced.tuber",      // Pure Tuber
        "com.stream.floating.tube",              // Floating Player
        "com.awesome.videoplayer",               // Awesome Pop-up Video
        "com.popuptube",                         // Popup Tube
        "com.flytube",                           // FlyTube
        "com.tubepip",                           // Tube PiP
        "com.floatvideo.player",                 // Float Video Player
        "org.schabi.newpipe",                    // NewPipe
        "com.vanced.android.youtube",            // YouTube Vanced
        "app.revanced.android.youtube",          // ReVanced
        "com.shy.floatingcalc",                  // Floating Calculator
        "com.calculator.floating",               // Float Calc
        "com.aviram.floatingcalculator",         // Floating Calculator
        "com.wg.floatingcalc",                   // Float Calculator
        "com.tasomaniac.floating.calculator",    // Floating Calculator
        "com.floatnotes.app",                    // Floating Notes
        "com.notes.floating",                    // Sticky Notes Floating
        "com.popnotes",                          // Popup Notes
        "com.floating.stickynotes",              // Floating Sticky Notes
        "com.floating.translate",                // Tap to Translate Screen
        "com.copymode.floating",                 // Screen Translate Floating
        "com.translate.floating",                // Floating Translator
        "com.language.translator.floating",      // Floating Language Translator
        "com.directchat.android",                // DirectChat (Chat Heads)
        "com.flyperinc.flyperlink",              // Flyperlink
        "com.flyperinc.flychat",                 // Flychat
        "com.chatheads",                         // Chat Heads
        "com.bubble.reply",                      // Bubble Reply
        "com.ninja.chatheads",                   // Ninja Chat Heads
        "com.split.screen.shortcut",             // Split Screen Shortcut
        "com.screensplit.multi",                 // Multi Window Split
        "com.smart.sidebar",                     // Smart Sidebar
        "com.edge.screen",                       // Edge Screen
        "com.fly.gps",                           // Fly GPS
        "com.locationchanger",                   // Location Changer
        "com.incorporateapps.fakegps.fre",       // Fake GPS
    )

    private val knownOverlayKeywords = listOf(
        "floating", "float", "overlay", "bubble", "popup", "chat.head", "chathead",
        "multitask", "pip", "tuber", "split screen", "screensplit", "screen translate",
        "remote.desktop", "teamviewer", "anydesk", "vnc",
    )

    private val systemPrefixes = listOf(
        "com.android.", "com.google.", "com.miui.", "com.xiaomi.",
        "com.qualcomm.", "com.samsung.", "com.sec.", "android",
        "com.mediatek.", "com.huawei.", "com.oppo.", "com.coloros.",
        "com.oplus.", "com.vivo.", "com.bbk.", "com.transsion.",
        "com.transsion.hilauncher", "com.infinix.", "com.itel.", "com.tecno.",
    )

    // Whitelist aplikasi yang diizinkan (Shopee, Musixmatch, Fly GPS, MyTelkomsel, Location Changer)
    private val defaultWhitelistedPackages = setOf(
        "com.shopee.id",
        "com.shopeepay.id",
        "com.musixmatch.android.lyrify",
        "com.fly.gps",
        "com.telkomsel.telkomselcm",
        "com.locationchanger",
        "com.pure.indosat.care",
        "com.apps.myxl",
        "com.byu.id",
        "id.dana",
        "ovo.id",
        "com.gojek.app",
        "com.tokopedia.tkpd",
    )

    // Dynamic whitelist dari server Moodle (jika sudah di-fetch)
    private var dynamicWhitelistedPackages: Set<String> = emptySet()
    private var dynamicBlacklistedPackages: Set<String> = emptySet()

    fun updateDynamicConfig(whitelist: Set<String>, blacklist: Set<String>) {
        dynamicWhitelistedPackages = whitelist
        dynamicBlacklistedPackages = blacklist
    }

    /**
     * Memindai seluruh aplikasi di HP siswa sebelum ujian dimulai (Pre-Exam Gate).
     * Mengembalikan daftar aplikasi terlarang yang wajib dicopot/dimatikan izinnya.
     */
    fun scanInstalledThreats(): List<ThreatAppInfo> {
        val threats = mutableListOf<ThreatAppInfo>()
        val pm = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager

        val installedPackages = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (_: Exception) {
            emptyList()
        }

        val allWhitelisted = defaultWhitelistedPackages + dynamicWhitelistedPackages
        val allBlacklisted = (blacklistedPackages + dynamicBlacklistedPackages) - allWhitelisted

        for (pkgInfo in installedPackages) {
            val pkgName = pkgInfo.packageName ?: continue
            if (pkgName == context.packageName) continue

            val pkgLower = pkgName.lowercase()

            // Jika masuk Whitelist (Shopee, Musixmatch, Fly GPS, MyTelkomsel, Location Changer), LEWATI!
            if (allWhitelisted.contains(pkgName) || allWhitelisted.any { pkgLower == it.lowercase() }) {
                continue
            }

            // Lewati aplikasi sistem OEM murni
            if (systemPrefixes.any { pkgLower.startsWith(it) }) continue

            val appInfo = pkgInfo.applicationInfo ?: continue
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystemApp) continue

            val appLabel = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkgName
            }
            val labelLower = appLabel.lowercase()

            // 1. Cek apakah masuk Blacklist By-Name resmi Play Store
            if (allBlacklisted.contains(pkgName) || allBlacklisted.any { pkgLower.startsWith(it) }) {
                threats.add(
                    ThreatAppInfo(
                        packageName = pkgName,
                        appName = appLabel,
                        reason = "Aplikasi floating/multitasking terlarang",
                    )
                )
                continue
            }

            // 2. Cek apakah memiliki kata kunci floating / overlay pada nama atau package
            val matchesKeyword = knownOverlayKeywords.any { kw ->
                labelLower.contains(kw) || pkgLower.contains(kw)
            }
            if (matchesKeyword) {
                threats.add(
                    ThreatAppInfo(
                        packageName = pkgName,
                        appName = appLabel,
                        reason = "Terdeteksi sebagai aplikasi floating/overlay",
                    )
                )
                continue
            }

            // 3. Cek apakah memiliki izin SYSTEM_ALERT_WINDOW aktif (Display over other apps)
            if (appOps != null && PlatformCompat.isApi23) {
                val hasOverlayAllowed = try {
                    val mode = if (PlatformCompat.isApi29) {
                        appOps.unsafeCheckOpNoThrow(
                            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                            appInfo.uid,
                            pkgName,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        appOps.checkOpNoThrow(
                            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                            appInfo.uid,
                            pkgName,
                        )
                    }
                    mode == AppOpsManager.MODE_ALLOWED
                } catch (_: Exception) {
                    false
                }

                if (hasOverlayAllowed) {
                    threats.add(
                        ThreatAppInfo(
                            packageName = pkgName,
                            appName = appLabel,
                            reason = "Izin 'Tampilkan di atas aplikasi lain' aktif",
                        )
                    )
                }
            }
        }

        return threats.distinctBy { it.packageName }
    }

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

        for (app in installedApps) {
            if (app.packageName == context.packageName) continue

            val pkgLower = app.packageName.lowercase()
            val allWhitelisted = defaultWhitelistedPackages + dynamicWhitelistedPackages
            if (allWhitelisted.contains(app.packageName) || allWhitelisted.any { pkgLower == it.lowercase() }) continue
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
                val isSuspicious = blacklistedPackages.contains(app.packageName) ||
                    knownOverlayKeywords.any { category ->
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

        @Suppress("DEPRECATION")
        val runningApps = activityManager.runningAppProcesses ?: return violations
        for (process in runningApps) {
            if (process.processName == context.packageName) continue
            val allWhitelisted = defaultWhitelistedPackages + dynamicWhitelistedPackages
            val procLower = process.processName.lowercase()
            if (allWhitelisted.contains(process.processName) || allWhitelisted.any { procLower == it.lowercase() }) continue
            if (systemPrefixes.any { process.processName.lowercase().startsWith(it) }) continue
            if (process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                val pkgLower = process.processName.lowercase()
                val isSuspicious = blacklistedPackages.contains(process.processName) ||
                    knownOverlayKeywords.any { category ->
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
