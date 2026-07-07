package com.khushi.assistant

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

/**
 * Manages the floating glowing orb window that pops up over your screen
 * while Khushi is listening/speaking (like Siri's floating indicator).
 * Requires the "Display over other apps" permission.
 */
object OverlayHelper {

    private var orbView: MoonOrbView? = null
    private var windowManager: WindowManager? = null

    fun canDrawOverlays(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)
    }

    fun showOrb(context: Context) {
        if (!canDrawOverlays(context)) return
        if (orbView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = MoonOrbView(context)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            220, 220,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 24
        params.y = 120

        try {
            windowManager?.addView(view, params)
            orbView = view
        } catch (e: Exception) {
            // overlay permission missing or restricted by device manufacturer
        }
    }

    fun setActive(active: Boolean) {
        orbView?.setActive(active)
    }

    fun hideOrb(context: Context) {
        orbView?.let {
            it.stop()
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
            }
        }
        orbView = null
    }
}
