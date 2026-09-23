package com.apagon.rhythm.notifications

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class PackageReplacedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        refreshSamsungIconCache(context)
    }

    companion object {
        fun refreshSamsungIconCache(context: Context) {
            val pm = context.packageManager
            // Toggle the non-launcher alias — never touch MainActivityDefault (the launcher entry)
            // as disabling it even briefly can remove the home screen shortcut on Samsung.
            // Toggling any component still fires PACKAGE_CHANGED, which causes Samsung One UI
            // Home to invalidate its icon cache and re-read the icon from the APK.
            val alias = ComponentName(context.packageName, "${context.packageName}.IconRefreshAlias")
            pm.setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
