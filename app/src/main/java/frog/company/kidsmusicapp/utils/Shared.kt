package frog.company.kidsmusicapp.utils

import android.app.ActivityManager
import android.content.Context

object Shared {
    fun serviceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE))
            if (serviceClass.name == service.service.className) return true
        return false
    }

}