package com.smapra.exambro.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class ExamDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Menonaktifkan device admin akan mencegah CBT SMAN 1 PRAMBON berjalan dalam mode kiosk."
    }
}
