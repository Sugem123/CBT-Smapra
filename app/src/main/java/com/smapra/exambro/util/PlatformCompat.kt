package com.smapra.exambro.util

import android.os.Build

/**
 * Centralized API-level checks. Every platform-dependent feature routes through here
 * so the same codebase compiles for minSdk 21 and runs with full features on 26+.
 *
 * No feature is removed — on older APIs the call is simply skipped or replaced
 * by the best available alternative.
 */
object PlatformCompat {

    /** Android 5.0 Lollipop */
    inline val isApi21 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    /** Android 6.0 Marshmallow */
    inline val isApi23 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    /** Android 7.0 Nougat — multi-window */
    inline val isApi24 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    /** Android 8.0 Oreo — PiP, adaptive icons, TYPE_APPLICATION_OVERLAY */
    inline val isApi26 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /** Android 10 Q — unsafeCheckOpNoThrow */
    inline val isApi29 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Android 11 R — WindowInsetsController */
    inline val isApi30 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Android 12 S — splash screen API */
    inline val isApi31 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Android 13 Tiramisu — setRecentsScreenshotEnabled */
    inline val isApi33 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Android 14 Upside Down Cake — MediaProjection callback */
    inline val isApi34 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /** Check if device supports multi-window (API 24+) */
    fun supportsMultiWindow() = isApi24

    /** Check if device supports PiP (API 26+) */
    fun supportsPip() = isApi26
}
